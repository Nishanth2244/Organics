package com.organics.products.respository;

import com.organics.products.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {


    List<Inventory> findByBranchId(Long branchId);

    boolean existsByProductIdAndBranchId(Long productId, Long branchId);


    Optional<Inventory> findByProductId(Long productId);

    Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

}
