
package com.organics.products.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

import lombok.extern.java.Log;
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

	public CategoryDTO addCategory(String categoryName, String description, MultipartFile imageFile) throws IOException {

		if (categoryRepo.existsByCategoryName(categoryName)) {
			throw new AlreadyExistsException("Category already exist with Name: " + categoryName);
		}

		Category category = new Category();
		category.setCategoryName(categoryName);
		category.setDescription(description);
		category.setStatus(true);

		String url = s3Service.uploadFile(imageFile);
		category.setCategoryImage(url);

		return convertToDTO(categoryRepo.save(category));
	}



	public List<CategoryDTO> getActive() {
		return categoryRepo.findByStatusTrue().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}



	public Category updateCategory(Long id, String categoryName, String description, MultipartFile imageFile)
			throws IOException {

		Category category = categoryRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category Not found to update: " + id));

		if (categoryName != null) {
			category.setCategoryName(categoryName);
		}

		if (description != null) {
			category.setDescription(description);
		}

		if (imageFile != null) {
			s3Service.deleteFile(category.getCategoryImage());
			String url = s3Service.uploadFile(imageFile);
			category.setCategoryImage(url);
		}

		return categoryRepo.save(category);
	}

	public void inActive(Long id, Boolean status) {

		Category category = categoryRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("category Not Found to Inactive: "+ id));

		category.setStatus(status);
		
		List<Product> products = productRepo.findByCategoryId(id);
		
		for (Product product : products) {
		    product.setStatus(status);
		}
		
		categoryRepo.save(category);
	}



	public List<CategoryDTO> getInActive() {

		return categoryRepo.findByStatusFalse().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}
	
	
	public List<CategoryRevenueDTO> getCategoryRevenueByMonth(int month, int year) {
//	    if (!SecurityUtil.isAdmin()) {
//	        throw new RuntimeException("Unauthorized: Admin access required");
//	    }
	    return orderRepository.getCategoryRevenueByMonth(month, year);
	}
}
