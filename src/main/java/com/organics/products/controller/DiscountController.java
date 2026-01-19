package com.organics.products.controller;

import com.organics.products.dto.DiscountRequestDTO;
import com.organics.products.entity.Discount;
import com.organics.products.service.DiscountService;
import lombok.extern.slf4j.Slf4j;
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
    public Discount create(@RequestBody DiscountRequestDTO dto) {
        return discountService.createDiscount(dto);
    }

    @PostMapping("/assign/product")
    public String assignToProduct(@RequestParam Long productId,
                                  @RequestParam Long discountId) {
        discountService.assignToProduct(productId, discountId);
        return "Discount assigned to product";
    }

    @PostMapping("/assign/category")
    public String assignToCategory(@RequestParam Long categoryId,
                                   @RequestParam Long discountId) {
        discountService.assignToCategory(categoryId, discountId);
        return "Discount assigned to category";
    }

    @PostMapping("/assign/cart")
    public String assignToCart(@RequestParam Long cartId,
                               @RequestParam Long discountId) {
        discountService.assignToCart(cartId, discountId);
        return "Discount assigned to cart";
    }



}
