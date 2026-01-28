package com.organics.products.controller;

import com.organics.products.dto.DiscountDTO;
import com.organics.products.dto.DiscountRequestDTO;
import com.organics.products.entity.Discount;
import com.organics.products.entity.EntityType;
import com.organics.products.service.DiscountService;
import com.organics.products.service.NotificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/discounts")
public class DiscountController {

    private final DiscountService discountService;

    private final NotificationService notificationService;

    public DiscountController(DiscountService discountService, NotificationService notificationService) {
        this.discountService = discountService;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<DiscountDTO> create(@RequestBody DiscountRequestDTO dto) {

        DiscountDTO saved = discountService.createDiscount(dto);

        notificationService.sendNotification(
                "ALL",
                "New Discount Available: " + saved.getName(),
                "ADMIN",
                "DISCOUNT_ALERT",
                "/products",
                "Promotions",
                "General",
                "Price Drop Alert!",
                EntityType.DISCOUNT,
                saved.getId()
        );

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/assign/product")
    public ResponseEntity<String> assignToProduct(@RequestParam Long productId,
                                                  @RequestParam Long discountId) {
        discountService.assignToProduct(productId, discountId);
        return ResponseEntity.ok("Discount assigned to product");
    }

    @PostMapping("/assign/category")
    public ResponseEntity<String> assignToCategory(@RequestParam Long categoryId,
                                                   @RequestParam Long discountId) {
        discountService.assignToCategory(categoryId, discountId);
        return ResponseEntity.ok("Discount assigned to category");
    }

    @PostMapping("/assign/cart")
    public ResponseEntity<String> assignToCart(@RequestParam Long cartId,
                                               @RequestParam Long discountId) {
        discountService.assignToCart(cartId, discountId);
        return ResponseEntity.ok("Discount assigned to cart");
    }
    @GetMapping
    public ResponseEntity<List<DiscountDTO>> getAll() {

        List<DiscountDTO> discounts = discountService.getAll();
        return ResponseEntity.ok(discounts);
    }


}
