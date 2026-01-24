package com.organics.products.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.organics.products.entity.UnitType;
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
			@RequestParam(value = "returnDays", required = false) Integer returnDays,
			@RequestParam(value = "unit", required = false) UnitType unitType,
			@RequestParam(value = "netWeight", required = false) Double netWeight,
			@RequestParam(value = "mrp", required = false) Double mrp) throws IOException {

		return productService.add(categoryId, images, productName, brand, description, returnDays, mrp, unitType, netWeight);
	}



	@PutMapping("/status/{id}")
	public String inActiveProd(@PathVariable Long id, @RequestParam Boolean status) {
		productService.inActive(id, status);

		log.info("product {} succesfully {}", status, id);
		return "product " + status + " Succesfully: " + id;
	}



	@GetMapping("/activeProd")
	public Page<ProductDTO> getActive( @RequestParam(defaultValue = "0") int page,
									   @RequestParam(defaultValue = "10") int size) {

		Page<ProductDTO> products = productService.activeProd(page,size);

		log.info("Fetching active products");
		return products;
	}



	@GetMapping("/inActive")
	public Page<ProductDTO> getInActive(@RequestParam(defaultValue = "0") int page,
										@RequestParam(defaultValue = "10") int size) {

		Page<ProductDTO> products = productService.getInActive(page,size);
		return products;
	}



	@PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProductDTO updateProduct(@PathVariable Long id,
	        @RequestPart(value = "images", required = false) MultipartFile[] images,
	        @RequestParam(value = "productName", required = false) String productName,
	        @RequestParam(value = "brand", required = false) String brand,
	        @RequestParam(value = "description", required = false) String description,
	        @RequestParam(value = "returnDays", required = false) Integer returnDays,
	        @RequestParam(value = "mrp", required = false) Double mrp,
	        @RequestParam(value = "unit", required = false) UnitType unitType,
			@RequestParam(value = "netWeight", required = false) Double netWeight,
	        @RequestParam(value = "categoryId", required = false) Long categoryId) throws IOException {

	    log.info("Updating product with ID: {}", id);
	    return productService.updateProduct(id, images, productName, brand, description, returnDays, mrp, categoryId, unitType, netWeight);
	}



	@GetMapping("/byCategory")
	public ResponseEntity<Page<ProductDTO>> byCategory(
			@RequestParam Long categoryId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		Page<ProductDTO> products = productService.byCategory(categoryId, page, size);
		return ResponseEntity.ok(products);
	}

	@GetMapping("/search")
	public Page<ProductDTO> search(@RequestParam String name,
								   @RequestParam(defaultValue = "0") int page,
								   @RequestParam(defaultValue = "10") int size) {

		log.info("Searching by name {}", name);

		return productService.searchByName(name, page, size);
	}

}
