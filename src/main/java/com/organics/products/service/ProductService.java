package com.organics.products.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.organics.products.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.organics.products.dto.ProductDTO;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CategoryRepo;
import com.organics.products.respository.InventoryRepository;
import com.organics.products.respository.ProductRepo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class ProductService {

	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private ProductRepo productRepo;

	@Autowired
	private DiscountService discountService;




	@Autowired
	private InventoryRepository inventoryRepository;


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
			List<String> urls = product.getImages()
					.stream()
					.map(img -> s3Service.getFileUrl(img.getImageUrl()))
					.collect(Collectors.toList());
			dto.setImageUrls(urls);
		}

		List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());
		if (!inventories.isEmpty()) {
			dto.setInventoryId(inventories.get(0).getId());
			dto.setAvailableStock(inventories.get(0).getAvailableStock());
		}

		Double finalPrice = discountService.calculateFinalPrice(product);
		dto.setFinalPrice(finalPrice);

		if (finalPrice < product.getMRP()) {
			dto.setDiscountAmount(product.getMRP() - finalPrice);

			Discount appliedDiscount = discountService.getApplicableDiscount(product);
			if (appliedDiscount != null) {
				dto.setDiscountType(appliedDiscount.getDiscountType());
			}
		} else {
			dto.setDiscountAmount(null);
			dto.setDiscountType(null);
		}

		return dto;
	}





	public ProductDTO add(Long categoryId, MultipartFile[] images, String productName, String brand, String description,Integer returnDays, Double mrp, UnitType unitType, Double netWeight) throws IOException {

		Category category = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));

		Product product = new Product();
		product.setProductName(productName);
		product.setBrand(brand);
		product.setDescription(description);
		product.setReturnDays(returnDays);
		product.setMRP(mrp);
		product.setCategory(category);
		product.setStatus(true);
		product.setUnit(unitType);
		product.setNetWeight(netWeight);


		Product savedProduct = productRepo.save(product);

		List<ProductImage> imageList = new ArrayList<>();

		for (MultipartFile file : images) {
			String url = s3Service.uploadFile(file);

			ProductImage img = new ProductImage();
			img.setImageUrl(url);
			img.setProduct(savedProduct);

			imageList.add(img);
		}

		savedProduct.setImages(imageList);
		Product finalProduct = productRepo.save(savedProduct);

		return convertToDTO(finalProduct);
	}



	public void inActive(Long id, Boolean status) {

		Product product = productRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found to inActive: " + id));

		product.setStatus(status);
		productRepo.save(product);
	}




	public List<ProductDTO> activeProd() {

		List<Product> products = productRepo.findByStatusTrue();

		return products.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}



	public List<ProductDTO> getInActive() {

		List<Product> products = productRepo.findByStatusFalse();

		return products.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}




	public Product updateProduct(Long id, MultipartFile[] images, String productName, String brand, String description,
								 Integer returnDays, Double mrp, Long categoryId, UnitType unitType, Double netWeight) throws IOException {

		Product product = productRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

		if (productName != null)
			product.setProductName(productName);
		if (brand != null)
			product.setBrand(brand);
		if (description != null)
			product.setDescription(description);
		if (returnDays != null)
			product.setReturnDays(returnDays);
		if (mrp != null)
			product.setMRP(mrp);

		if(unitType != null) {
			product.setUnit(unitType);
		}

		if(netWeight != null) {
			product.setNetWeight(netWeight);
		}


		if (categoryId != null) {
			Category category = categoryRepo.findById(categoryId)
					.orElseThrow(() -> new ResourceNotFoundException("Category not found"));
			product.setCategory(category);
		}

		if (images != null && images.length > 0) {
			if (product.getImages() != null) {
				for (ProductImage oldImg : product.getImages()) {
					s3Service.deleteFile(oldImg.getImageUrl());
				}

				product.getImages().clear();
			} else {
				product.setImages(new ArrayList<>());
			}

			for (MultipartFile file : images) {
				String url = s3Service.uploadFile(file);
				ProductImage img = new ProductImage();
				img.setImageUrl(url);
				img.setProduct(product);

				product.getImages().add(img);
			}
		}

		return productRepo.save(product);
	}



	public List<ProductDTO> byCategory(Long categoryId) {

		Category category = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not Found"));

		List<Product> products = productRepo.findByCategoryId(category.getId());

		return products.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

}