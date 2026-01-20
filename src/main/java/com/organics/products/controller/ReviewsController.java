package com.organics.products.controller;

import com.organics.products.dto.RatingSummaryDTO;
import com.organics.products.dto.ReviewResponseDTO;
import com.organics.products.service.ReviewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewsController {

    @Autowired
    private ReviewsService reviewsService;

    @PostMapping(value = "/{productId}", consumes = "multipart/form-data")
    public ReviewResponseDTO addReview(@PathVariable Long productId,
                             @RequestParam Double rating,
                             @RequestParam String description,
                             @RequestParam(required = false) MultipartFile image) throws Exception {
        return reviewsService.addReviewWithImage(productId, rating, description, image);
    }


    @GetMapping("/product/{productId}")
    public List<ReviewResponseDTO> getReviews(@PathVariable Long productId) {
        return reviewsService.getProductReviews(productId);
    }

    @GetMapping("/summary/{productId}")
    public RatingSummaryDTO getSummary(@PathVariable Long productId) {
        return reviewsService.getRatingSummary(productId);
    }


    @PutMapping("/admin/disable/{reviewId}")
    public void disableReview(@PathVariable Long reviewId) {
        reviewsService.disableReview(reviewId);
    }
}
