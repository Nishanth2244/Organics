
package com.organics.products.controller;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.organics.products.dto.CategoryDTO;
import com.organics.products.dto.CategoryRevenueDTO;
import com.organics.products.entity.Category;
import com.organics.products.service.CategoryService;

import lombok.extern.slf4j.Slf4j;


@CrossOrigin(origins  = "*")
@Slf4j
@RestController
@RequestMapping("/api/category")
public class CategoryController {

	@Autowired
	private CategoryService categoryService;


	@PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CategoryDTO addCat(@RequestParam (value = "categoryName", required = true) String categoryName,
							  @RequestParam(value = "description", required = true) String description,
							  @RequestParam(value = "imageFile", required = true) MultipartFile imageFile) throws IOException {

		log.info("image file from fronened is: {}", imageFile);
		CategoryDTO category = categoryService.addCategory(categoryName, description, imageFile);

		log.info("Category added Succesfully {}", categoryName);
		return category;

	}


	@GetMapping("/Active")
	public ResponseEntity<Page<CategoryDTO>> getActive(@RequestParam(defaultValue = "0")int page,@RequestParam(defaultValue = "10")int size){

		log.info("Fetching Active categories");
		return ResponseEntity.ok(categoryService.getActive(page, size));
	}


	@PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Category updateCategory(@PathVariable Long id,
								   @RequestParam(value = "categoryName", required = false) String categoryName,
								   @RequestParam(value = "description", required = false) String description,
								   @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {

		Category category = categoryService.updateCategory(id,categoryName, description, imageFile);

		log.info("Category updated succesfully: {}", id);
		return category;

	}
	
	
	@PutMapping("/Status/{id}")
	public String inActiveCat(@PathVariable Long id,
							  @RequestParam Boolean status) {
		categoryService.inActive(id, status);

		log.info("category {} succesfully {}", status, id);
		return "Category "+status+ " Succesfully: "+ id;
	}



	@GetMapping("/inActive")
	public ResponseEntity<Page<CategoryDTO>> getInActive(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		log.info("Fetching inactive categories");
		return ResponseEntity.ok(categoryService.getInActive(page, size));
	}

	@GetMapping("/admin/revenue/category-monthly")
	public ResponseEntity<Page<CategoryRevenueDTO>> getCategoryRevenueByMonth(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam int month,
			@RequestParam(required = false) Integer year) {

		int targetYear = (year != null) ? year : LocalDate.now().getYear();

		return ResponseEntity.ok(
				categoryService.getCategoryRevenueByMonth(page, size, month, targetYear)
		);
	}


}
