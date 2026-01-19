package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.ProductDTO;
import com.organics.products.entity.*;
import com.organics.products.respository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishListItemsRepository wishListItemsRepository;
    private final ProductRepo productRepository;
    private final InventoryRepository inventoryRepository;
    private final S3Service s3Service;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishListItemsRepository wishListItemsRepository,
            ProductRepo productRepository,
            InventoryRepository inventoryRepository,
            S3Service s3Service
    ) {
        this.wishlistRepository = wishlistRepository;
        this.wishListItemsRepository = wishListItemsRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.s3Service = s3Service;
    }

    // ➕ Add product to wishlist (FIXED for Set)
    public ProductDTO addToWishlist(Long productId) {

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

        wishlist.getWishListItems().add(item);
        wishlistRepository.save(wishlist);

        return mapToProductDTO(product);
    }

    // 📄 Get all wishlist products (Hibernate-safe)
    public List<ProductDTO> getMyWishlist() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Wishlist wishlist = wishlistRepository.findByUserIdWithProducts(userId)
                .orElseThrow(() -> new RuntimeException("Wishlist is empty"));

        return wishlist.getWishListItems()
                .stream()
                .filter(item ->
                        Boolean.TRUE.equals(item.getProduct().getStatus()) &&
                                item.getProduct().getCategory() != null &&
                                Boolean.TRUE.equals(item.getProduct().getCategory().getStatus())
                )
                .map(item -> mapToProductDTO(item.getProduct()))
                .toList();
    }

    public void removeFromWishlist(Long productId) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        wishListItemsRepository.deleteByWishlistIdAndProductId(
                wishlist.getId(), productId
        );
    }

    private ProductDTO mapToProductDTO(Product product) {

        ProductDTO r = new ProductDTO();

        r.setId(product.getId());
        r.setProductName(product.getProductName());
        r.setBrand(product.getBrand());
        r.setDescription(product.getDescription());
        r.setReturnDays(product.getReturnDays());

        r.setMrp(product.getMRP());
        r.setStatus(product.getStatus());
        r.setNetWeight(product.getNetWeight());
        r.setUnit(product.getUnit());

        if (product.getCategory() != null) {
            r.setCategoryId(product.getCategory().getId());
        }

        List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());
        if (inventories != null && !inventories.isEmpty()) {
            int totalStock = inventories.stream()
                    .mapToInt(inv -> inv.getAvailableStock() != null ? inv.getAvailableStock() : 0)
                    .sum();
            r.setAvailableStock(totalStock);
        } else {
            r.setAvailableStock(0);
        }

        if (product.getImages() != null) {
            r.setImageUrls(
                    product.getImages()
                            .stream()
                            .map(img -> s3Service.getFileUrl(img.getImageUrl()))
                            .toList()
            );
        }

        return r;
    }

}
