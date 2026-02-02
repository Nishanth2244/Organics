package com.organics.products.controller;

import java.io.IOException;

import com.organics.products.dto.PagedResponse;
import com.organics.products.dto.ProductDTO;
import com.organics.products.entity.UnitType;
import com.organics.products.service.ProductService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /* =========================
       ADD PRODUCT
       ========================= */
    @PostMapping(value = "/add/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> addProduct(
            @PathVariable Long categoryId,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer returnDays,
            @RequestParam(required = false) UnitType unit,
            @RequestParam(required = false) Double netWeight,
            @RequestParam(required = false) Double mrp) throws IOException {

        return ResponseEntity.ok(
                productService.add(
                        categoryId,
                        images,
                        productName,
                        brand,
                        description,
                        returnDays,
                        mrp,
                        unit,
                        netWeight
                )
        );
    }

    /* =========================
       ACTIVATE / DEACTIVATE
       ========================= */
    @PutMapping("/status/{id}")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id,
            @RequestParam boolean status) {

        productService.inActive(id, status);
        return ResponseEntity.ok("Product status updated successfully");
    }

    /* =========================
       ACTIVE PRODUCTS (CUSTOM PAGED RESPONSE)
       ========================= */
    @GetMapping("/active")
    public ResponseEntity<PagedResponse<ProductDTO>> getActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                productService.activeProd(page, size)
        );
    }

    /* =========================
       INACTIVE PRODUCTS
       ========================= */
    @GetMapping("/inactive")
    public ResponseEntity<Page<ProductDTO>> getInactive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                productService.getInActive(page, size)
        );
    }

    /* =========================
       UPDATE PRODUCT
       ========================= */
    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer returnDays,
            @RequestParam(required = false) Double mrp,
            @RequestParam(required = false) UnitType unit,
            @RequestParam(required = false) Double netWeight,
            @RequestParam(required = false) Long categoryId) throws IOException {

        return ResponseEntity.ok(
                productService.updateProduct(
                        id,
                        images,
                        productName,
                        brand,
                        description,
                        returnDays,
                        mrp,
                        categoryId,
                        unit,
                        netWeight
                )
        );
    }

    /* =========================
       PRODUCTS BY CATEGORY
       ========================= */
    @GetMapping("/by-category")
    public ResponseEntity<Page<ProductDTO>> byCategory(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                productService.byCategory(categoryId, page, size)
        );
    }

    /* =========================
       SEARCH
       SEARCH
       ========================= */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                productService.searchByName(name, page, size)
        );
    }
}
