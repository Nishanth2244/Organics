package com.organics.products.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.organics.products.dto.*;
import com.organics.products.entity.*;
import com.organics.products.respository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.organics.products.config.SecurityUtil;
import com.organics.products.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class OrderService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

    @Autowired
    private ProductRepo productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ShiprocketService shiprocketService;

    @Autowired
    private S3Service s3Service;
    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ProductDiscountRepository productDiscountRepository;
    
    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private TaxService taxService;

    @CacheEvict(
            value = { "userOrders", "adminOrders", "orderStats", "topProducts"}, allEntries = true)
    @Transactional
    public OrderDTO placeOrder(OrderAddressRequestDTO orderRequest) {
        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("🛒 Placing order for userId={}", userId);


        Address selectedAddress;
        if (orderRequest.getAddressId() != null) {
            selectedAddress = addressRepository.findById(orderRequest.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

            if (!selectedAddress.getUser().getId().equals(userId)) {
                throw new RuntimeException("Address does not belong to user");
            }
        } else {
            selectedAddress = addressRepository.findPrimaryAddressByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("No address found"));
        }

      // CART & ITEMS
        Cart cart = cartRepository.findByUserAndIsActive(user, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active cart"));

        List<CartItems> cartItems = cartItemRepository.findByCartIdWithProduct(cart.getId());

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double cartDiscount = cart.getDiscountAmount() != null ? cart.getDiscountAmount() : 0.0;

        log.info("📦 Cart items={}, Cart discount={}", cartItems.size(), cartDiscount);

      // FIRST PASS – MRP + STOCK
        double totalCartMrp = 0.0;
        Map<Long, Integer> productQtyMap = new HashMap<>();
        List<Inventory> inventoriesToReserve = new ArrayList<>();

        for (CartItems item : cartItems) {
            Inventory inventory = item.getInventory();
            Product product = inventory.getProduct();

            if (inventory.getAvailableStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for " + product.getProductName());
            }

            inventoriesToReserve.add(inventory);
            productQtyMap.put(inventory.getId(), item.getQuantity());

            totalCartMrp += product.getMRP() * item.getQuantity();
        }

        log.info(" Total cart MRP={}", totalCartMrp);

      // CREATE ORDER (NO AMOUNT)
        Order order = new Order();
        order.setUser(user);
        order.setCart(cart);
        order.setShippingAddress(selectedAddress);
        order.setOrderDate(LocalDate.now());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

       // SECOND PASS – PRICING
        List<OrderItems> orderItemsList = new ArrayList<>();

        double finalAmount = 0.0;
        double totalTax = 0.0;
        double totalDiscount = 0.0;

        for (CartItems item : cartItems) {

            Product product = item.getInventory().getProduct();
            int qty = item.getQuantity();
            double mrp = product.getMRP();

            double itemMrpValue = mrp * qty;

            double itemDiscount = (cartDiscount > 0 && totalCartMrp > 0)
                    ? (itemMrpValue / totalCartMrp) * cartDiscount
                    : 0.0;

            double netItemPrice = itemMrpValue - itemDiscount;

            double taxPercent = taxService.getTaxPercentByCategoryId(
                    product.getCategory().getId());

            double itemTax = (netItemPrice * taxPercent) / 100;

            OrderItems orderItem = new OrderItems();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(qty);
            orderItem.setPrice(netItemPrice / qty);
            orderItem.setDiscount(itemDiscount / qty);
            orderItem.setTax(itemTax / qty);

            orderItemsList.add(orderItem);

            finalAmount += netItemPrice + itemTax;
            totalTax += itemTax;
            totalDiscount += itemDiscount;

            /* ---------- LOGS ---------- */
            log.info(" Item: {}", product.getProductName());
            log.info("   MRP/unit      : {}", mrp);
            log.info("   Quantity      : {}", qty);
            log.info("   Item Discount : {}", itemDiscount);
            log.info("   Item Tax      : {}", itemTax);
            log.info("   Item Total    : {}", netItemPrice + itemTax);
        }

        if (orderItemsList.isEmpty()) {
            throw new RuntimeException("No valid items to create order");
        }

     //  SAVE ITEMS + FINAL AMOUNT
        orderItemsRepository.saveAll(orderItemsList);

        savedOrder.setOrderItems(orderItemsList);
        savedOrder.setOrderAmount(finalAmount);

        orderRepository.save(savedOrder);

        log.info(" Order created. Final Amount={}", finalAmount);

    //   INVENTORY RESERVATION
        for (Inventory inv : inventoriesToReserve) {
            inventoryService.reserveStock(
                    inv.getId(),
                    productQtyMap.get(inv.getId()),
                    savedOrder.getId()
            );
        }

      // SHIPROCKET INTEGRATION
        try {
            ShiprocketCreateOrderResponse response =
                    shiprocketService.createOrder(savedOrder, user, selectedAddress);

            if (response != null) {
                savedOrder.setShiprocketOrderId(
                        response.getOrderId() != null ? String.valueOf(response.getOrderId()) : null);
                savedOrder.setShiprocketShipmentId(response.getShipmentId());
                savedOrder.setShiprocketAwbCode(response.getAwbCode());
                savedOrder.setShiprocketCourierName(response.getCourierName());

                if (response.getAwbCode() != null) {
                    savedOrder.setShiprocketTrackingUrl(
                            "https://shiprocket.co/tracking/" + response.getAwbCode());
                }

                orderRepository.save(savedOrder);
                log.info(" Shiprocket order created. ShipmentId={}", response.getShipmentId());
            }
        } catch (Exception e) {
            log.error(" Shiprocket failed: {}", e.getMessage());
            savedOrder.setShiprocketOrderId("FAILED");
            orderRepository.save(savedOrder);
        }


        log.info(" ORDER SUMMARY");
        log.info("   Subtotal MRP : {}", totalCartMrp);
        log.info("   Discount     : {}", totalDiscount);
        log.info("   Tax          : {}", totalTax);
        log.info("   Grand Total  : {}", finalAmount);

        return convertToOrderDTO(savedOrder);
    }


    private Cart getActiveCartWithItems(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByIdWithItemsAndUser(userId);

        if (cartOpt.isPresent()) {
            return cartOpt.get();
        }

        List<Cart> activeCarts = cartRepository.findByUserAndIsActive(
                userRepository.findById(userId).orElse(null),
                true
        );

        if (activeCarts.isEmpty()) {
            return null;
        }

        Cart cart = activeCarts.get(0);
        List<CartItems> items = cartItemRepository.findByCartIdWithProduct(cart.getId());
        cart.setItems(new HashSet<>(items));

        return cart;
    }

    @Cacheable(
            value = "userOrders",
            key = "T(com.organics.products.config.SecurityUtil).getCurrentUserId().orElse(null) + '-' + #page + '-' + #size",
            unless = "#result == null")
    @Transactional(readOnly = true)
    public Page<OrderDTO> getUserOrders(int page, int size) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());

        Page<Order> orderPage = orderRepository.findByUser(user, pageable);

        if (orderPage.isEmpty()) {
            log.info("No orders found for userId={}", userId);
            return Page.empty(pageable);
        }

        return orderPage.map(this::convertToOrderDTO);
    }

    @Cacheable(
            value = "orderById", key = "#orderId", unless = "#result == null")
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId) && !SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized access to order");
        }

        return convertToOrderDTO(order);
    }
    @Cacheable(
            value = "adminOrders", key = "'all'")
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        List<Order> orders = orderRepository.findAllByOrderByOrderDateDesc();

        return orders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }
    @Cacheable(
            value = "adminOrders",
            key = "'status-' + #status")
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(OrderStatus status) {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        List<Order> orders = orderRepository.findByOrderStatusOrderByOrderDateDesc(status);

        return orders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }
    @CacheEvict(
            value = { "orderById", "userOrders", "adminOrders", "orderStats"}, allEntries = true)
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        order.setOrderStatus(status);

        if (status == OrderStatus.SHIPPED && order.getShiprocketShipmentId() != null) {
            try {

                String pickupDate = LocalDate.now().plusDays(1).toString();
                shiprocketService.schedulePickup(order.getShiprocketShipmentId(), pickupDate);
                log.info("Pickup scheduled for shipment ID: {}", order.getShiprocketShipmentId());
            } catch (Exception e) {
                log.error("Failed to schedule pickup: {}", e.getMessage());
            }
        }

        if (status == OrderStatus.CANCELLED && order.getShiprocketShipmentId() != null) {
            try {
                shiprocketService.cancelShipment(order.getShiprocketShipmentId());
                log.info("Shipment cancelled for shipment ID: {}", order.getShiprocketShipmentId());
            } catch (Exception e) {
                log.error("Failed to cancel shipment: {}", e.getMessage());
            }
        }

        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, status);

        return convertToOrderDTO(updatedOrder);
    }

    @CacheEvict(
            value = { "orderById", "userOrders", "adminOrders", "orderStats"}, allEntries = true)
    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId) && !SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
                order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel order that is already shipped or delivered");
        }

        if (order.getShiprocketShipmentId() != null) {
            try {
                shiprocketService.cancelShipment(order.getShiprocketShipmentId());
                log.info("Shiprocket shipment cancelled for order ID: {}", orderId);
            } catch (Exception e) {
                log.error("Failed to cancel Shiprocket shipment: {}", e.getMessage());
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} cancelled by user {}", orderId, userId);

        return convertToOrderDTO(updatedOrder);
    }

    public byte[] getShippingLabel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getShiprocketShipmentId() == null) {
            throw new RuntimeException("No Shiprocket shipment found for this order");
        }

        return shiprocketService.generateShippingLabel(order.getShiprocketShipmentId());
    }

    public ShiprocketTrackingResponse trackOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getShiprocketAwbCode() == null) {
            throw new RuntimeException("No AWB code found for this order");
        }

        return shiprocketService.trackOrder(order.getShiprocketAwbCode());
    }

    public List<Map<String, Object>> getShippingRates(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = order.getUser();
        Address deliveryAddress = order.getShippingAddress();
        if (deliveryAddress == null) {
            throw new RuntimeException("No shipping address found for order");
        }

        Address pickupAddress = getWarehouseAddress();

        double totalWeight = 1.0;

        return shiprocketService.getShippingRates(
                pickupAddress,
                deliveryAddress,
                totalWeight,
                10, 10, 10);
    }


    private OrderDTO convertToOrderDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setOrderAmount(order.getOrderAmount());
        dto.setDescription(order.getDescription());
        dto.setOrderStatus(order.getOrderStatus());
        //dto.setPaymentStatus(dto.getPaymentStatus());
        dto.setPaymentStatus(String.valueOf(order.getPaymentStatus()));  // ok

        dto.setUserId(order.getUser().getId());

        User user = order.getUser();
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String userName = (firstName + " " + lastName).trim();

        dto.setUserName(userName.isEmpty() ? "Customer" : userName);
        dto.setUserEmail(user.getEmailId());
        dto.setUserPhone(user.getPhoneNumber());

        if (order.getShippingAddress() != null) {
            Address address = order.getShippingAddress();
            ShippingAddressDTO addressDTO = new ShippingAddressDTO();
            addressDTO.setId(address.getId());
            addressDTO.setHouseNumber(address.getHouseNumber());
            addressDTO.setApartmentName(address.getApartmentName());
            addressDTO.setStreetName(address.getStreetName());
            addressDTO.setLandMark(address.getLandMark());
            addressDTO.setCity(address.getCity());
            addressDTO.setState(address.getState());
            addressDTO.setPinCode(address.getPinCode());
            addressDTO.setAlternatePhoneNumber(address.getAlternatePhoneNumber());
            addressDTO.setAddressType(String.valueOf(address.getAddressType()));
            addressDTO.setIsPrimary(address.getIsPrimary());

            dto.setShippingAddress(addressDTO);
        } else {
            dto.setShippingAddress(null);
        }

        dto.setShiprocketOrderId(order.getShiprocketOrderId());
        dto.setShiprocketShipmentId(order.getShiprocketShipmentId());
        dto.setShiprocketAwbCode(order.getShiprocketAwbCode());
        dto.setShiprocketCourierName(order.getShiprocketCourierName());
        dto.setShiprocketLabelUrl(order.getShiprocketLabelUrl());
        dto.setShiprocketTrackingUrl(order.getShiprocketTrackingUrl());
        dto.setPaymentStatus(String.valueOf(order.getPaymentStatus()));

        if (order.getPayment() != null) {
            dto.setRazorpayOrderId(order.getPayment().getRazorpayOrderId());
        }

        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        double orderSubtotal = 0.0;
        double orderItemDiscount = 0.0;
        double orderTax = 0.0;

        if (order.getOrderItems() != null) {
            for (OrderItems item : order.getOrderItems()) {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getProductName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setTax(item.getTax());
                itemDTO.setDiscount(item.getDiscount());

                // Set image URL
                String imageUrl = null;
                Product product = item.getProduct();

                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    ProductImage firstImage = product.getImages().get(0);
                    if (firstImage != null && firstImage.getImageUrl() != null) {
                        try {
                            imageUrl = s3Service.getFileUrl(firstImage.getImageUrl());
                        } catch (Exception e) {
                            log.warn("Failed to get image URL for product {}: {}",
                                    product.getId(), e.getMessage());
                            imageUrl = firstImage.getImageUrl();
                        }
                    }
                }

                if (imageUrl == null && item.getProduct().getImages() != null) {
                    try {
                        imageUrl = s3Service.getFileUrl(item.getProduct().getImages().toString());
                    } catch (Exception e) {
                        log.warn("Failed to get image URL from order item: {}", e.getMessage());
                    }
                }

                itemDTO.setImageUrl(imageUrl);

                double itemTotal = item.getPrice() * item.getQuantity();
                double itemTax = item.getTax() * item.getQuantity();
                double itemDiscount = item.getDiscount() * item.getQuantity();

                double lineTotal = itemTotal + itemTax;
                itemDTO.setTotalPrice(lineTotal);

                orderSubtotal += itemTotal;
                orderTax += itemTax;
                orderItemDiscount += itemDiscount;

                itemDTOs.add(itemDTO);
            }
        }

        dto.setOrderItems(itemDTOs);

        // Cart discount is the sum of all item discounts (distributed proportionally)
        double totalCartDiscount = orderItemDiscount;

        dto.setSubtotal(orderSubtotal);
        dto.setTotalTax(orderTax);
        dto.setItemDiscount(orderItemDiscount);
        dto.setCartDiscount(totalCartDiscount);
        dto.setTotalDiscount(totalCartDiscount);
        dto.setGrandTotal(orderSubtotal + orderTax - totalCartDiscount);
        return dto;
    }

    @Cacheable(
            value = "orderStats", key = "'summary'")
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatistics() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        List<Order> allOrders = orderRepository.findAll();

        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.PENDING)
                .count();
        long confirmedOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.CONFIRMED)
                .count();
        long shippedOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.SHIPPED)
                .count();
        long deliveredOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
                .count();

        double totalRevenue = allOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getOrderAmount)
                .sum();

        return Map.of(
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "confirmedOrders", confirmedOrders,
                "shippedOrders", shippedOrders,
                "deliveredOrders", deliveredOrders,
                "totalRevenue", totalRevenue
        );
    }

    @Cacheable(
            value = "adminOrders", key = "'date-' + #date")
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersOnDate(LocalDate date) {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        if (date == null) {
            throw new RuntimeException("Date is required");
        }

        List<Order> orders = orderRepository.findByOrderDateOrderByOrderDateDesc(date);
        return orders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(
            value = "orderStats", key = "'daily-' + #startDate + '-' + #endDate")
    @Transactional(readOnly = true)
    public List<DailyOrderStatsDTO> getDailyOrderStatistics(LocalDate startDate, LocalDate endDate) {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        if (startDate == null || endDate == null) {
            throw new RuntimeException("Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        List<DailyOrderStatsDTO> stats = new ArrayList<>();

        List<Object[]> dailyData = orderRepository.getDailyOrderStats(startDate, endDate);

        for (Object[] data : dailyData) {
            DailyOrderStatsDTO dailyStat = new DailyOrderStatsDTO();
            dailyStat.setDate((LocalDate) data[0]);
            dailyStat.setTotalOrders((Long) data[1]);
            dailyStat.setTotalRevenue((Double) data[2]);
            dailyStat.setPendingOrders((Long) data[3]);
            dailyStat.setDeliveredOrders((Long) data[4]);

            if (dailyStat.getTotalOrders() > 0) {
                dailyStat.setAverageOrderValue(dailyStat.getTotalRevenue() / dailyStat.getTotalOrders());
            } else {
                dailyStat.setAverageOrderValue(0.0);
            }

            stats.add(dailyStat);
        }
        List<LocalDate> allDates = startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
        for (LocalDate date : allDates) {
            boolean dateExists = stats.stream().anyMatch(stat -> stat.getDate().equals(date));
            if (!dateExists) {
                DailyOrderStatsDTO zeroStat = new DailyOrderStatsDTO();
                zeroStat.setDate(date);
                zeroStat.setTotalOrders(0L);
                zeroStat.setTotalRevenue(0.0);
                zeroStat.setPendingOrders(0L);
                zeroStat.setDeliveredOrders(0L);
                zeroStat.setAverageOrderValue(0.0);
                stats.add(zeroStat);
            }
        }

        stats.sort(Comparator.comparing(DailyOrderStatsDTO::getDate));
        return stats;
    }
    @Cacheable(
            value = "adminOrders", key = "'today'", unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<OrderDTO> getTodayOrders() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate today = LocalDate.now();
        List<Order> orders = orderRepository.findByOrderDateOrderByOrderDateDesc(today);
        return orders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(
            value = "adminOrders", key = "'month-' + T(java.time.LocalDate).now().getMonthValue()",
            unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<OrderDTO> getThisMonthOrders() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Order> orders = orderRepository.findByOrderDateBetweenOrderByOrderDateDesc(startOfMonth, endOfMonth);
        return orders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(
            value = "orderStats", key = "'today-summary'", unless = "#result == null")
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayOrderCount() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate today = LocalDate.now();
        Long totalCount = orderRepository.countByOrderDate(today);
        Double totalRevenue = orderRepository.sumOrderAmountByDate(today);
        Map<String, Long> statusCount = getTodayOrderCountByStatus();

        Map<String, Object> result = new HashMap<>();
        result.put("date", today.toString());
        result.put("totalOrders", totalCount != null ? totalCount : 0);
        result.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        result.put("statusWiseCount", statusCount);

        return result;
    }

    @Cacheable(
            value = "orderStats", key = "'today-status'", unless = "#result == null")
    @Transactional(readOnly = true)
    public Map<String, Long> getTodayOrderCountByStatus() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate today = LocalDate.now();
        Map<String, Long> statusCount = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCount.put(status.toString(), 0L);
        }

        List<Object[]> counts = orderRepository.countByStatusAndDate(today);
        for (Object[] count : counts) {
            OrderStatus status = (OrderStatus) count[0];
            Long countValue = (Long) count[1];
            statusCount.put(status.toString(), countValue);
        }

        return statusCount;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyOrderCount() {
        return getThisMonthOrderCount();
    }
    @Cacheable(
            value = "orderStats", key = "'month-summary-' + T(java.time.LocalDate).now().getMonthValue()",
            unless = "#result == null")
    @Transactional(readOnly = true)
    public Map<String, Object> getThisMonthOrderCount() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        Long totalCount = orderRepository.countByOrderDateBetween(startOfMonth, today);
        Double totalRevenue = orderRepository.sumOrderAmountByDateRange(startOfMonth, today);

        Map<String, Long> statusCount = getThisMonthOrderCountByStatus();
        long daysInMonth = ChronoUnit.DAYS.between(startOfMonth, today) + 1;
        double dailyAverage = totalCount > 0 ? (double) totalCount / daysInMonth : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("month", startOfMonth.getMonth().toString());
        result.put("year", startOfMonth.getYear());
        result.put("fromDate", startOfMonth.toString());
        result.put("toDate", today.toString());
        result.put("totalOrders", totalCount != null ? totalCount : 0);
        result.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        result.put("dailyAverageOrders", String.format("%.2f", dailyAverage));
        result.put("statusWiseCount", statusCount);

        return result;
    }
    @Cacheable(
            value = "orderStats", key = "'month-status-' + T(java.time.LocalDate).now().getMonthValue()",
            unless = "#result == null")
    @Transactional(readOnly = true)
    public Map<String, Long> getThisMonthOrderCountByStatus() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        Map<String, Long> statusCount = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCount.put(status.toString(), 0L);
        }
        List<Object[]> counts = orderRepository.countByStatusAndDateRange(startOfMonth, today);
        for (Object[] count : counts) {
            OrderStatus status = (OrderStatus) count[0];
            Long countValue = (Long) count[1];
            statusCount.put(status.toString(), countValue);
        }

        return statusCount;
    }

    private Address getWarehouseAddress() {
        Address warehouseAddress = new Address();
        warehouseAddress.setCity("Mumbai");
        warehouseAddress.setState("Maharashtra");
        warehouseAddress.setPinCode("400001");
        warehouseAddress.setHouseNumber("123");
        warehouseAddress.setStreetName("Warehouse Street");
        warehouseAddress.setApartmentName("Main Warehouse");

        return warehouseAddress;
    }
    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getHouseNumber() != null) sb.append(address.getHouseNumber()).append(", ");
        if (address.getApartmentName() != null) sb.append(address.getApartmentName()).append(", ");
        if (address.getStreetName() != null) sb.append(address.getStreetName()).append(", ");
        if (address.getLandMark() != null) sb.append(address.getLandMark()).append(", ");
        if (address.getCity() != null) sb.append(address.getCity()).append(", ");
        if (address.getState() != null) sb.append(address.getState()).append(", ");
        if (address.getPinCode() != null) sb.append(address.getPinCode());
        return sb.toString();
    }

    public boolean checkServiceability(Long userId, Long addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address deliveryAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        Address pickupAddress = getWarehouseAddress();

        return shiprocketService.checkServiceability(pickupAddress, deliveryAddress, 1.0);
    }
    
    
    @Transactional
    public void sendOrderToShiprocket(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        try {
            ShiprocketCreateOrderResponse shiprocketResponse = shiprocketService.createOrder(order, order.getUser(), order.getShippingAddress());
            
            if (shiprocketResponse.getOrderId() != null && shiprocketResponse.getOrderId() > 0) {
                order.setShiprocketOrderId(String.valueOf(shiprocketResponse.getOrderId()));
                order.setShiprocketShipmentId(shiprocketResponse.getShipmentId());
                order.setShiprocketAwbCode(shiprocketResponse.getAwbCode());
                order.setShiprocketTrackingUrl("https://shiprocket.co/tracking/" + shiprocketResponse.getAwbCode());
                //order.setOrderStatus(OrderStatus.PENDING); // Ikada confirm chestunnam
                orderRepository.save(order);
                log.info("✅ Shiprocket order created successfully for Order ID: {}", orderId);
            }
        } catch (Exception e) {
            log.error("❌ Failed to send order to Shiprocket: {}", e.getMessage());
        }
    }

    @Cacheable(
            value = "topProducts", key = "#limit + '-' + #startDate + '-' + #endDate + '-' + #inventoryId",
            unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<TopOrderedProductsDTO> getTopOrderedProducts(Integer limit, LocalDate startDate, LocalDate endDate, Long inventoryId) {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }
        List<TopOrderedProductsDTO> results;

        if (inventoryId != null) {
            // Filter by specific inventory
            results = orderItemsRepository.findTopOrderedProductsByInventoryId(inventoryId);
        } else if (startDate != null && endDate != null) {
            // Filter by date range
            results = orderItemsRepository.findTopOrderedProductsByDateRange(startDate, endDate);
        } else {
            // Get all-time top products
            results = orderItemsRepository.findTopOrderedProducts();
        }

        // Apply limit if specified
        if (limit != null && limit > 0) {
            return results.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // Default to top 10 if no limit specified
        return results.stream()
                .limit(10)
                .collect(Collectors.toList());
    }
    @Cacheable(
            value = "topProducts", key = "#limit + '-' + #startDate + '-' + #endDate + '-' + #inventoryId")
    @Transactional(readOnly = true)
    public List<TopOrderedProductsDTO> getTopOrderedProductsLast30Days() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<TopOrderedProductsDTO> results = orderItemsRepository.findTopOrderedProductsByDateRange(startDate, endDate);

        return results.stream()
                .limit(10)
                .collect(Collectors.toList());
    }
    @Cacheable(
            value = "topProducts", key = "'thisMonth'")
    @Transactional(readOnly = true)
    public List<TopOrderedProductsDTO> getTopOrderedProductsThisMonth() {

        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();

        log.info("Fetching top ordered products for current month: {} to {}",
                startDate, endDate);

        List<TopOrderedProductsDTO> results =
                orderItemsRepository.findTopOrderedProductsByDateRange(startDate, endDate);

        return results.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    @CacheEvict(
            value = { "userOrders", "adminOrders", "orderStats", "topProducts"}, allEntries = true)
    @Transactional
    public OrderDTO buyNowOrder(BuyNowRequestDTO request) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        int qty = request.getQuantity();
        if (qty <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

     //  INVENTORY CHECK
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No inventory found"));

        if (inventory.getAvailableStock() < qty) {
            throw new RuntimeException("Insufficient stock for product: " + product.getProductName());
        }

       // PRODUCT DISCOUNT
        double mrp = product.getMRP();
        double productDiscountPerUnit = 0.0;

        Optional<ProductDiscount> activeDiscount =
                productDiscountRepository.findByProductId(product.getId())
                        .stream()
                        .filter(pd -> pd.getDiscount().getActive())
                        .findFirst();

        if (activeDiscount.isPresent()) {
            Discount d = activeDiscount.get().getDiscount();
            productDiscountPerUnit =
                    d.getDiscountType() == DiscountType.PERCENT
                            ? (mrp * d.getDiscountValue()) / 100
                            : d.getDiscountValue();
        }

        double discountedUnitPrice = mrp - productDiscountPerUnit;
        double subTotal = discountedUnitPrice * qty;

      // COUPON DISCOUNT
        double couponDiscount = 0.0;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCode(request.getCouponCode());

            if (coupon == null || !coupon.isActive() || coupon.getEndDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("Invalid or expired coupon");
            }

            if (subTotal < coupon.getMinOrderAmount()) {
                throw new RuntimeException("Minimum order amount is " + coupon.getMinOrderAmount());
            }

            couponDiscount =
                    coupon.getDiscountType() == DiscountType.PERCENT
                            ? (subTotal * coupon.getDiscountValue()) / 100
                            : coupon.getDiscountValue();

            if (coupon.getMaxDiscountAmount() != null) {
                couponDiscount = Math.min(couponDiscount, coupon.getMaxDiscountAmount());
            }
        }

    //   TAX
        double taxPercent = taxService.getTaxPercentByCategoryId(product.getCategory().getId());
        double taxableAmount = subTotal - couponDiscount;
        double taxAmount = (taxableAmount * taxPercent) / 100;

        double finalOrderAmount = taxableAmount + taxAmount;

    //   CREATE ORDER
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDate.now());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress(address);
        order.setOrderAmount(finalOrderAmount);

        Order savedOrder = orderRepository.save(order);

     //  ORDER ITEM
        OrderItems orderItem = new OrderItems();
        orderItem.setOrder(savedOrder);
        orderItem.setProduct(product);
        orderItem.setQuantity(qty);
        orderItem.setPrice(discountedUnitPrice);
        orderItem.setDiscount(productDiscountPerUnit);
        orderItem.setTax(taxAmount / qty);

        orderItemsRepository.save(orderItem);

      // RESERVE STOCK
        inventoryService.reserveStock(inventory.getId(), qty, savedOrder.getId());

        log.info(" BUY NOW ORDER PLACED");
        log.info("Product      : {}", product.getProductName());
        log.info("Qty          : {}", qty);
        log.info("MRP          : {}", mrp);
        log.info("Unit Price   : {}", discountedUnitPrice);
        log.info("Coupon Disc  : {}", couponDiscount);
        log.info("Tax          : {}", taxAmount);
        log.info("Final Amount : {}", finalOrderAmount);

        return convertToOrderDTO(savedOrder);
    }
}