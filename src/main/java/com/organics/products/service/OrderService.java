package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.OrderItemDTO;
import com.organics.products.dto.OrderResponseDTO;
import com.organics.products.dto.PlaceOrderRequestDTO;
import com.organics.products.entity.*;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemsRepository orderItemsRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AddressRepository addressRepository;

    public OrderResponseDTO placeOrder(PlaceOrderRequestDTO request) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserAndIsActive(user, true)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No active cart found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid address"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        for (CartItems ci : cart.getItems()) {
            Inventory inventory = ci.getInventory();
            if (inventory.getAvailableStock() < ci.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " +
                        inventory.getProduct().getProductName());
            }
        }

        for (CartItems ci : cart.getItems()) {
            Inventory inventory = ci.getInventory();
            inventory.setAvailableStock(
                    inventory.getAvailableStock() - ci.getQuantity()
            );
            inventoryRepository.save(inventory);
        }

        Order order = new Order();
        order.setUser(user);
        order.setCart(cart);
        order.setAddress(address);
        order.setOrderDate(LocalDate.now());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setOrderAmount(cart.getPayableAmount());
        order.setDescription("Order placed from cart: " + cart.getId());

        Order savedOrder = orderRepository.save(order);

        List<OrderItems> orderItemsList = new ArrayList<>();
        for (CartItems ci : cart.getItems()) {
            OrderItems oi = new OrderItems();
            oi.setOrder(savedOrder);
            oi.setProduct(ci.getInventory().getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getInventory().getProduct().getMRP());
            oi.setDiscount(cart.getDiscountAmount());
            oi.setTax(0.0);
            orderItemsRepository.save(oi);
            orderItemsList.add(oi);
        }

        savedOrder.setOrderItems(orderItemsList);

        cart.setActive(false);
        cartRepository.save(cart);

        return mapToDTO(savedOrder);
    }

    private OrderResponseDTO mapToDTO(Order order) {

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setOrderAmount(order.getOrderAmount());
        dto.setStatus(order.getOrderStatus());

        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        for (OrderItems oi : order.getOrderItems()) {
            OrderItemDTO itemDTO = new OrderItemDTO();
            itemDTO.setProductId(oi.getProduct().getId());
            itemDTO.setProductName(oi.getProduct().getProductName());
            itemDTO.setQuantity(oi.getQuantity());
            itemDTO.setPrice(oi.getPrice());
            itemDTOs.add(itemDTO);
        }

        dto.setItems(itemDTOs);
        return dto;
    }
}
