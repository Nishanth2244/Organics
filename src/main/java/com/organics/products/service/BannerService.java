package com.organics.products.service;

import com.organics.products.dto.BannerResponse;
import com.organics.products.entity.Banner;
import com.organics.products.entity.EntityType;
import com.organics.products.entity.User;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.BannerRepository;
import com.organics.products.respository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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

    public BannerService(BannerRepository bannerRepository,
                         S3Service s3Service,
                         UserRepository userRepository,
                         NotificationService notificationService) {
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

        log.info("Starting banner creation: title={}", title);

        if (title == null || title.trim().isEmpty()) {
            log.warn("Banner title is empty");
            throw new IllegalArgumentException("Banner title is mandatory");
        }

        if (images == null || images.length == 0) {
            log.warn("No banner images provided");
            throw new IllegalArgumentException("At least one banner image is required");
        }

        List<String> imageKeys = Arrays.stream(images)
                .map(file -> {
                    try {
                        log.info("Uploading banner image: {}", file.getOriginalFilename());
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

        log.info("Banner saved successfully. bannerId={}", saved.getId());
        log.info("Broadcasting new banner notification to all users...");

        List<User> users = userRepository.findAll();

        if (users == null || users.isEmpty()) {
            log.warn("No users found to notify for new banner");
        } else {
            for (User user : users) {
                notificationService.sendNotification(
                        String.valueOf(user.getId()),
                        "New Offer: " + saved.getTitle(),
                        "ADMIN",
                        "PROMO",
                        saved.getRedirectUrl(),
                        "MARKETING",
                        "SYSTEM",
                        "New Banner Available!",
                        EntityType.BANNER,
                        saved.getId()
                );
            }
            log.info("Banner notifications sent to {} users", users.size());
        }

        log.info("Banner created successfully with ID {}", saved.getId());
        return mapToResponse(saved);
    }


    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {

        log.info("Fetching all banners");

        List<Banner> banners = bannerRepository.findAll();

        if (banners == null || banners.isEmpty()) {
            log.info("No banners found");
            return List.of(); // safe empty list
        }

        log.info("Found {} banners", banners.size());

        return banners.stream()
                .map(this::mapToResponse)
                .toList();
    }




    public void deleteBanner(Long id) {

        log.warn("Deleting banner ID {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Banner not found for id={}", id);
                    return new ResourceNotFoundException("Banner not found with id: " + id);
                });

        if (banner.getImageUrls() != null && !banner.getImageUrls().isEmpty()) {
            log.info("Deleting {} images from S3 for bannerId={}", banner.getImageUrls().size(), id);
            banner.getImageUrls().forEach(s3Service::deleteFile);
        } else {
            log.info("No images found in bannerId={} to delete", id);
        }

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
                .orElseThrow(() -> {
                    log.warn("Banner not found for id={}", id);
                    return new ResourceNotFoundException("Banner not found with id: " + id);
                });

        if (title != null) {
            banner.setTitle(title);
            log.info("Updated title for bannerId={}", id);
        }

        if (description != null) {
            banner.setDescription(description);
            log.info("Updated description for bannerId={}", id);
        }

        if (redirectUrl != null) {
            banner.setRedirectUrl(redirectUrl);
            log.info("Updated redirectUrl for bannerId={}", id);
        }

        if (images != null && images.length > 0) {

            log.info("Adding new images to banner ID {}", id);


            if (banner.getImageUrls() == null) {
                banner.setImageUrls(new ArrayList<>());
            }

            List<String> newImageKeys = Arrays.stream(images)
                    .map(file -> {
                        try {
                            log.info("Uploading new banner image: {}", file.getOriginalFilename());
                            return s3Service.uploadFile(file);
                        } catch (IOException e) {
                            log.error("Failed to upload image {}", file.getOriginalFilename(), e);
                            throw new RuntimeException("Failed to upload banner image");
                        }
                    })
                    .toList();


            banner.getImageUrls().addAll(newImageKeys);
        }


        Banner updated = bannerRepository.save(banner);

        log.info("Banner updated successfully ID {}", id);
        return mapToResponse(updated);
    }


    private BannerResponse mapToResponse(Banner banner) {

        BannerResponse res = new BannerResponse();
        res.setId(banner.getId());
        res.setTitle(banner.getTitle());
        res.setDescription(banner.getDescription());

        if (banner.getImageUrls() == null || banner.getImageUrls().isEmpty()) {
            res.setImageUrls(List.of());
        } else {
            res.setImageUrls(
                    banner.getImageUrls()
                            .stream()
                            .map(s3Service::getFileUrl)
                            .toList()
            );
        }

        res.setRedirectUrl(banner.getRedirectUrl());
        return res;
    }
}
