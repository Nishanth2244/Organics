package com.organics.products.controller;

import com.organics.products.dto.BannerCreateRequest;
import com.organics.products.dto.BannerResponse;
import com.organics.products.service.BannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/admin/banners")
@CrossOrigin(originPatterns = "*")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BannerResponse addBanner(
            @RequestPart("images") MultipartFile[] images,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String redirectUrl
    ) throws IOException {

        log.info("API request to create banner: {}", title);
        return bannerService.add(images, title, description, redirectUrl);
    }

    @GetMapping
    public Page<BannerResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return bannerService.getAllBanners(page,size);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        bannerService.deleteBanner(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BannerResponse> updateBanner(
            @PathVariable Long id,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String redirectUrl
    ) throws IOException {

        log.info("API request to update banner ID {}", id);
        return ResponseEntity.ok(
                bannerService.update(id, images, title, description, redirectUrl)
        );
    }



}
