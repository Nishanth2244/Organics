package com.organics.products.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organics.products.dto.AddToCartRequest;
import com.organics.products.dto.CartDTO;
import com.organics.products.dto.CartItemDTO;
import com.organics.products.entity.Cart;
import com.organics.products.entity.CartItems;
import com.organics.products.entity.Product;
import com.organics.products.entity.User;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CartItemRepository;
import com.organics.products.respository.CartRepository;
import com.organics.products.respository.ProductRepo;
import com.organics.products.respository.UserRepository;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class CartService {
	
	@Autowired
	private UserRepository customerRepository;
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private ProductRepo productRepo;
	
	@Autowired
	private CartItemRepository cartItemRepository;
	
	
	@Autowired
	private S3Service s3Service;
	
	
	private CartDTO convertToCartDTO(Cart cart) {
	    CartDTO dto = new CartDTO();
	    dto.setId(cart.getId());
	    dto.setActive(true);
	    dto.setTotalAmount(cart.getTotalAmount());
	    dto.setCustomerId(cart.getUser().getId());
	    
	    List<CartItemDTO> itemDTOs = cart.getItems().stream().map(item -> {
	        CartItemDTO itemDTO = new CartItemDTO();
	        itemDTO.setId(item.getId());
	        itemDTO.setProductId(item.getProduct().getId());
	        itemDTO.setProductName(item.getProduct().getProductName());
	        itemDTO.setQuantity(item.getQuantity());
	        itemDTO.setPrice(item.getProduct().getMRP());
	        if (!item.getProduct().getImages().isEmpty()) {
	            itemDTO.setImageUrl(s3Service.getFileUrl(item.getProduct().getImages().get(0).getImageUrl()));
	        }
	        return itemDTO;
	    }).collect(Collectors.toList());
	    
	    dto.setItems(itemDTOs);
	    return dto;
	}
	
	
	
	private Cart getOrCreateActiveCart(User customer) {
		
		List<Cart> activeCarts = cartRepository.findByUserAndIsActive(customer, true);
		Cart cart;

		if (activeCarts.isEmpty()) {
			log.info("No active cart found for user {}. Creating a new one.", customer);
			cart = new Cart();
			cart.setUser(customer);			cart.setTotalAmount(0.0);
			cart.setActive(true);
			return cartRepository.save(cart); 

		} else {
			cart = activeCarts.get(0);

			if (activeCarts.size() > 1) {
				log.warn("Found {} active carts for user {}. Deactivating duplicates.", activeCarts.size(),
						customer);
				for (int i = 1; i < activeCarts.size(); i++) {
					Cart duplicateCart = activeCarts.get(i);
					duplicateCart.setActive(false);
					cartRepository.save(duplicateCart);
				}
			}
			return cart;
		}
	}


	public CartDTO addToCart(AddToCartRequest addToCartRequest, Long customerId) {
		
		User customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User not Found to add Items to cart: "+ customerId));
		
		Cart cart = getOrCreateActiveCart(customer);
		
		Product product = productRepo.findById(addToCartRequest.getProductId())
				.orElseThrow(() -> new ResourceNotFoundException("Product Not found to add to Cart: "+ addToCartRequest.getProductId()));
		
		Optional<CartItems> existingItem = cart.getItems().stream()
				.filter(item -> item.getProduct().getId().equals(addToCartRequest.getProductId())).findFirst();
		
		if(existingItem.isPresent()) {
			CartItems item = existingItem.get();
			item.setQuantity(item.getQuantity() + addToCartRequest.getQuantity());
			cartItemRepository.save(item);		
			
		}else {
			CartItems newItem = new CartItems();
			newItem.setCart(cart);
			newItem.setProduct(product);
			newItem.setQuantity(addToCartRequest.getQuantity());
			cart.getItems().add(newItem);
			cartItemRepository.save(newItem);
			;
		}
		
		double total = cart.getItems().stream().mapToDouble(item -> item.getProduct().getMRP() * item.getQuantity())
				.sum();

		cart.setTotalAmount(total);
		
		Cart savedCart = cartRepository.save(cart);
	    return convertToCartDTO(savedCart);		
	}

	
}
