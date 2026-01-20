package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.RatingSummaryDTO;
import com.organics.products.dto.ReviewResponseDTO;
import com.organics.products.entity.OrderStatus;
import com.organics.products.entity.Product;
import com.organics.products.entity.Reviews;
import com.organics.products.respository.OrderItemsRepository;
import com.organics.products.respository.ProductRepo;
import com.organics.products.respository.ReviewsRepository;
import com.organics.products.respository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class ReviewsService {

    @Autowired
    private ReviewsRepository reviewsRepository;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;





    public RatingSummaryDTO getRatingSummary(Long productId) {
        List<Object[]> result = reviewsRepository.getRatingSummary(productId);

        if (result.isEmpty()) {
            return new RatingSummaryDTO(0L, 0.0);
        }

        Object[] row = result.get(0);

        Long count = ((Number) row[0]).longValue();
        Double avg = ((Number) row[1]).doubleValue();

        return new RatingSummaryDTO(count, avg);
    }


    public void disableReview(Long reviewId) {
        Reviews review = reviewsRepository.findById(reviewId).orElseThrow();
        review.setIsEligible(false);
        reviewsRepository.save(review);
    }



    public ReviewResponseDTO addReviewWithImage(Long productId, Double rating,
                                                String description, MultipartFile image) throws Exception {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        boolean delivered = orderItemsRepository
                .existsByOrderUserIdAndProductIdAndOrderOrderStatus(
                        userId, productId, OrderStatus.DELIVERED);

        if (!delivered)
            throw new RuntimeException("You can review only after delivery");

        if (reviewsRepository.existsByUserIdAndProductId(userId, productId))
            throw new RuntimeException("You already reviewed this product");

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Reviews review = new Reviews();
        review.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
        review.setProduct(product);
        review.setRating(rating);
        review.setDescription(description);
        review.setIsEligible(true);
        review.setVerifiedPurchase(true);
        review.setCreatedAt(LocalDateTime.now());

        if (image != null && !image.isEmpty()) {
            String s3Key = s3Service.uploadFile(image);
            review.setReviewImage(s3Key);
        }

        Reviews saved = reviewsRepository.save(review);
        return mapToDTO(saved);
    }


    public List<ReviewResponseDTO> getProductReviews(Long productId) {
        return reviewsRepository.findByProductIdAndIsEligibleTrue(productId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private ReviewResponseDTO mapToDTO(Reviews review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setDescription(review.getDescription());
        dto.setRating(review.getRating());
        dto.setUserName(review.getUser().getDisplayName());
        dto.setCreatedAt(review.getCreatedAt());

        if (review.getReviewImage() != null) {
            dto.setImageUrl(s3Service.getFileUrl(review.getReviewImage())); // pre-signed
        }

        return dto;
    }


}
