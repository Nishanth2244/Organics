package com.organics.products.service;

import com.organics.products.dto.InventoryCreateRequest;
import com.organics.products.dto.InventoryResponse;
import com.organics.products.entity.*;
import com.organics.products.respository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepo productRepository;
    private final BranchRepository branchRepository;
    private final InventoryTransactionRepository transactionRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepo productRepository,
            BranchRepository branchRepository,
            InventoryTransactionRepository transactionRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.transactionRepository = transactionRepository;
    }

    public InventoryResponse createInventory(InventoryCreateRequest request) {

        if (inventoryRepository.existsByProductIdAndBranchId(
                request.getProductId(), request.getBranchId())) {
            throw new RuntimeException("Inventory already exists");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setBranch(branch);
        inventory.setAvailableStock(request.getStock());
        inventory.setReservedStock(0);

        Inventory saved = inventoryRepository.save(inventory);

        saveTransaction(saved, InventoryTransactionType.IN,
                request.getStock(), InventoryReferenceType.ADMIN, null);

        return mapToResponse(saved);
    }

    public void addStock(Long inventoryId, Integer quantity) {

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setAvailableStock(
                inventory.getAvailableStock() + quantity
        );

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.IN,
                quantity, InventoryReferenceType.ADMIN, null);
    }

    public List<InventoryResponse> getInventoryByBranch(Long branchId) {

        return inventoryRepository.findByBranchId(branchId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<InventoryResponse> getInventoryByProduct(Long productId) {

        return inventoryRepository.findByProductId(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void reserveStock(Long inventoryId, Integer quantity, Long orderId) {

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (inventory.getAvailableStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventory.setReservedStock(inventory.getReservedStock() + quantity);

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.RESERVE,
                quantity, InventoryReferenceType.ORDER, orderId);
    }

    public void confirmStock(Long inventoryId, Integer quantity, Long orderId) {

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setReservedStock(
                inventory.getReservedStock() - quantity
        );

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.SOLD,
                quantity, InventoryReferenceType.ORDER, orderId);
    }

    public void releaseStock(Long inventoryId, Integer quantity, Long orderId) {

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setReservedStock(
                inventory.getReservedStock() - quantity
        );
        inventory.setAvailableStock(
                inventory.getAvailableStock() + quantity
        );

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.RELEASE,
                quantity, InventoryReferenceType.ORDER, orderId);
    }

    public List<InventoryTransactions> getInventoryTransactions(Long inventoryId) {

        return transactionRepository
                .findByInventoryIdOrderByTransactionDateDesc(inventoryId);
    }

    private void saveTransaction(
            Inventory inventory,
            InventoryTransactionType type,
            Integer quantity,
            InventoryReferenceType refType,
            Long refId
    ) {
        InventoryTransactions tx = new InventoryTransactions();
        tx.setInventory(inventory);
        tx.setTransactionType(type);
        tx.setQuantity(quantity);
        tx.setReferenceType(refType);
        tx.setReferenceId(refId);
        transactionRepository.save(tx);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {

        InventoryResponse r = new InventoryResponse();
        r.setInventoryId(inventory.getId());
        r.setProductId(inventory.getProduct().getId());
        r.setBranchId(inventory.getBranch().getId());
        r.setAvailableStock(inventory.getAvailableStock());
        r.setReservedStock(inventory.getReservedStock());
        return r;
    }


    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

}
