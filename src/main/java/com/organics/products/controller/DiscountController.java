package com.organics.products.controller;

import com.organics.products.dto.DiscountDTO;
import com.organics.products.dto.DiscountRequestDTO;
import com.organics.products.entity.Discount;
import com.organics.products.service.DiscountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/admin/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping
    public ResponseEntity<DiscountDTO> create(@RequestBody @Valid DiscountRequestDTO dto) {
        return ResponseEntity.ok(discountService.createDiscount(dto));
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
    public ResponseEntity<Page<DiscountDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(discountService.getAll(page, size));
    }

}
