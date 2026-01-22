package com.organics.products.service;

import com.organics.products.dto.DiscountRequestDTO;
import com.organics.products.entity.*;
import com.organics.products.exception.BadRequestException;
import com.organics.products.exception.DiscountException;
import com.organics.products.exception.DiscountNotFoundException;
import com.organics.products.respository.*;
import lombok.extern.slf4j.Slf4j;
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

    public DiscountService(DiscountRepository discountRepository,
                           ProductDiscountRepository productDiscountRepository,
                           CategoryDiscountRepository categoryDiscountRepository,
                           CartDiscountRepository cartDiscountRepository,
                           ProductRepo productRepo,
                           CategoryRepo categoryRepo) {
        this.discountRepository = discountRepository;
        this.productDiscountRepository = productDiscountRepository;
        this.categoryDiscountRepository = categoryDiscountRepository;
        this.cartDiscountRepository = cartDiscountRepository;
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }


    public Discount createDiscount(DiscountRequestDTO dto) {

        log.info("Creating discount: {}", dto.getName());

        if (dto.getName() == null || dto.getName().isBlank()) {
            log.warn("Discount creation failed: name is empty");
            throw new BadRequestException("Discount name cannot be empty");
        }

        if (dto.getDiscountValue() == null || dto.getDiscountValue() <= 0) {
            log.warn("Invalid discount value: {}", dto.getDiscountValue());
            throw new BadRequestException("Discount value must be greater than zero");
        }

        if (dto.getDiscountType() == null) {
            log.warn("Discount type missing");
            throw new BadRequestException("Discount type is required");
        }

        if (dto.getScope() == null) {
            log.warn("Discount scope missing");
            throw new BadRequestException("Discount scope is required");
        }

        Discount discount = new Discount();
        discount.setName(dto.getName());
        discount.setDiscountType(dto.getDiscountType());
        discount.setDiscountValue(dto.getDiscountValue());
        discount.setScope(dto.getScope());
        discount.setActive(dto.getActive());
        discount.setValidFrom(dto.getValidFrom());
        discount.setValidTo(dto.getValidTo());
        discount.setMinCartValue(dto.getMinCartValue());

        Discount saved = discountRepository.save(discount);

        log.info("Discount created successfully: id={}", saved.getId());

        return saved;
    }


    public void assignToProduct(Long productId, Long discountId) {

        log.info("Assigning discount {} to product {}", discountId, productId);

        if (productDiscountRepository.existsByProductId(productId)) {
            log.warn("Product {} already has a discount", productId);
            throw new DiscountException("Product already has a discount");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new DiscountNotFoundException("Product not found: " + productId);
                });

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> {
                    log.warn("Discount not found: {}", discountId);
                    return new DiscountNotFoundException("Discount not found: " + discountId);
                });

        if (discount.getScope() != DiscountScope.PRODUCT) {
            log.warn("Discount {} is not a PRODUCT scope discount", discountId);
            throw new DiscountException("Not a product discount");
        }

        ProductDiscount pd = new ProductDiscount();
        pd.setProduct(product);
        pd.setDiscount(discount);

        productDiscountRepository.save(pd);

        log.info("Discount {} assigned to product {}", discountId, productId);
    }

    public void assignToCategory(Long categoryId, Long discountId) {

        log.info("Assigning discount {} to category {}", discountId, categoryId);

        if (categoryDiscountRepository.existsByCategoryId(categoryId)) {
            log.warn("Category {} already has a discount", categoryId);
            throw new DiscountException("Category already has discount");
        }

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found: {}", categoryId);
                    return new DiscountNotFoundException("Category not found: " + categoryId);
                });

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> {
                    log.warn("Discount not found: {}", discountId);
                    return new DiscountNotFoundException("Discount not found: " + discountId);
                });

        if (discount.getScope() != DiscountScope.CATEGORY) {
            log.warn("Discount {} is not CATEGORY scope", discountId);
            throw new DiscountException("Not a category discount");
        }

        CategoryDiscount cd = new CategoryDiscount();
        cd.setCategory(category);
        cd.setDiscount(discount);

        categoryDiscountRepository.save(cd);

        log.info("Discount {} assigned to category {}", discountId, categoryId);
    }

    public void assignToCart(Long cartId, Long discountId) {

        log.info("Assigning discount {} to cart {}", discountId, cartId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> {
                    log.warn("Discount not found: {}", discountId);
                    return new DiscountNotFoundException("Discount not found: " + discountId);
                });

        if (discount.getScope() != DiscountScope.CART) {
            log.warn("Discount {} is not CART scope", discountId);
            throw new DiscountException("Not a cart discount");
        }

        CartDiscount cd = new CartDiscount();
        cd.setCartId(cartId);
        cd.setDiscount(discount);

        cartDiscountRepository.save(cd);

        log.info("Discount {} assigned to cart {}", discountId, cartId);
    }


    public double calculateFinalPrice(Product product) {

        if (product == null) {
            log.warn("Product is null while calculating final price");
            throw new BadRequestException("Product cannot be null");
        }

        double mrp = product.getMRP();

        List<ProductDiscount> pd = productDiscountRepository.findByProductId(product.getId());
        if (!pd.isEmpty()) {
            log.debug("Applying product discount for product {}", product.getId());
            return applyDiscount(pd.get(0).getDiscount(), mrp);
        }

        if (product.getCategory() != null) {
            CategoryDiscount cd = categoryDiscountRepository.findByCategoryId(product.getCategory().getId());
            if (cd != null) {
                log.debug("Applying category discount for product {}", product.getId());
                return applyDiscount(cd.getDiscount(), mrp);
            }
        }

        return mrp;
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

    public double applyCartDiscount(Long cartId, double cartTotal) {

        log.info("Applying cart discount for cart {}", cartId);

        CartDiscount cd = cartDiscountRepository.findByCartId(cartId);

        if (cd == null) {
            log.debug("No cart discount for cart {}", cartId);
            return cartTotal;
        }

        Discount discount = cd.getDiscount();

        if (discount.getMinCartValue() != null &&
                cartTotal < discount.getMinCartValue()) {
            log.debug("Cart total {} less than min required {}", cartTotal, discount.getMinCartValue());
            return cartTotal;
        }

        return applyDiscount(discount, cartTotal);
    }

    public Discount getApplicableDiscount(Product product) {

        if (product == null) return null;

        List<ProductDiscount> pd = productDiscountRepository.findByProductId(product.getId());
        if (!pd.isEmpty()) {
            return pd.get(0).getDiscount();
        }

        if (product.getCategory() != null) {
            CategoryDiscount cd = categoryDiscountRepository.findByCategoryId(product.getCategory().getId());
            if (cd != null) {
                return cd.getDiscount();
            }
        }

        return null;
    }
}
