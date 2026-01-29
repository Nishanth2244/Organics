package com.organics.products.controller;

import com.organics.products.dto.TaxRequestDTO;
import com.organics.products.entity.CategoryTax;
import com.organics.products.service.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    //  Save / Update Tax
    @PostMapping
    public ResponseEntity<CategoryTax> saveTax(
            @RequestBody TaxRequestDTO dto) {

        return ResponseEntity.ok(
                taxService.saveOrUpdateTax(
                        dto.getCategoryId(),
                        dto.getTaxPercent()
                )
        );
    }

    //  Get tax by categoryId
    @GetMapping("/{categoryId}")
    public ResponseEntity<Double> getTax(@PathVariable Long categoryId) {
        return ResponseEntity.ok(
                taxService.getTaxPercentByCategoryId(categoryId)
        );
    }
}
