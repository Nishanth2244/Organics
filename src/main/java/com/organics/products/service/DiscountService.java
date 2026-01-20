package com.organics.products.service;

import com.organics.products.dto.DiscountRequestDTO;
import com.organics.products.entity.*;
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

    // ================= CREATE DISCOUNT =================

    public Discount createDiscount(DiscountRequestDTO dto) {
        Discount discount = new Discount();
        discount.setName(dto.getName());
        discount.setDiscountType(dto.getDiscountType());
        discount.setDiscountValue(dto.getDiscountValue());
        discount.setScope(dto.getScope());
        discount.setActive(dto.getActive());
        discount.setValidFrom(dto.getValidFrom());
        discount.setValidTo(dto.getValidTo());
        discount.setMinCartValue(dto.getMinCartValue());

        log.info("Creating discount: {}", dto.getName());
        return discountRepository.save(discount);
    }

    // ================= ASSIGN DISCOUNTS =================

    public void assignToProduct(Long productId, Long discountId) {

        if (productDiscountRepository.existsByProductId(productId))
            throw new RuntimeException("Product already has discount");

        Product product = productRepo.findById(productId).orElseThrow();
        Discount discount = discountRepository.findById(discountId).orElseThrow();

        if (discount.getScope() != DiscountScope.PRODUCT)
            throw new RuntimeException("Not a product discount");

        ProductDiscount pd = new ProductDiscount();
        pd.setProduct(product);
        pd.setDiscount(discount);

        productDiscountRepository.save(pd);
    }

    public void assignToCategory(Long categoryId, Long discountId) {

        if (categoryDiscountRepository.existsByCategoryId(categoryId))
            throw new RuntimeException("Category already has discount");

        Category category = categoryRepo.findById(categoryId).orElseThrow();
        Discount discount = discountRepository.findById(discountId).orElseThrow();

        if (discount.getScope() != DiscountScope.CATEGORY)
            throw new RuntimeException("Not category discount");

        CategoryDiscount cd = new CategoryDiscount();
        cd.setCategory(category);
        cd.setDiscount(discount);

        categoryDiscountRepository.save(cd);
    }

    public void assignToCart(Long cartId, Long discountId) {

        Discount discount = discountRepository.findById(discountId).orElseThrow();

        if (discount.getScope() != DiscountScope.CART)
            throw new RuntimeException("Not cart discount");

        CartDiscount cd = new CartDiscount();
        cd.setCartId(cartId);
        cd.setDiscount(discount);

        cartDiscountRepository.save(cd);
    }

    // ================= PRODUCT PRICE CALCULATION =================

    public double calculateFinalPrice(Product product) {

        double mrp = product.getMRP();

        // 1️⃣ Product Discount (Highest Priority)
        List<ProductDiscount> pd = productDiscountRepository.findByProductId(product.getId());
        if (!pd.isEmpty()) {
            return applyDiscount(pd.get(0).getDiscount(), mrp);
        }

        // 2️⃣ Category Discount
        if (product.getCategory() != null) {
            CategoryDiscount cd = categoryDiscountRepository.findByCategoryId(product.getCategory().getId());
            if (cd != null) {
                return applyDiscount(cd.getDiscount(), mrp);
            }
        }

        return mrp;
    }


    private double applyDiscount(Discount discount, double price) {

        if (!Boolean.TRUE.equals(discount.getActive())) return price;

        LocalDateTime now = LocalDateTime.now();
        if (discount.getValidFrom() != null && now.isBefore(discount.getValidFrom())) return price;
        if (discount.getValidTo() != null && now.isAfter(discount.getValidTo())) return price;

        double finalPrice = price;

        if (discount.getDiscountType() == DiscountType.PERCENT) {
            finalPrice -= (price * discount.getDiscountValue() / 100);
        } else {
            finalPrice -= discount.getDiscountValue();
        }

        return Math.max(finalPrice, 0);
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


    public Discount getApplicableDiscount(Product product) {

        // Product Discount first
        List<ProductDiscount> pd = productDiscountRepository.findByProductId(product.getId());
        if (!pd.isEmpty()) {
            return pd.get(0).getDiscount();
        }

        // Then Category Discount
        if (product.getCategory() != null) {
            CategoryDiscount cd = categoryDiscountRepository.findByCategoryId(product.getCategory().getId());
            if (cd != null) {
                return cd.getDiscount();
            }
        }

        return null;
    }


}
