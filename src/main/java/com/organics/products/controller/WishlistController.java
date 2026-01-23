package com.organics.products.controller;

import com.organics.products.dto.ProductDTO;
import com.organics.products.service.WishlistService;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<ProductDTO> addToWishlist(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                wishlistService.addToWishlist(productId)
        );
    }

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                wishlistService.getMyWishlist(page, size)
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
