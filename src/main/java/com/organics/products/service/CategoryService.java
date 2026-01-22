package com.organics.products.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.organics.products.dto.CategoryDTO;
import com.organics.products.entity.Category;
import com.organics.products.entity.Product;
import com.organics.products.exception.AlreadyExistsException;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CategoryRepo;
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


	public List<CategoryDTO> getActive() {

		log.info("Fetching active categories");

		List<Category> categories = categoryRepo.findByStatusTrue();

		if (categories == null || categories.isEmpty()) {
			log.info("No active categories found");
			return List.of();
		}

		log.info("Found {} active categories", categories.size());

		return categories.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}


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


	public List<CategoryDTO> getInActive() {

		log.info("Fetching inactive categories");

		List<Category> categories = categoryRepo.findByStatusFalse();

		if (categories == null || categories.isEmpty()) {
			log.info("No inactive categories found");
			return List.of();
		}

		log.info("Found {} inactive categories", categories.size());

		return categories.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}
}
