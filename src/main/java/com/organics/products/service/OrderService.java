package com.organics.products.service;

import com.organics.products.dto.*;


import com.organics.products.entity.*;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.config.SecurityUtil;

import com.organics.products.respository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    private AddressRepository addressRepository; // Add this

    //@Autowired
    //private WarehouseRepository warehouseRepository; // For pickup address

    @Transactional
    public OrderDTO placeOrder(OrderAddressRequestDTO orderRequest) {
        // Get current user
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

        // Get active cart with items
        Cart activeCart = cartRepository.findByUserAndIsActive(user, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active cart found"));

        // Fetch cart items with products
        List<CartItems> cartItems = cartItemRepository.findByCartIdWithProduct(activeCart.getId());

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot place order.");
        }

        // Create order FIRST
        Order order = new Order();
        order.setOrderDate(LocalDate.now());
        order.setOrderAmount(0.0); // Will calculate below
        order.setDescription(orderRequest.getDescription());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setUser(user);
        order.setCart(activeCart);
        order.setShippingAddress(selectedAddress);

        Order savedOrder = orderRepository.save(order);

        // Create order items from cart items and calculate total
        List<OrderItems> orderItemsList = new ArrayList<>();
        double totalAmount = 0.0;

        for (CartItems cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product == null) {
                throw new RuntimeException("Product not found for cart item: " + cartItem.getId());
            }

            OrderItems orderItem = new OrderItems();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());

            Double mrp = product.getMRP();
            Double afterDiscount = product.getMRP(); // Should this be getDiscountedPrice()?

            double price = (afterDiscount != null) ? afterDiscount :
                    (mrp != null) ? mrp : 0.0;

            orderItem.setPrice(price);

            if (mrp != null && afterDiscount != null) {
                orderItem.setDiscount(mrp - afterDiscount);
            } else {
                orderItem.setDiscount(0.0);
            }

            orderItem.setTax(price * 0.05);

            orderItemsList.add(orderItem);
            totalAmount += price * cartItem.getQuantity();
        }

        if (orderItemsList.isEmpty()) {
            throw new RuntimeException("No valid items to create order.");
        }

        // Update order amount
        savedOrder.setOrderAmount(totalAmount);

        // Save all order items
        orderItemsRepository.saveAll(orderItemsList);
        savedOrder.setOrderItems(orderItemsList);

        // **IMPORTANT: Clear cart items BEFORE saving cart**
        // Detach cart items from cart first
        activeCart.getItems().clear();

        // Save cart to persist the cleared items state
        cartRepository.save(activeCart);

        // **THEN** delete the cart items
        cartItemRepository.deleteAll(cartItems);

        // Deactivate the cart
        activeCart.setActive(false);
        activeCart.setTotalAmount(0.0);
        activeCart.setPayableAmount(0.0);
        activeCart.setDiscountAmount(0.0);
        cartRepository.save(activeCart);

        log.info("Order placed successfully. Order ID: {}, Amount: {}", savedOrder.getId(), totalAmount);
        // Integrate with Shiprocket for shipping
        try {
            ShiprocketCreateOrderResponse shiprocketResponse = shiprocketService.createOrder(savedOrder, user, selectedAddress);

            // Store Shiprocket details
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

            savedOrder.setOrderStatus(OrderStatus.CONFIRMED);

            log.info("✅ Shiprocket order created successfully!");

        } catch (Exception e) {
            log.error("Failed to create Shiprocket order: {}", e.getMessage());

            // Store only first 200 chars of error
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 200) {
                errorMessage = errorMessage.substring(0, 200);
            }

            savedOrder.setOrderStatus(OrderStatus.PENDING);
            savedOrder.setShiprocketOrderId("FAILED: " + errorMessage);

            log.warn("Order saved locally. Shiprocket error: {}", errorMessage);
        }

// Save the order
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
        // Try to get cart with items in one query
        Optional<Cart> cartOpt = cartRepository.findByIdWithItemsAndUser(userId);

        if (cartOpt.isPresent()) {
            return cartOpt.get();
        }

        // Fallback: get cart and then load items separately
        List<Cart> activeCarts = cartRepository.findByUserAndIsActive(
                userRepository.findById(userId).orElse(null),
                true
        );

        if (activeCarts.isEmpty()) {
            return null;
        }

        Cart cart = activeCarts.get(0);
        // Load items separately
        List<CartItems> items = cartItemRepository.findByCartIdWithProduct(cart.getId());
        cart.setItems(new HashSet<>(items));

        return cart;
    }

    /**
     * Get all orders for current user
     */
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

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Verify order belongs to user (unless admin)
        if (!order.getUser().getId().equals(userId) && !SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized access to order");
        }

        return convertToOrderDTO(order);
    }

    /**
     * Get all orders (for admin)
     */
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

    /**
     * Get orders by status
     */
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

    /**
     * Update order status
     */
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Update status
        order.setOrderStatus(status);

        // If status is SHIPPED and we have Shiprocket shipment, trigger pickup
        if (status == OrderStatus.SHIPPED && order.getShiprocketShipmentId() != null) {
            try {
                // Schedule pickup for tomorrow
                String pickupDate = LocalDate.now().plusDays(1).toString();
                shiprocketService.schedulePickup(order.getShiprocketShipmentId(), pickupDate);
                log.info("Pickup scheduled for shipment ID: {}", order.getShiprocketShipmentId());
            } catch (Exception e) {
                log.error("Failed to schedule pickup: {}", e.getMessage());
            }
        }

        // If status is CANCELLED and we have Shiprocket shipment, cancel it
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

    /**
     * Cancel order
     */
    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Verify order belongs to user
        if (!order.getUser().getId().equals(userId) && !SecurityUtil.isAdmin()) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        // Check if order can be cancelled
        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
                order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel order that is already shipped or delivered");
        }

        // Cancel Shiprocket shipment if exists
        if (order.getShiprocketShipmentId() != null) {
            try {
                shiprocketService.cancelShipment(order.getShiprocketShipmentId());
                log.info("Shiprocket shipment cancelled for order ID: {}", orderId);
            } catch (Exception e) {
                log.error("Failed to cancel Shiprocket shipment: {}", e.getMessage());
            }
        }

        // Update order status
        order.setOrderStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} cancelled by user {}", orderId, userId);

        return convertToOrderDTO(updatedOrder);
    }

    /**
     * Get shipping label for order
     */
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

        // Get delivery address from order
        Address deliveryAddress = order.getShippingAddress();
        if (deliveryAddress == null) {
            throw new RuntimeException("No shipping address found for order");
        }

        // Get pickup address from warehouse
        Address pickupAddress = getWarehouseAddress();

        // Calculate total weight (you might want to store product weight in Product entity)
        double totalWeight = 1.0; // Default weight

        return shiprocketService.getShippingRates(
                pickupAddress,
                deliveryAddress,
                totalWeight,
                10, 10, 10); // Default dimensions
    }


    private OrderDTO convertToOrderDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setOrderAmount(order.getOrderAmount());
        dto.setDescription(order.getDescription());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setUserId(order.getUser().getId());

        // User details
        User user = order.getUser();
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String userName = (firstName + " " + lastName).trim();

        dto.setUserName(userName.isEmpty() ? "Customer" : userName);
        dto.setUserEmail(user.getEmailId());
        dto.setUserPhone(user.getPhoneNumber());

        // ✅ Shipping Address - Add this
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

        // Shiprocket details
        dto.setShiprocketOrderId(order.getShiprocketOrderId());
        dto.setShiprocketShipmentId(order.getShiprocketShipmentId());
        dto.setShiprocketAwbCode(order.getShiprocketAwbCode());
        dto.setShiprocketCourierName(order.getShiprocketCourierName());
        dto.setShiprocketLabelUrl(order.getShiprocketLabelUrl());
        dto.setShiprocketTrackingUrl(order.getShiprocketTrackingUrl());

        // Order items
        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItems item : order.getOrderItems()) {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getProductName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setTax(item.getTax());
                itemDTO.setDiscount(item.getDiscount());
                itemDTO.setTotalPrice(item.getPrice() * item.getQuantity());
                itemDTOs.add(itemDTO);
            }
        }
        dto.setOrderItems(itemDTOs);

        return dto;
    }
    /**
     * Get order statistics for dashboard
     */
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

        // Validate dates
        if (startDate == null || endDate == null) {
            throw new RuntimeException("Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        List<DailyOrderStatsDTO> stats = new ArrayList<>();

        // Get daily order data from repository
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

        // Fill missing dates with zero values
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

        // Sort by date
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

        // Get total count
        Long totalCount = orderRepository.countByOrderDate(today);

        // Get total revenue
        Double totalRevenue = orderRepository.sumOrderAmountByDate(today);

        // Get count by status
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

        // Initialize with zeros for all statuses
        Map<String, Long> statusCount = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCount.put(status.toString(), 0L);
        }

        // Get actual counts from repository
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

        // Get total count
        Long totalCount = orderRepository.countByOrderDateBetween(startOfMonth, today);

        // Get total revenue
        Double totalRevenue = orderRepository.sumOrderAmountByDateRange(startOfMonth, today);

        // Get count by status
        Map<String, Long> statusCount = getThisMonthOrderCountByStatus();

        // Get daily average
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

        // Initialize with zeros for all statuses
        Map<String, Long> statusCount = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCount.put(status.toString(), 0L);
        }

        // Get actual counts from repository
        List<Object[]> counts = orderRepository.countByStatusAndDateRange(startOfMonth, today);
        for (Object[] count : counts) {
            OrderStatus status = (OrderStatus) count[0];
            Long countValue = (Long) count[1];
            statusCount.put(status.toString(), countValue);
        }

        return statusCount;
    }

    private Address getWarehouseAddress() {
        // This should come from your warehouse configuration
        // For now, return a default address
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

        // Get user's address
        Address deliveryAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Get warehouse address
        Address pickupAddress = getWarehouseAddress();

        return shiprocketService.checkServiceability(pickupAddress, deliveryAddress, 1.0);
    }
}