package com.organics.products.controller;

import com.organics.products.dto.ProductDTO;
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

    // 🔒 Same endpoint: POST /api/user/wishlist/{productId}
    @PostMapping("/{productId}")
    public ResponseEntity<ProductDTO> addToWishlist(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                wishlistService.addToWishlist(productId)
        );
    }

    // 🔒 Same endpoint: GET /api/user/wishlist
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getWishlist() {
        return ResponseEntity.ok(
                wishlistService.getMyWishlist()
        );
    }

    // 🔒 Same endpoint: DELETE /api/user/wishlist/{productId}
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long productId
    ) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.noContent().build();
    }
}
