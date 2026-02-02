package com.organics.products.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.CategoryDTO;
import com.organics.products.dto.CategoryRevenueDTO;
import com.organics.products.entity.Category;
import com.organics.products.entity.Product;
import com.organics.products.exception.AlreadyExistsException;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CategoryRepo;
import com.organics.products.respository.OrderRepository;
import com.organics.products.respository.ProductRepo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryService {

	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private ProductRepo productRepo;
	
	@Autowired
	private OrderRepository orderRepository;
	
	
	private CategoryDTO convertToDTO(Category category) {

		CategoryDTO dto = new CategoryDTO();
		dto.setId(category.getId());
		dto.setCategoryName(category.getCategoryName());
		dto.setDescription(category.getDescription());
		dto.setStatus(category.getStatus());

		if (category.getCategoryImage() != null) {
			dto.setCategoryImage(s3Service.getFileUrl(category.getCategoryImage()));
		}

		return dto;
	}

    @CacheEvict(
            value = { "activeCategories", "inactiveCategories", "categoryRevenue"}, allEntries = true)
	public CategoryDTO addCategory(String categoryName,
								   String description,
								   MultipartFile imageFile) throws IOException {

		log.info("Adding new category: name={}", categoryName);

		if (categoryName == null || categoryName.trim().isEmpty()) {
			log.warn("Category name is empty");
			throw new IllegalArgumentException("Category name is required");
		}

		if (categoryRepo.existsByCategoryName(categoryName)) {
			log.warn("Category already exists with name={}", categoryName);
			throw new AlreadyExistsException("Category already exist with Name: " + categoryName);
		}

		Category category = new Category();
		category.setCategoryName(categoryName);
		category.setDescription(description);
		category.setStatus(true);

		if (imageFile != null && !imageFile.isEmpty()) {
			log.info("Uploading category image for {}", categoryName);
			String url = s3Service.uploadFile(imageFile);
			category.setCategoryImage(url);
		} else {
			log.warn("No image provided for category {}", categoryName);
		}

		Category saved = categoryRepo.save(category);

		log.info("Category created successfully. categoryId={}", saved.getId());

		return convertToDTO(saved);
	}
    @Cacheable(
            value = "activeCategories",
            key = "#page + '-' + #size",
            unless = "#result == null || #result.isEmpty()")
	@Transactional(readOnly = true)
	public Page<CategoryDTO> getActive(int page, int size) {

		log.info("Fetching active categories: page={}, size={}", page, size);

		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

		Page<Category> categoriesPage = categoryRepo.findByStatusTrue(pageable);

		if (categoriesPage.isEmpty()) {
			log.info("No active categories found");
			return Page.empty(pageable);
		}

		log.info("Found {} active categories", categoriesPage.getTotalElements());

		return categoriesPage.map(this::convertToDTO);
	}

    @CacheEvict(
            value = { "activeCategories", "inactiveCategories", "categoryRevenue"}, allEntries = true)
	public Category updateCategory(Long id,
								   String categoryName,
								   String description,
								   MultipartFile imageFile) throws IOException {

		log.info("Updating category. categoryId={}", id);

		Category category = categoryRepo.findById(id)
				.orElseThrow(() -> {
					log.warn("Category not found for id={}", id);
					return new ResourceNotFoundException("Category Not found to update: " + id);
				});

		if (categoryName != null) {
			category.setCategoryName(categoryName);
			log.info("Updated category name for id={}", id);
		}

		if (description != null) {
			category.setDescription(description);
			log.info("Updated description for id={}", id);
		}

		if (imageFile != null && !imageFile.isEmpty()) {
			log.info("Replacing image for categoryId={}", id);

			if (category.getCategoryImage() != null) {
				s3Service.deleteFile(category.getCategoryImage());
			}

			String url = s3Service.uploadFile(imageFile);
			category.setCategoryImage(url);
		}

		Category updated = categoryRepo.save(category);

		log.info("Category updated successfully. categoryId={}", id);

		return updated;
	}

    @CacheEvict(
            value = { "activeCategories", "inactiveCategories", "categoryRevenue"}, allEntries = true)
	public void inActive(Long id, Boolean status) {

		log.info("Updating category status. categoryId={}, status={}", id, status);

		Category category = categoryRepo.findById(id)
				.orElseThrow(() -> {
					log.warn("Category not found for id={}", id);
					return new ResourceNotFoundException("category Not Found to Inactive: " + id);
				});

		category.setStatus(status);

		List<Product> products = productRepo.findByCategoryId(id);

		if (products == null || products.isEmpty()) {
			log.info("No products found for categoryId={}", id);
		} else {
			log.info("Updating {} products status for categoryId={}", products.size(), id);
			for (Product product : products) {
				product.setStatus(status);
			}
		}

		categoryRepo.save(category);

		log.info("Category status updated successfully. categoryId={}, status={}", id, status);
	}

    @Cacheable(
            value = "inactiveCategories", key = "#page + '-' + #size",
            unless = "#result == null || #result.isEmpty()")
	@Transactional(readOnly = true)
	public Page<CategoryDTO> getInActive(int page, int size) {

		log.info("Fetching inactive categories: page={}, size={}", page, size);

		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

		Page<Category> categoriesPage = categoryRepo.findByStatusFalse(pageable);

		if (categoriesPage.isEmpty()) {
			log.info("No inactive categories found");
			return Page.empty(pageable);
		}

		log.info("Found {} inactive categories", categoriesPage.getTotalElements());

		return categoriesPage.map(this::convertToDTO);
	}


    @Cacheable(
            value = "categoryRevenue",
            key = "#month + '-' + #year + '-' + #page + '-' + #size")
	@Transactional(readOnly = true)
	public Page<CategoryRevenueDTO> getCategoryRevenueByMonth(
			int page, int size, int month, int year) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("totalRevenue").descending());

		return orderRepository.getCategoryRevenueByMonth(month, year, pageable);
	}


}
