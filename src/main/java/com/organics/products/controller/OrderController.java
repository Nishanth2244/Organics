package com.organics.products.controller;

import com.organics.products.dto.*;
import com.organics.products.entity.OrderStatus;
import com.organics.products.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<OrderDTO> placeOrder(@RequestBody OrderAddressRequestDTO orderRequest) {
        log.info("Placing order with address ID: {}", orderRequest.getAddressId());
        OrderDTO order = orderService.placeOrder(orderRequest);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderDTO>> getUserOrders(@RequestParam(defaultValue = "0")int page,@RequestParam(defaultValue = "10")int size) {
        Page<OrderDTO> orders = orderService.getUserOrders(page,size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long orderId) {
        OrderDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/track/{orderId}")
    public ResponseEntity<ShiprocketTrackingResponse> trackOrder(@PathVariable Long orderId) {
        ShiprocketTrackingResponse trackingInfo = orderService.trackOrder(orderId);
        return ResponseEntity.ok(trackingInfo);
    }

//    @GetMapping("/{orderId}/label")
//    public ResponseEntity<byte[]> getShippingLabel(@PathVariable Long orderId) {
//        byte[] label = orderService.getShippingLabel(orderId);
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename=\"shipping_label_" + orderId + ".pdf\"")
//                .body(label);
//    }


    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        OrderDTO order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long orderId) {
        OrderDTO order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(@RequestParam(defaultValue = "0")int page,@RequestParam(defaultValue = "10")int size) {
        Page<OrderDTO> orders = orderService.getAllOrders(page,size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/status")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@RequestParam OrderStatus status) {
        List<OrderDTO> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }
    @GetMapping("/admin/statistics")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        Map<String, Object> statistics = orderService.getOrderStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/admin/date")
    public ResponseEntity<List<OrderDTO>> getOrdersOnDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<OrderDTO> orders = orderService.getOrdersOnDate(date);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/daily-statistics")
    public ResponseEntity<List<DailyOrderStatsDTO>> getDailyOrderStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE ) LocalDate endDate) {

        List<DailyOrderStatsDTO> stats = orderService.getDailyOrderStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/admin/today")
    public ResponseEntity<List<OrderDTO>> getTodayOrders() {
        List<OrderDTO> orders = orderService.getTodayOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/this-month")
    public ResponseEntity<List<OrderDTO>> getThisMonthOrders() {
        List<OrderDTO> orders = orderService.getThisMonthOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/today/count")
    public ResponseEntity<Map<String, Object>> getTodayOrderCount() {
        Map<String, Object> count = orderService.getTodayOrderCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/admin/monthly/count")
    public ResponseEntity<Map<String, Object>> getMonthlyOrderCount() {
        Map<String, Object> count = orderService.getMonthlyOrderCount();
        return ResponseEntity.ok(count);
    }

    // Add these endpoints to your OrderController class

    @GetMapping("/admin/topproducts")
    public ResponseEntity<List<TopOrderedProductsDTO>> getTopOrderedProducts(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long inventoryId) {

        List<TopOrderedProductsDTO> topProducts = orderService.getTopOrderedProducts(limit, startDate, endDate, inventoryId);
        return ResponseEntity.ok(topProducts);
    }

    @GetMapping("/admin/top-products/last30days")
    public ResponseEntity<List<TopOrderedProductsDTO>> getTopOrderedProductsLast30Days() {
        List<TopOrderedProductsDTO> topProducts = orderService.getTopOrderedProductsLast30Days();
        return ResponseEntity.ok(topProducts);
    }

    @GetMapping("/admin/topproducts/thismonth")
    public ResponseEntity<List<TopOrderedProductsDTO>> getTopOrderedProductsThisMonth() {
        List<TopOrderedProductsDTO> topProducts = orderService.getTopOrderedProductsThisMonth();
        return ResponseEntity.ok(topProducts);
    }
}