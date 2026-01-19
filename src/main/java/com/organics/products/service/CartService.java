package com.organics.products.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.AddToCartRequest;
import com.organics.products.dto.CartDTO;
import com.organics.products.dto.CartItemDTO;
import com.organics.products.entity.Cart;
import com.organics.products.entity.CartItems;
import com.organics.products.entity.Coupon;
import com.organics.products.entity.Inventory;
import com.organics.products.entity.Product;
import com.organics.products.entity.User;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.CartItemRepository;
import com.organics.products.respository.CartRepository;
import com.organics.products.respository.CouponRepository;
import com.organics.products.respository.InventoryRepository;
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

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private CouponRepository couponRepository;

	private CartDTO convertToCartDTO(Cart cart) {
		CartDTO dto = new CartDTO();

		dto.setId(cart.getId());
		dto.setActive(cart.isActive());
		dto.setPayableAmount(cart.getPayableAmount());
		dto.setCustomerId(cart.getUser().getId());

		double cartTotalMrp = 0.0;
		double cartTotalDiscount = 0.0;
		double cartPayableAmount = 0.0;

		List<CartItemDTO> itemDTOs = new ArrayList<>();

		for (CartItems item : cart.getItems()) {

			Inventory inventory = item.getInventory();
			Product product = inventory.getProduct();
			int qty = item.getQuantity();

			double mrp = product.getMRP() != null ? product.getMRP() : 0.0;

			double itemTotalMrp = mrp * qty;

			CartItemDTO itemDTO = new CartItemDTO();
			itemDTO.setId(item.getId());
			itemDTO.setProductId(product.getId());
			itemDTO.setInventoryId(inventory.getId());
			itemDTO.setProductName(product.getProductName());
			itemDTO.setQuantity(qty);
			itemDTO.setMrp(mrp);
			itemDTO.setUnit(product.getUnit());
			itemDTO.setNetWeight(product.getNetWeight());


			if (product.getImages() != null && !product.getImages().isEmpty()) {
				itemDTO.setImageUrl(s3Service.getFileUrl(product.getImages().get(0).getImageUrl()));
			}

			itemDTOs.add(itemDTO);

			cartTotalMrp += itemTotalMrp;
		}

		dto.setItems(itemDTOs);
		dto.setTotalMrp(cartTotalMrp);
		dto.setTotalDiscount(cartTotalDiscount);
		dto.setPayableAmount(cartPayableAmount);

		return dto;
	}



	private Cart getOrCreateActiveCart(User customer) {

		List<Cart> activeCarts = cartRepository.findByUserAndIsActive(customer, true);
		Cart cart;

		if (activeCarts.isEmpty()) {
			log.info("No active cart found for user {}. Creating a new one.", customer);
			cart = new Cart();
			cart.setUser(customer);
			cart.setTotalAmount(0.0);
			cart.setActive(true);
			return cartRepository.save(cart);

		} else {
			cart = activeCarts.get(0);

			if (activeCarts.size() > 1) {
				log.warn("Found {} active carts for user {}. Deactivating duplicates.", activeCarts.size(), customer);
				for (int i = 1; i < activeCarts.size(); i++) {
					Cart duplicateCart = activeCarts.get(i);
					duplicateCart.setActive(false);
					cartRepository.save(duplicateCart);
				}
			}
			return cart;
		}
	}



	public CartDTO addToCart(AddToCartRequest request) {

		Long customerId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new RuntimeException("User not authenticated"));

		User customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User not Found to add Items to cart: " + customerId));

		Cart cart = getOrCreateActiveCart(customer);

		Inventory inventory = inventoryRepository.findById(request.getInventoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + request.getInventoryId()));

		if (inventory.getAvailableStock() < request.getQuantity()) {
			throw new RuntimeException("Insufficient stock");
		}
		Optional<CartItems> existingItem = cart.getItems().stream()
				.filter(item -> item.getInventory().getId().equals(request.getInventoryId())).findFirst();

		if (existingItem.isPresent()) {

			CartItems item = existingItem.get();
			int newQty = item.getQuantity() + request.getQuantity();

			if (inventory.getAvailableStock() < newQty) {
				throw new RuntimeException("Insufficient stock for requested quantity");
			}

			item.setQuantity(newQty);
			cartItemRepository.save(item);

		} else {
			CartItems newItem = new CartItems();
			newItem.setCart(cart);
			newItem.setInventory(inventory);
			newItem.setQuantity(request.getQuantity());
			cart.getItems().add(newItem);
			cartItemRepository.save(newItem);

		}

		double totalMrp = 0.0;
		double totalPayable = 0.0;

		for (CartItems item : cart.getItems()) {
			Product product = item.getInventory().getProduct();
			int qty = item.getQuantity();

			double mrp = product.getMRP() != null ? product.getMRP() : 0.0;

			totalMrp += (mrp * qty);
		}

		cart.setTotalAmount(totalMrp);
		cart.setPayableAmount(totalPayable);

		Cart savedCart = cartRepository.save(cart);
		return convertToCartDTO(savedCart);
	}



	public CartDTO myCart() {

		Long customerId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new ResourceNotFoundException("No user Logged in to get Cart"));

		User user = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User not Found to get cart with Id: " + customerId));

		Cart cart = getOrCreateActiveCart(user);

		return convertToCartDTO(cart);
	}




	public CartDTO decreaseQuantity(Long inventoryId) {

		Long customerId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new ResourceNotFoundException("User Not authenticated to decrease cart: "));

		User user = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User Not Found to decrease cart"));

		Cart cart = cartRepository.findByUserAndIsActive(user, true).stream().findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

		CartItems existingItem = cart.getItems().stream()
				.filter(item -> item.getInventory().getId().equals(inventoryId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));

		if (existingItem.getQuantity() > 1) {
			existingItem.setQuantity(existingItem.getQuantity() - 1);
			cartItemRepository.save(existingItem);
		} else {
			cart.getItems().remove(existingItem);
			cartItemRepository.delete(existingItem);
		}

		Cart updatedCart = cartRepository.save(cart);
		return convertToCartDTO(updatedCart);
	}

}