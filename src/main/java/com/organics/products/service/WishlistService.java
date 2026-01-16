package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.WishlistProductResponse;
import com.organics.products.entity.Product;
import com.organics.products.entity.ProductImage;
import com.organics.products.entity.User;
import com.organics.products.entity.WishListItems;
import com.organics.products.entity.Wishlist;
import com.organics.products.respository.ProductRepo;
import com.organics.products.respository.WishListItemsRepository;
import com.organics.products.respository.WishlistRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishListItemsRepository wishListItemsRepository;
    private final ProductRepo productRepository;
    private final S3Service s3Service;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishListItemsRepository wishListItemsRepository,
            ProductRepo productRepository, S3Service s3Service1
    ) {
        this.wishlistRepository = wishlistRepository;
        this.wishListItemsRepository = wishListItemsRepository;
        this.productRepository = productRepository;
        this.s3Service = s3Service1;
    }


    public WishlistProductResponse addToWishlist(Long productId) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Wishlist wishlist = wishlistRepository
                .findByUserId(userId)
                .orElseGet(() -> {
                    Wishlist w = new Wishlist();
                    User user = new User();
                    user.setId(userId);
                    w.setUser(user);
                    return wishlistRepository.save(w);
                });


        if (wishListItemsRepository
                .existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            throw new RuntimeException("Product already in wishlist");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        WishListItems item = new WishListItems();
        item.setWishlist(wishlist);
        item.setProduct(product);
        wishListItemsRepository.save(item);

        return mapToWishlistProductResponse(product);
    }


    public List<WishlistProductResponse> getMyWishlist() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wishlist empty"));

        return wishlist.getWishListItems()
                .stream()
                .filter(item ->
                        Boolean.TRUE.equals(item.getProduct().getStatus()) &&
                                item.getProduct().getCategory() != null &&
                                Boolean.TRUE.equals(item.getProduct().getCategory().getStatus())
                )
                .map(item -> mapToWishlistProductResponse(item.getProduct()))
                .toList();
    }


    public void removeFromWishlist(Long productId) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        wishListItemsRepository.deleteByWishlistIdAndProductId(
                wishlist.getId(),
                productId
        );
    }


    private WishlistProductResponse mapToWishlistProductResponse(Product product) {

        WishlistProductResponse r = new WishlistProductResponse();

        r.setId(product.getId());
        r.setProductName(product.getProductName());
        r.setBrand(product.getBrand());
        r.setDescription(product.getDescription());

        r.setNetWeight(product.getNetWeight());
        r.setUnit(product.getUnit());

        r.setReturnDays(product.getReturnDays());
        r.setMrp(product.getMRP());
        r.setAfterDiscount(product.getAfterDiscount());
        r.setDiscount(product.getDiscount());
        r.setStatus(product.getStatus());

        if (product.getCategory() != null) {
            r.setCategoryId(product.getCategory().getId());
        }

        r.setImageUrls(
                product.getImages()
                        .stream()
                        .map(img -> s3Service.getFileUrl(img.getImageUrl()))
                        .toList()
        );

        return r;
    }
}