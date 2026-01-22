package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.RatingSummaryDTO;
import com.organics.products.dto.ReviewResponseDTO;
import com.organics.products.entity.EntityType;
import com.organics.products.entity.OrderStatus;
import com.organics.products.entity.Product;
import com.organics.products.entity.Reviews;
import com.organics.products.respository.OrderItemsRepository;
import com.organics.products.respository.ProductRepo;
import com.organics.products.respository.ReviewsRepository;
import com.organics.products.respository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private NotificationService notificationService;

    @Value("${app.admin.notification-receiver}")
    private String adminReceiver;

    // ================= RATING SUMMARY =================

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

    // ================= DISABLE REVIEW =================

    public void disableReview(Long reviewId) {

        Reviews review = reviewsRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setIsEligible(false);
        reviewsRepository.save(review);

        // ✅ ADMIN notification
        notificationService.sendNotification(
                adminReceiver,
                "Review ID " + reviewId + " has been disabled",
                "SYSTEM",
                "ALERT",
                "/admin/reviews",
                "REVIEW",
                "SYSTEM",
                "Review Disabled",
                EntityType.REVIEW,
                reviewId
        );
    }


    public ReviewResponseDTO addReviewWithImage(Long productId,
                                                Double rating,
                                                String description,
                                                MultipartFile image) throws Exception {

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

        notificationService.sendNotification(
                String.valueOf(userId),
                "Thank you for reviewing " + product.getProductName(),
                "SYSTEM",
                "SUCCESS",
                "/products/" + productId,
                "REVIEW",
                "SYSTEM",
                "Review Submitted",
                EntityType.REVIEW,
                saved.getId()
        );

        notificationService.sendNotification(
                adminReceiver,
                "New review submitted for product: " + product.getProductName(),
                "SYSTEM",
                "INFO",
                "/admin/reviews",
                "REVIEW",
                "SYSTEM",
                "New Review",
                EntityType.REVIEW,
                saved.getId()
        );

        return mapToDTO(saved);
    }

    // ================= GET PRODUCT REVIEWS =================

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
            dto.setImageUrl(
                    s3Service.getFileUrl(review.getReviewImage())
            );
        }

        return dto;
    }
}
