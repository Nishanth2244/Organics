package com.organics.products.controller;

import com.organics.products.dto.WishlistProductResponse;
import com.organics.products.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }


    @PostMapping("/{productId}")
    public ResponseEntity<WishlistProductResponse> addToWishlist(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                wishlistService.addToWishlist(productId)
        );
    }


    @GetMapping
    public ResponseEntity<List<WishlistProductResponse>> getWishlist() {
        return ResponseEntity.ok(
                wishlistService.getMyWishlist()
        );
    }


    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long productId
    ) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.noContent().build();
    }
}
