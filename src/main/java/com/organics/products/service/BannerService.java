package com.organics.products.service;

import com.organics.products.dto.BannerResponse;
import com.organics.products.entity.Banner;
import com.organics.products.entity.User;
import com.organics.products.respository.BannerRepository;
import com.organics.products.respository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BannerService(BannerRepository bannerRepository, S3Service s3Service, UserRepository userRepository, NotificationService notificationService) {
        this.bannerRepository = bannerRepository;
        this.s3Service = s3Service;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public BannerResponse add(
            MultipartFile[] images,
            String title,
            String description,
            String redirectUrl
    ) throws IOException {

        log.info("Starting banner creation: {}", title);

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Banner title is mandatory");
        }

        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("At least one banner image is required");
        }

        List<String> imageKeys = Arrays.stream(images)
                .map(file -> {
                    try {
                        return s3Service.uploadFile(file);
                    } catch (IOException e) {
                        log.error("Failed to upload image {}", file.getOriginalFilename(), e);
                        throw new RuntimeException("Failed to upload banner image");
                    }
                })
                .toList();

        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setDescription(description);
        banner.setImageUrls(imageKeys);
        banner.setRedirectUrl(redirectUrl);

        Banner saved = bannerRepository.save(banner);
        log.info("Broadcasting new banner notification to all users...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            notificationService.sendNotification(
                    String.valueOf(user.getId()),    // Receiver ID
                    "New Offer: " + saved.getTitle(),// Message
                    "Admin",                         // Sender
                    "PROMO",                         // Type
                    saved.getRedirectUrl(),          // Link
                    "MARKETING",                     // Category
                    "INFO",                          // Kind
                    "Check out our new updates!"     // Subject
            );
        }

        log.info("Banner created and notifications sent. Banner ID {}", saved.getId());
        log.info("Banner created successfully with ID {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        log.info("Fetching all banners");

        return bannerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void deleteBanner(Long id) {
        log.warn("Deleting banner ID {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));

        // Delete S3 images
        banner.getImageUrls().forEach(s3Service::deleteFile);

        bannerRepository.delete(banner);
        log.info("Banner deleted successfully ID {}", id);
    }

    public BannerResponse update(
            Long id,
            MultipartFile[] images,
            String title,
            String description,
            String redirectUrl
    ) throws IOException {

        log.info("Updating banner ID {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));

        if (title != null) banner.setTitle(title);
        if (description != null) banner.setDescription(description);
        if (redirectUrl != null) banner.setRedirectUrl(redirectUrl);

        // Replace images if new provided
        if (images != null && images.length > 0) {

            log.info("Replacing images for banner ID {}", id);

            banner.getImageUrls().forEach(s3Service::deleteFile);
            banner.getImageUrls().clear();

            List<String> newImageKeys = Arrays.stream(images)
                    .map(file -> {
                        try {
                            return s3Service.uploadFile(file);
                        } catch (IOException e) {
                            log.error("Failed to upload image {}", file.getOriginalFilename(), e);
                            throw new RuntimeException("Failed to upload banner image");
                        }
                    })
                    .collect(Collectors.toList());

            banner.setImageUrls(newImageKeys);
        }

        Banner updated = bannerRepository.save(banner);

        log.info("Banner updated successfully ID {}", id);
        return mapToResponse(updated);
    }

    // ✅ MAPPER
    private BannerResponse mapToResponse(Banner banner) {
        BannerResponse res = new BannerResponse();
        res.setId(banner.getId());
        res.setTitle(banner.getTitle());
        res.setDescription(banner.getDescription());
        res.setImageUrls(
                banner.getImageUrls()
                        .stream()
                        .map(s3Service::getFileUrl)
                        .toList()
        );
        res.setRedirectUrl(banner.getRedirectUrl());
        return res;
    }
}
