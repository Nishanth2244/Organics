package com.organics.products.controller;

import com.organics.products.dto.InventoryCreateRequest;
import com.organics.products.dto.InventoryResponse;
import com.organics.products.entity.InventoryTransactions;
import com.organics.products.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inventory")
    public ResponseEntity<InventoryResponse> createInventory(
            @RequestBody InventoryCreateRequest request
    ) {
        return ResponseEntity.ok(inventoryService.createInventory(request));
    }

    @PostMapping("/inventory/{inventoryId}/add-stock")
    public ResponseEntity<Void> addStock(
            @PathVariable Long inventoryId,
            @RequestParam Integer quantity
    ) {
        inventoryService.addStock(inventoryId, quantity);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inventory/branch/{branchId}")
    public ResponseEntity<Page<InventoryResponse>> getInventoryByBranch(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size,
            @PathVariable Long branchId
    ) {
        return ResponseEntity.ok(
                inventoryService.getInventoryByBranch(page,size,branchId)
        );
    }

    @GetMapping("/inventory/product/{productId}")
    public ResponseEntity<List<InventoryResponse>> getInventoryByProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                inventoryService.getInventoryByProduct(productId)
        );
    }

    @PostMapping("/inventory/{inventoryId}/reserve")
    public ResponseEntity<Void> reserveStock(
            @PathVariable Long inventoryId,
            @RequestParam Integer quantity,
            @RequestParam Long orderId
    ) {
        inventoryService.reserveStock(inventoryId, quantity, orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inventory/{inventoryId}/confirm")
    public ResponseEntity<Void> confirmStock(
            @PathVariable Long inventoryId,
            @RequestParam Integer quantity,
            @RequestParam Long orderId
    ) {
        inventoryService.confirmStock(inventoryId, quantity, orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inventory/{inventoryId}/release")
    public ResponseEntity<Void> releaseStock(
            @PathVariable Long inventoryId,
            @RequestParam Integer quantity,
            @RequestParam Long orderId
    ) {
        inventoryService.releaseStock(inventoryId, quantity, orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inventory/{inventoryId}/transactions")
    public ResponseEntity<Page<InventoryTransactions>> getInventoryTransactions(@RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "10") int size,
            @PathVariable Long inventoryId
    ) {
        return ResponseEntity.ok(
                inventoryService.getInventoryTransactions(page,size,inventoryId)
        );
    }

    @GetMapping("/inventory")
    public ResponseEntity<Page<InventoryResponse>> getAllInventory(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inventoryService.getAllInventory(page,size));
    }

}
