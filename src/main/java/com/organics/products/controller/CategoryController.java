
package com.organics.products.controller;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
	public List<CategoryDTO> getActive(){

		List<CategoryDTO> categories = categoryService.getActive();

		log.info("Fetching Active categories");
		return categories;
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
	public List<CategoryDTO> getInActive(){

		List<CategoryDTO> categories = categoryService.getInActive();
		return categories;
	}

}
