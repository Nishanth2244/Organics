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
import com.organics.products.dto.*;
import com.organics.products.entity.*;
import com.organics.products.respository.*;
import org.springframework.beans.factory.annotation.Autowired;
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



    @Transactional
    public OrderDTO placeOrder(OrderAddressRequestDTO orderRequest) {
        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Address selectedAddress = null;
        if (orderRequest.getAddressId() != null) {
            selectedAddress = addressRepository.findById(orderRequest.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

            if (!selectedAddress.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Address doesn't belong to user");
            }
        } else {
            selectedAddress = addressRepository.findPrimaryAddressByUserId(userId)
                    .orElse(addressRepository.findFirstByUserId(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("No address found for user")));
        }


        Cart activeCart = cartRepository.findByUserAndIsActive(user, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active cart found"));


        List<CartItems> cartItems = cartItemRepository.findByCartIdWithProduct(activeCart.getId());
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot place order.");
        }

        List<Inventory> inventoriesToReserve = new ArrayList<>();
        Map<Long, Integer> productQuantities = new HashMap<>();

        for (CartItems cartItem : cartItems) {
            Product product = cartItem.getInventory().getProduct();

            List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());

            if (inventories.isEmpty()) {
                throw new RuntimeException("No inventory found for product: " + product.getProductName());
            }


            Inventory inventory = inventories.get(0);

            if (inventory.getAvailableStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " +
                        product.getProductName() +
                        ". Available: " + inventory.getAvailableStock() +
                        ", Requested: " + cartItem.getQuantity());
            }


            inventoriesToReserve.add(inventory);
            productQuantities.put(inventory.getId(), cartItem.getQuantity());
        }

        Order order = new Order();
        order.setOrderDate(LocalDate.now());
        order.setOrderAmount(0.0);
        order.setDescription(orderRequest.getDescription());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setUser(user);
        order.setCart(activeCart);
        order.setShippingAddress(selectedAddress);

//        if (cartItems.getImages() != null && !product.getImages().isEmpty()) {
//            order.setImageUrl(s3Service.getFileUrl(product.getImages().get(0).getImageUrl()));
//        }

        Order savedOrder = orderRepository.save(order);
        List<OrderItems> orderItemsList = new ArrayList<>();
        double totalAmount = 0.0;
        double totalTax = 0.0;
        double totalDiscount = 0.0;

        for (CartItems cartItem : cartItems) {
            Product product = cartItem.getInventory().getProduct();

            OrderItems orderItem = new OrderItems();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());

//            if (orderItem.getProduct().getImages()!= null && !orderItem.getProduct().getImages().isEmpty()) {
//          orderItem.setImageUrl(s3Service.getFileUrl(product.getImages().get(0).getImageUrl()));
//      }

            Double mrp = product.getMRP();
            Double sellingPrice = cartItem.getCart().getPayableAmount();
            if (sellingPrice == null || sellingPrice == 0.0) {
                sellingPrice = mrp;
            }


            double price = sellingPrice != null ? sellingPrice :
                    (mrp != null ? mrp : 0.0);

            if (price <= 0) {
                throw new RuntimeException("Invalid price for product: " + product.getProductName());
            }

            orderItem.setPrice(price);
            double discountPerItem = 0.0;
            if (mrp != null && sellingPrice != null && mrp > sellingPrice) {
                discountPerItem = mrp - sellingPrice;
            }
            orderItem.setDiscount(discountPerItem);

            double taxPerItem = price * 0.05;
            orderItem.setTax(taxPerItem);

            double itemTotal = price * cartItem.getQuantity();
            double itemDiscount = discountPerItem * cartItem.getQuantity();
            double itemTax = taxPerItem * cartItem.getQuantity();

            totalAmount += itemTotal;
            totalDiscount += itemDiscount;
            totalTax += itemTax;

            orderItemsList.add(orderItem);
        }

        if (orderItemsList.isEmpty()) {
            throw new RuntimeException("No valid items to create order.");
        }
        Double cartDiscount = activeCart.getDiscountAmount();
        if (cartDiscount != null && cartDiscount > 0) {
            totalDiscount += cartDiscount;
        }

        double grandTotal = totalAmount;
        Double cartPayableAmount = activeCart.getPayableAmount();
        if (cartPayableAmount != null && Math.abs(cartPayableAmount - grandTotal) > 1.0) {
            log.warn("Cart payable amount (${}) differs from calculated amount (${})",
                    cartPayableAmount, grandTotal);
        }

        savedOrder.setOrderAmount(grandTotal);

        for (Inventory inventory : inventoriesToReserve) {
            Integer quantity = productQuantities.get(inventory.getId());
            try {
                inventoryService.reserveStock(inventory.getId(), quantity, savedOrder.getId());
                log.info("Stock reserved for product {}: {} units. Available: {}, Reserved: {}",
                        inventory.getProduct().getProductName(), quantity,
                        inventory.getAvailableStock() - quantity,
                        inventory.getReservedStock() + quantity);
            } catch (Exception e) {
                orderRepository.delete(savedOrder);
                throw new RuntimeException("Failed to reserve stock for product: " +
                        inventory.getProduct().getProductName() + ". " + e.getMessage());
            }
        }

        orderItemsRepository.saveAll(orderItemsList);
        savedOrder.setOrderItems(orderItemsList);

        log.info("Order placed successfully. Order ID: {}, Amount: {}, Tax: {}, Discount: {}",
                savedOrder.getId(), grandTotal, totalTax, totalDiscount);
        //savedOrder.setOrderStatus(OrderStatus.PENDING);
        orderRepository.save(savedOrder);




        try {
            ShiprocketCreateOrderResponse shiprocketResponse = shiprocketService.createOrder(savedOrder, user, selectedAddress);

            if (shiprocketResponse.getOrderId() != null && shiprocketResponse.getOrderId() > 0) {
                savedOrder.setShiprocketOrderId(String.valueOf(shiprocketResponse.getOrderId()));
            }
            if (shiprocketResponse.getShipmentId() != null) {
                savedOrder.setShiprocketShipmentId(shiprocketResponse.getShipmentId());
            }
            if (shiprocketResponse.getAwbCode() != null) {
                savedOrder.setShiprocketAwbCode(shiprocketResponse.getAwbCode());
                savedOrder.setShiprocketTrackingUrl("https://shiprocket.co/tracking/" + shiprocketResponse.getAwbCode());
            }
            if (shiprocketResponse.getCourierName() != null) {
                savedOrder.setShiprocketCourierName(shiprocketResponse.getCourierName());
            }

            //savedOrder.setOrderStatus(OrderStatus.PENDING);


            log.info("Shiprocket order created successfully!");

        } catch (Exception e) {
            log.error("Failed to create Shiprocket order: {}", e.getMessage());

            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 200) {
                errorMessage = errorMessage.substring(0, 200);
            }
            //savedOrder.setOrderStatus(OrderStatus.PENDING);
            savedOrder.setShiprocketOrderId("FAILED: " + errorMessage);

            log.warn("Order saved locally. Shiprocket error: {}", errorMessage);
        }

        try {
            orderRepository.save(savedOrder);
        } catch (Exception e) {
            log.error("Error saving order: {}", e.getMessage());
            // Try without Shiprocket details
            savedOrder.setShiprocketOrderId(null);
            savedOrder.setShiprocketTrackingUrl(null);
            orderRepository.save(savedOrder);
        }
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

    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders() {
        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(user);

        return orders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }

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
        dto.setPaymentStatus(dto.getPaymentStatus());
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
//                if(itemDTO.setImageUrl()!=null &&  itemDTO.getImageUrl().isEmpty()){
//                    itemDTO.setImageUrl(s3Service.getFileUrl(order.getOrderItems().getP));
//
//                }


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

                double lineTotal = itemTotal + itemTax - itemDiscount;
                itemDTO.setTotalPrice(lineTotal);

                orderSubtotal += itemTotal;
                orderTax += itemTax;
                orderItemDiscount += itemDiscount;

                itemDTOs.add(itemDTO);
            }
        }
        dto.setOrderItems(itemDTOs);
        double cartLevelDiscount = 0.0;
        if (order.getCart() != null && order.getCart().getDiscountAmount() != null) {
            cartLevelDiscount = order.getCart().getDiscountAmount();
        }

        double totalDiscount = orderItemDiscount + cartLevelDiscount;

        dto.setSubtotal(orderSubtotal);
        dto.setTotalTax(orderTax);
        dto.setItemDiscount(orderItemDiscount);
        dto.setCartDiscount(cartLevelDiscount);
        dto.setTotalDiscount(totalDiscount);
        dto.setGrandTotal(order.getOrderAmount());
        return dto;
    }

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


    // Add this method to your OrderService class

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

    @Transactional(readOnly = true)
    public List<TopOrderedProductsDTO> getTopOrderedProductsThisMonth() {
        if (!SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();

        List<TopOrderedProductsDTO> results = orderItemsRepository.findTopOrderedProductsByDateRange(startDate, endDate);

        return results.stream()
                .limit(10)
                .collect(Collectors.toList());
    }
}