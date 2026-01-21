package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.ProductDTO;
import com.organics.products.entity.*;
import com.organics.products.exception.BadRequestException;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.exception.UnauthorizedException;
import com.organics.products.respository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishListItemsRepository wishListItemsRepository;
    private final ProductRepo productRepository;
    private final InventoryRepository inventoryRepository;
    private final S3Service s3Service;
    private final DiscountService discountService;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishListItemsRepository wishListItemsRepository,
            ProductRepo productRepository,
            InventoryRepository inventoryRepository,
            S3Service s3Service,
            DiscountService discountService
    ) {
        this.wishlistRepository = wishlistRepository;
        this.wishListItemsRepository = wishListItemsRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.s3Service = s3Service;
        this.discountService = discountService;
    }

    public ProductDTO addToWishlist(Long productId) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));

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
            throw new BadRequestException("Product already in wishlist");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        WishListItems item = new WishListItems();
        item.setWishlist(wishlist);
        item.setProduct(product);

        wishlist.getWishListItems().add(item);
        wishlistRepository.save(wishlist);

        return mapToProductDTO(product);
    }

    public List<ProductDTO> getMyWishlist() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));

        Wishlist wishlist = wishlistRepository.findByUserIdWithProducts(userId)
                .orElse(null);

        if (wishlist == null || wishlist.getWishListItems() == null || wishlist.getWishListItems().isEmpty()) {
            return Collections.emptyList();
        }

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
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));

        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));

        boolean exists = wishListItemsRepository
                .existsByWishlistIdAndProductId(wishlist.getId(), productId);

        if (!exists) {
            throw new ResourceNotFoundException("Product not in wishlist");
        }

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

            Inventory inv = inventories.get(0);
            r.setInventoryId(inv.getId());

            int totalStock = inventories.stream()
                    .mapToInt(i -> i.getAvailableStock() != null ? i.getAvailableStock() : 0)
                    .sum();
            r.setAvailableStock(totalStock);
        } else {
            r.setAvailableStock(0);
            r.setInventoryId(null);
        }

        if (product.getImages() != null) {
            r.setImageUrls(
                    product.getImages()
                            .stream()
                            .map(img -> s3Service.getFileUrl(img.getImageUrl()))
                            .toList()
            );
        }

        Double finalPrice = discountService.calculateFinalPrice(product);
        r.setFinalPrice(finalPrice);

        if (finalPrice < product.getMRP()) {
            r.setDiscountAmount(product.getMRP() - finalPrice);

            Discount discount = discountService.getApplicableDiscount(product);
            if (discount != null) {
                r.setDiscountType(discount.getDiscountType());
            }
        } else {
            r.setDiscountAmount(null);
            r.setDiscountType(null);
        }

        return r;
    }
}
