package com.organics.products.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.organics.products.dto.ProductDTO;
import com.organics.products.entity.Product;
import com.organics.products.service.ProductService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/api/products")
public class ProductController {

	@Autowired
	private ProductService productService;

	@PostMapping(value = "/add/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProductDTO addProduct(@PathVariable Long categoryId,

			@RequestPart("images") MultipartFile[] images,

			@RequestParam(value = "productName", required = false) String productName,
			@RequestParam(value = "brand", required = false) String brand,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "discount", required = false) Double discount,
			@RequestParam(value = "returnDays", required = false) Integer returnDays,
			@RequestParam(value = "mrp", required = false) Double mrp) throws IOException {

		return productService.add(categoryId, images, productName, brand, description, discount, returnDays, mrp);
	}
	
	

	@PutMapping("/status/{id}")
	public String inActiveProd(@PathVariable Long id, @RequestParam Boolean status) {
		productService.inActive(id, status);

		log.info("product {} succesfully {}", status, id);
		return "product " + status + " Succesfully: " + id;
	}
	
	

	@GetMapping("/activeProd")
	public List<ProductDTO> getActive() {

		List<ProductDTO> products = productService.activeProd();
		return products;
	}
	
	
	
	@GetMapping("/inActive")
	public List<ProductDTO> getInActive(){
		
		List<ProductDTO> products = productService.getInActive();
		return products;
	}
	
	
	
	@PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Product updateProduct(@PathVariable Long id,
	        @RequestPart(value = "images", required = false) MultipartFile[] images,
	        @RequestParam(value = "productName", required = false) String productName,
	        @RequestParam(value = "brand", required = false) String brand,
	        @RequestParam(value = "description", required = false) String description,
	        @RequestParam(value = "discount", required = false) Double discount,
	        @RequestParam(value = "returnDays", required = false) Integer returnDays,
	        @RequestParam(value = "mrp", required = false) Double mrp,
	        @RequestParam(value = "categoryId", required = false) Long categoryId) throws IOException {

	    log.info("Updating product with ID: {}", id);
	    return productService.updateProduct(id, images, productName, brand, description, discount, returnDays, mrp, categoryId);
	}
	
	
	
	@GetMapping("/byCategory")
	public List<ProductDTO> byCategory(@RequestParam Long categoryId){
		
		List<ProductDTO> products = productService.byCategory(categoryId);
		return products;
	}

}
