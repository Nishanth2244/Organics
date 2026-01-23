package com.organics.products.service;

import com.organics.products.dto.InventoryCreateRequest;
import com.organics.products.dto.InventoryResponse;
import com.organics.products.entity.*;
import com.organics.products.exception.BadRequestException;
import com.organics.products.exception.InventoryException;
import com.organics.products.exception.InventoryNotFoundException;
import com.organics.products.respository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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

        log.info("Creating inventory: productId={}, branchId={}",
                request.getProductId(), request.getBranchId());

        if (request.getProductId() == null || request.getBranchId() == null) {
            log.warn("Invalid inventory request: {}", request);
            throw new BadRequestException("ProductId and BranchId are required");
        }

        if (inventoryRepository.existsByProductIdAndBranchId(
                request.getProductId(), request.getBranchId())) {
            log.warn("Inventory already exists for product {} and branch {}",
                    request.getProductId(), request.getBranchId());
            throw new InventoryException("Inventory already exists for this product & branch");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", request.getProductId());
                    return new InventoryNotFoundException("Product not found: " + request.getProductId());
                });

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> {
                    log.warn("Branch not found: {}", request.getBranchId());
                    return new InventoryNotFoundException("Branch not found: " + request.getBranchId());
                });

        if (request.getStock() == null || request.getStock() < 0) {
            log.warn("Invalid stock value: {}", request.getStock());
            throw new BadRequestException("Stock must be >= 0");
        }

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setBranch(branch);
        inventory.setAvailableStock(request.getStock());
        inventory.setReservedStock(0);

        Inventory saved = inventoryRepository.save(inventory);

        saveTransaction(saved, InventoryTransactionType.IN,
                request.getStock(), InventoryReferenceType.ADMIN, null);

        log.info("Inventory created successfully: id={}", saved.getId());

        return mapToResponse(saved);
    }


    public void addStock(Long inventoryId, Integer quantity) {

        log.info("Adding stock: inventoryId={}, quantity={}", inventoryId, quantity);

        if (quantity == null || quantity <= 0) {
            log.warn("Invalid stock quantity: {}", quantity);
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> {
                    log.warn("Inventory not found: {}", inventoryId);
                    return new InventoryNotFoundException("Inventory not found: " + inventoryId);
                });

        inventory.setAvailableStock(inventory.getAvailableStock() + quantity);

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.IN,
                quantity, InventoryReferenceType.ADMIN, null);

        log.info("Stock added successfully: inventoryId={}, newAvailable={}",
                inventoryId, inventory.getAvailableStock());
    }


    public Page<InventoryResponse> getInventoryByBranch( int page, int size,Long branchId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        log.info("Fetching inventory for branch {}", branchId);

        Page<Inventory> inventories = inventoryRepository.findByBranchId(branchId,pageable);

        if (inventories.isEmpty()) {
            log.warn("No inventory found for branch {}", branchId);
            return Page.empty(pageable);
        }

        return inventories
                .map(this::mapToResponse);
                }

    public List<InventoryResponse> getInventoryByProduct(Long productId) {

        log.info("Fetching inventory for product {}", productId);

        List<Inventory> inventories = inventoryRepository.findByProductId(productId);

        if (inventories.isEmpty()) {
            log.warn("No inventory found for product {}", productId);
            throw new InventoryNotFoundException("No inventory found for product: " + productId);
        }

        return inventories.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Page<InventoryResponse> getAllInventory(int page,int size) {

        log.info("Fetching all inventory");
        Pageable pageable=PageRequest.of(page, size, Sort.by("id").descending());

        Page<Inventory> inventories = inventoryRepository.findAll(pageable);

        if (inventories.isEmpty()) {
            log.warn("No inventory records found");
            return Page.empty(pageable);
        }

        return inventories
                .map(this::mapToResponse);

    }


    public void reserveStock(Long inventoryId, Integer quantity, Long orderId) {

        log.info("Reserving stock: inventoryId={}, qty={}, orderId={}",
                inventoryId, quantity, orderId);

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> {
                    log.warn("Inventory not found: {}", inventoryId);
                    return new InventoryNotFoundException("Inventory not found: " + inventoryId);
                });

        if (inventory.getAvailableStock() < quantity) {
            log.warn("Insufficient stock: available={}, requested={}",
                    inventory.getAvailableStock(), quantity);
            throw new InventoryException("Insufficient stock");
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventory.setReservedStock(inventory.getReservedStock() + quantity);

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.SOLD,
                quantity, InventoryReferenceType.ORDER, orderId);

        log.info("Stock reserved successfully: inventoryId={}", inventoryId);
    }

    public void confirmStock(Long inventoryId, Integer quantity, Long orderId) {

        log.info("Confirming stock: inventoryId={}, qty={}, orderId={}",
                inventoryId, quantity, orderId);

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> {
                    log.warn("Inventory not found: {}", inventoryId);
                    return new InventoryNotFoundException("Inventory not found: " + inventoryId);
                });

        if (inventory.getReservedStock() < quantity) {
            log.warn("Reserved stock less than confirm qty: reserved={}, requested={}",
                    inventory.getReservedStock(), quantity);
            throw new InventoryException("Not enough reserved stock to confirm");
        }

        inventory.setReservedStock(inventory.getReservedStock() - quantity);

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.SOLD,
                quantity, InventoryReferenceType.ORDER, orderId);

        log.info("Stock confirmed successfully: inventoryId={}", inventoryId);
    }

    public void releaseStock(Long inventoryId, Integer quantity, Long orderId) {

        log.info("Releasing stock: inventoryId={}, qty={}, orderId={}",
                inventoryId, quantity, orderId);

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> {
                    log.warn("Inventory not found: {}", inventoryId);
                    return new InventoryNotFoundException("Inventory not found: " + inventoryId);
                });

        if (inventory.getReservedStock() < quantity) {
            log.warn("Reserved stock less than release qty: reserved={}, requested={}",
                    inventory.getReservedStock(), quantity);
            throw new InventoryException("Not enough reserved stock to release");
        }

        inventory.setReservedStock(inventory.getReservedStock() - quantity);
        inventory.setAvailableStock(inventory.getAvailableStock() + quantity);

        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.RELEASE,
                quantity, InventoryReferenceType.ORDER, orderId);

        log.info("Stock released successfully: inventoryId={}", inventoryId);
    }


    public Page<InventoryTransactions> getInventoryTransactions(int page,int size,Long inventoryId) {

        log.info("Fetching transactions for inventory {}", inventoryId);

        Pageable pageable= PageRequest.of(page,size);
        Page<InventoryTransactions> txs =
                transactionRepository.findByInventoryIdOrderByTransactionDateDesc(inventoryId,pageable);

        if (txs.isEmpty()) {
            log.warn("No transactions found for inventory {}", inventoryId);
            return Page.empty(pageable);
        }

        return txs;
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
}
