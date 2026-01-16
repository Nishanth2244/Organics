package com.organics.products.respository;

import com.organics.products.entity.InventoryTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransactions, Long> {
    List<InventoryTransactions> findByInventoryId(Long inventoryId);

    List<InventoryTransactions> findByInventoryIdOrderByTransactionDateDesc(Long inventoryId);
}
