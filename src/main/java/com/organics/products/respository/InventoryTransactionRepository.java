package com.organics.products.respository;

import com.organics.products.entity.InventoryTransactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransactions, Long> {
    List<InventoryTransactions> findByInventoryId(Long inventoryId);

    Page<InventoryTransactions> findByInventoryIdOrderByTransactionDateDesc( Long inventoryId,Pageable pageable);
}
