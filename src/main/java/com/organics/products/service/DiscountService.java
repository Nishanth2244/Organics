package com.organics.products.service;

import com.organics.products.dto.DiscountDTO;
import com.organics.products.dto.DiscountRequestDTO;
import com.organics.products.entity.*;
import com.organics.products.exception.BadRequestException;
import com.organics.products.exception.DiscountException;
import com.organics.products.exception.DiscountNotFoundException;
import com.organics.products.respository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final ProductDiscountRepository productDiscountRepository;
    private final CategoryDiscountRepository categoryDiscountRepository;
    private final CartDiscountRepository cartDiscountRepository;
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final CartRepository cartRepository;
    private final NotificationService notificationService;

    public DiscountService(DiscountRepository discountRepository,
                           ProductDiscountRepository productDiscountRepository,
                           CategoryDiscountRepository categoryDiscountRepository,
                           CartDiscountRepository cartDiscountRepository,
                           ProductRepo productRepo,
                           CategoryRepo categoryRepo,
                           CartRepository cartRepository, NotificationService notificationService) {
        this.discountRepository = discountRepository;
        this.productDiscountRepository = productDiscountRepository;
        this.categoryDiscountRepository = categoryDiscountRepository;
        this.cartDiscountRepository = cartDiscountRepository;
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.cartRepository = cartRepository;
        this.notificationService = notificationService;
    }

    @CacheEvict(value = {
                    "productFinalPrice", "productApplicableDiscount", "allDiscounts"}, allEntries = true)
    public DiscountDTO createDiscount(DiscountRequestDTO dto) {

        log.info("Creating discount: {}", dto.getName());

        if (discountRepository.existsByName(dto.getName())) {
            throw new DiscountException("Discount name already exists");
        }

        Discount discount = new Discount();
        discount.setName(dto.getName());
        discount.setDiscountType(dto.getDiscountType());
        discount.setDiscountValue(dto.getDiscountValue());
        discount.setScope(dto.getScope());
        discount.setActive(dto.getActive());
        discount.setValidFrom(dto.getValidFrom());
        discount.setValidTo(dto.getValidTo());

        Discount saved = discountRepository.save(discount);

        log.info("Discount created successfully: id={}", saved.getId());
        try {
            notificationService.sendNotification(
                    "ALL", // Receiver
                    "New Discount Available: " + saved.getName() + " (" + saved.getDiscountValue() + "% OFF)",
                    "ADMIN",
                    "DISCOUNT_ALERT",
                    "/products",
                    "Promotions",
                    "General",
                    "Price Drop Alert!",
                    EntityType.DISCOUNT,
                    saved.getId()
            );
        } catch (Exception e) {
            log.error("Failed to send discount notification", e);
        }

        return convertToDTO(saved);
    }

    @CacheEvict(value = {
            "productFinalPrice", "productApplicableDiscount", "allDiscounts"}, allEntries = true)
    public void assignToProduct(Long productId, Long discountId) {

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new BadRequestException("Product not found: " + productId));

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new DiscountNotFoundException("Discount not found: " + discountId));

        validateDiscount(discount, DiscountScope.PRODUCT);

        if (productDiscountRepository.existsByProductId(productId)) {
            throw new DiscountException("Product already has a discount");
        }

        ProductDiscount pd = new ProductDiscount();
        pd.setProduct(product);
        pd.setDiscount(discount);

        productDiscountRepository.save(pd);
    }

    @CacheEvict(value = {
            "productFinalPrice", "productApplicableDiscount", "allDiscounts"}, allEntries = true)
    public void assignToCategory(Long categoryId, Long discountId) {

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("Category not found: " + categoryId));

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new DiscountNotFoundException("Discount not found: " + discountId));

        validateDiscount(discount, DiscountScope.CATEGORY);

        if (categoryDiscountRepository.existsByCategoryId(categoryId)) {
            throw new DiscountException("Category already has discount");
        }

        CategoryDiscount cd = new CategoryDiscount();
        cd.setCategory(category);
        cd.setDiscount(discount);

        categoryDiscountRepository.save(cd);
    }

    @CacheEvict(value = {
            "productFinalPrice", "productApplicableDiscount", "allDiscounts"}, allEntries = true)
    public void assignToCart(Long cartId, Long discountId) {

        if (!cartRepository.existsById(cartId)) {
            throw new BadRequestException("Cart not found: " + cartId);
        }

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new DiscountNotFoundException("Discount not found: " + discountId));

        validateDiscount(discount, DiscountScope.CART);

        CartDiscount cd = new CartDiscount();
        cd.setCartId(cartId);
        cd.setDiscount(discount);

        cartDiscountRepository.save(cd);
    }

    @Cacheable(
            value = "productFinalPrice", key = "#product.id", unless = "#result == null")
    public double calculateFinalPrice(Product product) {

        if (product == null) {
            throw new BadRequestException("Product cannot be null");
        }

        double mrp = product.getMRP();

        List<ProductDiscount> pd = productDiscountRepository.findByProductId(product.getId());
        if (!pd.isEmpty()) {
            return applyDiscount(pd.get(0).getDiscount(), mrp);
        }

        if (product.getCategory() != null) {
            CategoryDiscount cd = categoryDiscountRepository
                    .findByCategoryId(product.getCategory().getId());
            if (cd != null) {
                return applyDiscount(cd.getDiscount(), mrp);
            }
        }

        return mrp;
    }

    public double applyCartDiscount(Long cartId, double cartTotal) {

        CartDiscount cd = cartDiscountRepository.findByCartId(cartId);

        if (cd == null) return cartTotal;

        Discount discount = cd.getDiscount();

        if (discount.getMinCartValue() != null &&
                cartTotal < discount.getMinCartValue()) {
            return cartTotal;
        }

        return applyDiscount(discount, cartTotal);
    }

    @Cacheable(
            value = "productApplicableDiscount", key = "#product.id", unless = "#result == null")
    public Discount getApplicableDiscount(Product product) {

        if (product == null) return null;

        List<ProductDiscount> pd = productDiscountRepository.findByProductId(product.getId());
        if (!pd.isEmpty()) {
            return pd.get(0).getDiscount();
        }

        if (product.getCategory() != null) {
            CategoryDiscount cd = categoryDiscountRepository
                    .findByCategoryId(product.getCategory().getId());
            if (cd != null) {
                return cd.getDiscount();
            }
        }

        return null;
    }


    private double applyDiscount(Discount discount, double price) {

        if (discount == null) return price;

        if (!Boolean.TRUE.equals(discount.getActive())) return price;

        LocalDateTime now = LocalDateTime.now();

        if (discount.getValidFrom() != null && now.isBefore(discount.getValidFrom())) return price;
        if (discount.getValidTo() != null && now.isAfter(discount.getValidTo())) return price;

        double finalPrice;

        if (discount.getDiscountType() == DiscountType.PERCENT) {
            finalPrice = price - (price * discount.getDiscountValue() / 100);
        } else {
            finalPrice = price - discount.getDiscountValue();
        }

        return Math.max(finalPrice, 0);
    }

    private void validateDiscount(Discount discount, DiscountScope expectedScope) {

        if (discount.getScope() != expectedScope) {
            throw new DiscountException("Discount is not " + expectedScope + " scope");
        }

        if (!Boolean.TRUE.equals(discount.getActive())) {
            throw new DiscountException("Discount is inactive");
        }

        LocalDateTime now = LocalDateTime.now();

        if (discount.getValidFrom() != null && now.isBefore(discount.getValidFrom())) {
            throw new DiscountException("Discount is not yet valid");
        }

        if (discount.getValidTo() != null && now.isAfter(discount.getValidTo())) {
            throw new DiscountException("Discount has expired");
        }
    }

    private DiscountDTO convertToDTO(Discount discount) {

        if (discount == null) return null;

        DiscountDTO dto = new DiscountDTO();
        dto.setId(discount.getId());
        dto.setName(discount.getName());
        dto.setDiscountType(discount.getDiscountType());
        dto.setDiscountValue(discount.getDiscountValue());
        dto.setScope(discount.getScope());
        dto.setActive(discount.getActive());
        dto.setValidFrom(discount.getValidFrom());
        dto.setValidTo(discount.getValidTo());

        return dto;
    }
    @Cacheable(value = "allDiscounts", unless = "#result == null || #result.isEmpty()")
    public List<DiscountDTO> getAll() {

        log.info("Fetching all discounts");

        List<Discount> discounts = discountRepository.findAll();

        if (discounts == null || discounts.isEmpty()) {
            log.warn("No discounts found");
            return List.of();
        }

        log.info("Found {} discounts", discounts.size());

        return discounts.stream()
                .map(this::convertToDTO)
                .toList();
    }

}
