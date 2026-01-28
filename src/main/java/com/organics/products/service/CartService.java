package com.organics.products.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.organics.products.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.AddToCartRequest;
import com.organics.products.dto.CartDTO;
import com.organics.products.dto.CartItemDTO;
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

	@Autowired
	private DiscountService discountService;


	private CartDTO convertToCartDTO(Cart cart) {

		CartDTO dto = new CartDTO();

		dto.setId(cart.getId());
		dto.setActive(cart.isActive());
		dto.setCustomerId(cart.getUser().getId());

		dto.setTotalMrp(cart.getTotalAmount() != null ? cart.getTotalAmount() : 0.0);
		dto.setTotalDiscount(cart.getDiscountAmount() != null ? cart.getDiscountAmount() : 0.0);
		dto.setPayableAmount(cart.getPayableAmount() != null ? cart.getPayableAmount() : 0.0);

		List<CartItemDTO> itemDTOs = new ArrayList<>();

		if (cart.getItems() != null && !cart.getItems().isEmpty()) {
			for (CartItems item : cart.getItems()) {

				Inventory inventory = item.getInventory();
				Product product = inventory.getProduct();
				int qty = item.getQuantity();

				double mrp = product.getMRP() != null ? product.getMRP() : 0.0;

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

				Double finalPrice = discountService.calculateFinalPrice(product);
				itemDTO.setFinalPrice(finalPrice);

				if (finalPrice < mrp) {
					itemDTO.setDiscountAmount(mrp - finalPrice);

					Discount discount = discountService.getApplicableDiscount(product);
					if (discount != null) {
						itemDTO.setDiscountType(discount.getDiscountType());
					}
				} else {
					itemDTO.setDiscountAmount(null);
					itemDTO.setDiscountType(null);
				}

				itemDTOs.add(itemDTO);
			}
		} else {
			log.info("Cart {} has no items", cart.getId());
		}

		dto.setItems(itemDTOs);
		return dto;
	}

	private Cart getOrCreateActiveCart(User customer) {

		List<Cart> activeCarts = cartRepository.findByUserAndIsActive(customer, true);
		Cart cart;

		if (activeCarts == null || activeCarts.isEmpty()) {
			log.info("No active cart found for user {}. Creating new cart.", customer.getId());

			cart = new Cart();
			cart.setUser(customer);
			cart.setTotalAmount(0.0);
			cart.setDiscountAmount(0.0);
			cart.setPayableAmount(0.0);
			cart.setActive(true);

			return cartRepository.save(cart);
		} else {
			cart = activeCarts.get(0);

			if (activeCarts.size() > 1) {
				log.warn("Found {} active carts for user {}. Deactivating duplicates.",
						activeCarts.size(), customer.getId());

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

		log.info("Adding item to cart: inventoryId={}, qty={}",
				request.getInventoryId(), request.getQuantity());

		Long customerId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new RuntimeException("User not authenticated"));

		User customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User not Found: " + customerId));

		Cart cart = getOrCreateActiveCart(customer);

		Inventory inventory = inventoryRepository.findById(request.getInventoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + request.getInventoryId()));

		if (inventory.getAvailableStock() < request.getQuantity()) {
			log.warn("Insufficient stock for inventoryId={}", request.getInventoryId());
			throw new RuntimeException("Insufficient stock");
		}

		Product product = inventory.getProduct();
		if (product == null) {
			throw new RuntimeException("Product not found for inventory ID: " + request.getInventoryId());
		}

		Optional<CartItems> existingItem = cart.getItems().stream()
				.filter(item -> item.getInventory().getId().equals(request.getInventoryId()))
				.findFirst();

		if (existingItem.isPresent()) {

			CartItems item = existingItem.get();
			int newQty = item.getQuantity() + request.getQuantity();

			if (inventory.getAvailableStock() < newQty) {
				throw new RuntimeException("Insufficient stock for requested quantity");
			}

			item.setQuantity(newQty);
			cartItemRepository.save(item);

			log.info("Updated quantity in cart. inventoryId={}, newQty={}",
					request.getInventoryId(), newQty);

		} else {

			CartItems newItem = new CartItems();
			newItem.setCart(cart);
			newItem.setInventory(inventory);
			newItem.setQuantity(request.getQuantity());
			cart.getItems().add(newItem);
			cartItemRepository.save(newItem);

			log.info("Added new item to cart. inventoryId={}, qty={}",
					request.getInventoryId(), request.getQuantity());
		}

		// Recalculate totals
		double totalMrp = 0.0;
		double totalPayable = 0.0;
		double totalDiscount = 0.0;

		for (CartItems item : cart.getItems()) {

			Product cartItemProduct = item.getInventory().getProduct();
			int qty = item.getQuantity();

			double mrp = cartItemProduct.getMRP() != null ? cartItemProduct.getMRP() : 0.0;
			double finalPrice = discountService.calculateFinalPrice(cartItemProduct);

			totalMrp += (mrp * qty);
			totalPayable += (finalPrice * qty);
			totalDiscount += ((mrp - finalPrice) * qty);
		}

		cart.setTotalAmount(totalMrp);
		cart.setPayableAmount(totalPayable);
		cart.setDiscountAmount(totalDiscount);

		Cart savedCart = cartRepository.save(cart);

		log.info("Cart updated successfully. cartId={}", savedCart.getId());

		return convertToCartDTO(savedCart);
	}


	public CartDTO myCart() {

		Long customerId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new ResourceNotFoundException("No user logged in"));

		User user = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User not Found: " + customerId));

		Cart cart = getOrCreateActiveCart(user);

		log.info("Fetched cart for user {}. cartId={}", customerId, cart.getId());

		return convertToCartDTO(cart);
	}


	public CartDTO decreaseQuantity(Long inventoryId) {

		log.info("Decreasing quantity. inventoryId={}", inventoryId);

		Long customerId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));

		User user = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("User not Found"));

		Cart cart = cartRepository.findByUserAndIsActive(user, true).stream().findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

		CartItems existingItem = cart.getItems().stream()
				.filter(item -> item.getInventory().getId().equals(inventoryId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));

		if (existingItem.getQuantity() > 1) {
			existingItem.setQuantity(existingItem.getQuantity() - 1);
			cartItemRepository.save(existingItem);
			log.info("Decreased quantity. inventoryId={}, newQty={}",
					inventoryId, existingItem.getQuantity());
		} else {
			cart.getItems().remove(existingItem);
			cartItemRepository.delete(existingItem);
			log.info("Removed item from cart. inventoryId={}", inventoryId);
		}

		double totalMrp = 0.0;
		double totalPayable = 0.0;
		double totalDiscount = 0.0;

		for (CartItems item : cart.getItems()) {
			Product product = item.getInventory().getProduct();
			int qty = item.getQuantity();

			double mrp = product.getMRP() != null ? product.getMRP() : 0.0;
			double finalPrice = discountService.calculateFinalPrice(product);

			totalMrp += (mrp * qty);
			totalPayable += (finalPrice * qty);
			totalDiscount += ((mrp - finalPrice) * qty);
		}

		cart.setTotalAmount(totalMrp);
		cart.setPayableAmount(totalPayable);
		cart.setDiscountAmount(totalDiscount);

		Cart updatedCart = cartRepository.save(cart);

		return convertToCartDTO(updatedCart);
	}


	public CartDTO applyCoupon(Long couponId) {

		log.info("Applying coupon. couponId={}", couponId);

		Long userId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new RuntimeException("Unauthorized"));

		User user = customerRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Cart cart = getOrCreateActiveCart(user);

		if (cart.getItems().isEmpty()) {
			throw new RuntimeException("Cart is empty");
		}
		
		boolean usedBefore = cartRepository.findByUser(user).stream()
	            .flatMap(c -> c.getAppliedCoupons().stream())
	            .anyMatch(cc -> cc.getCoupon().getId().equals(couponId));

	    if (usedBefore) {
	        throw new RuntimeException("You have already used this coupon once.");
	    }
		
	    Coupon coupon = couponRepository.findById(couponId)
	            .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

		if (!coupon.isActive()) {
			throw new RuntimeException("Coupon is inactive");
		}

		if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(LocalDate.now())) {
			throw new RuntimeException("Coupon expired");
		}

		double totalAmount = cart.getPayableAmount();

		if (coupon.getMinOrderAmount() != null && totalAmount < coupon.getMinOrderAmount()) {
			throw new RuntimeException("Minimum amount required: " + coupon.getMinOrderAmount());
		}

		double discount;

		if (coupon.getDiscountType() == DiscountType.PERCENT) {
			discount = (totalAmount * coupon.getDiscountValue()) / 100;
			if (coupon.getMaxDiscountAmount() != null && discount > coupon.getMaxDiscountAmount()) {
				discount = coupon.getMaxDiscountAmount();
			}
		} else {
			discount = coupon.getDiscountValue();
		}

		cart.setDiscountAmount(cart.getDiscountAmount() + discount);
		cart.setPayableAmount(totalAmount - discount);

		CartCoupon cartCoupon = new CartCoupon();
		cartCoupon.setCart(cart);
		cartCoupon.setCoupon(coupon);
		cart.getAppliedCoupons().add(cartCoupon);

		cartRepository.save(cart);

		log.info("Coupon applied successfully. couponId={}, discount={}", couponId, discount);

		return convertToCartDTO(cart);
	}


	public CartDTO removeCoupon(Long couponId) {

		log.info("Removing coupon. couponId={}", couponId);

		Long userId = SecurityUtil.getCurrentUserId()
				.orElseThrow(() -> new RuntimeException("Unauthorized"));

		User user = customerRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Cart cart = getOrCreateActiveCart(user);

		CartCoupon appliedCoupon = cart.getAppliedCoupons().stream()
				.filter(c -> c.getCoupon().getId().equals(couponId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Coupon not found in this cart"));

		cart.getAppliedCoupons().remove(appliedCoupon);

		double totalMrp = 0.0;
		double totalPayable = 0.0;
		double totalDiscount = 0.0;

		for (CartItems item : cart.getItems()) {
			Product product = item.getInventory().getProduct();
			int qty = item.getQuantity();

			double mrp = product.getMRP() != null ? product.getMRP() : 0.0;
			double finalPrice = discountService.calculateFinalPrice(product);

			totalMrp += (mrp * qty);
			totalPayable += (finalPrice * qty);
			totalDiscount += ((mrp - finalPrice) * qty);
		}

		cart.setTotalAmount(totalMrp);
		cart.setDiscountAmount(totalDiscount);
		cart.setPayableAmount(totalPayable);

		cartRepository.save(cart);

		log.info("Coupon removed successfully. couponId={}", couponId);

		return convertToCartDTO(cart);
	}
}
