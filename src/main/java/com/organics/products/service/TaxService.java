package com.organics.products.service;

import com.organics.products.entity.Category;
import com.organics.products.entity.CategoryTax;
import com.organics.products.respository.CategoryRepo;
import com.organics.products.respository.CategoryTaxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final CategoryTaxRepository categoryTaxRepository;
    private final CategoryRepo categoryRepo;

    @Transactional
    public CategoryTax saveOrUpdateTax(Long categoryId, double taxPercent) {

        if (taxPercent < 0 || taxPercent > 100) {
            throw new RuntimeException("Tax must be between 0 and 100");
        }

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));

        CategoryTax tax = categoryTaxRepository
                .findByCategory_Id(categoryId)
                .orElse(new CategoryTax());

        tax.setCategory(category);
        tax.setTaxPercent(taxPercent);

        return categoryTaxRepository.save(tax);
    }

    public double getTaxPercentByCategoryId(Long categoryId) {
        return categoryTaxRepository
                .findByCategory_Id(categoryId)
                .map(CategoryTax::getTaxPercent)
                .orElse(0.0);
    }
}
