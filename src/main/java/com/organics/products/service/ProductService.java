package com.organics.products.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.organics.products.dto.PagedResponse;
import com.organics.products.dto.ProductDTO;
import com.organics.products.entity.*;
import com.organics.products.exception.ProductNotFoundException;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CategoryRepo;
import com.organics.products.respository.InventoryRepository;
import com.organics.products.respository.ProductRepo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
public class ProductService {

    @Autowired private CategoryRepo categoryRepo;
    @Autowired private ProductRepo productRepo;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private DiscountService discountService;
    @Autowired private S3Service s3Service;

    /* =========================
       DTO CONVERTER
       ========================= */
    private ProductDTO convertToDTO(Product product) {

        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setProductName(product.getProductName());
        dto.setBrand(product.getBrand());
        dto.setDescription(product.getDescription());
        dto.setReturnDays(product.getReturnDays());
        dto.setMrp(product.getMRP());
        dto.setStatus(product.getStatus());
        dto.setUnit(product.getUnit());
        dto.setNetWeight(product.getNetWeight());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
        }

        if (product.getImages() != null) {
            dto.setImageUrls(
                    product.getImages().stream()
                            .map(img -> s3Service.getFileUrl(img.getImageUrl()))
                            .collect(Collectors.toList())
            );
        }

        inventoryRepository.findByProductId(product.getId())
                .stream()
                .findFirst()
                .ifPresent(inv -> {
                    dto.setInventoryId(inv.getId());
                    dto.setAvailableStock(inv.getAvailableStock());
                });

        Double finalPrice = discountService.calculateFinalPrice(product);
        dto.setFinalPrice(finalPrice);

        if (finalPrice < product.getMRP()) {
            dto.setDiscountAmount(product.getMRP() - finalPrice);
            Discount discount = discountService.getApplicableDiscount(product);
            if (discount != null) {
                dto.setDiscountType(discount.getDiscountType());
            }
        }

        return dto;
    }

    /* =========================
       ADD PRODUCT
       ========================= */
    public ProductDTO add(
            Long categoryId,
            MultipartFile[] images,
            String productName,
            String brand,
            String description,
            Integer returnDays,
            Double mrp,
            UnitType unit,
            Double netWeight) throws IOException {

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ProductNotFoundException("Category not found"));

        Product product = new Product();
        product.setProductName(productName);
        product.setBrand(brand);
        product.setDescription(description);
        product.setReturnDays(returnDays);
        product.setMRP(mrp);
        product.setUnit(unit);
        product.setNetWeight(netWeight);
        product.setStatus(true);
        product.setCategory(category);

        Product saved = productRepo.save(product);

        if (images != null) {
            List<ProductImage> imgs = new ArrayList<>();
            for (MultipartFile file : images) {
                ProductImage img = new ProductImage();
                img.setImageUrl(s3Service.uploadFile(file));
                img.setProduct(saved);
                imgs.add(img);
            }
            saved.setImages(imgs);
            productRepo.save(saved);
        }

        return convertToDTO(saved);
    }

    /* =========================
       STATUS UPDATE
       ========================= */
    public void inActive(Long id, boolean status) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        product.setStatus(status);
        productRepo.save(product);
    }

    /* =========================
       ACTIVE PRODUCTS (CUSTOM RESPONSE)
       ========================= */
    @Cacheable(value = "activeProducts:v2", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public PagedResponse<ProductDTO> activeProd(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> result = productRepo.findByStatusTrue(pageable);

        PagedResponse<ProductDTO> response = new PagedResponse<>();
        response.setContent(
                result.getContent().stream().map(this::convertToDTO).toList()
        );
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setLast(result.isLast());

        return response;
    }

    /* =========================
       INACTIVE PRODUCTS
       ========================= */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getInActive(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepo.findByStatusFalse(pageable)
                .map(this::convertToDTO);
    }

    /* =========================
       UPDATE PRODUCT
       ========================= */
    public ProductDTO updateProduct(
            Long id,
            MultipartFile[] images,
            String productName,
            String brand,
            String description,
            Integer returnDays,
            Double mrp,
            Long categoryId,
            UnitType unit,
            Double netWeight) throws IOException {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (productName != null) product.setProductName(productName);
        if (brand != null) product.setBrand(brand);
        if (description != null) product.setDescription(description);
        if (returnDays != null) product.setReturnDays(returnDays);
        if (mrp != null) product.setMRP(mrp);
        if (unit != null) product.setUnit(unit);
        if (netWeight != null) product.setNetWeight(netWeight);

        if (categoryId != null) {
            Category category = categoryRepo.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        return convertToDTO(productRepo.save(product));
    }

    /* =========================
       BY CATEGORY
       ========================= */
    @Transactional(readOnly = true)
    public Page<ProductDTO> byCategory(Long categoryId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepo.findByCategoryId(categoryId, pageable)
                .map(this::convertToDTO);
    }

    /* =========================
       SEARCH
       ========================= */
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchByName(String name, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepo
                .findByProductNameContainingIgnoreCaseAndStatusTrue(name, pageable)
                .map(this::convertToDTO);
    }
}
