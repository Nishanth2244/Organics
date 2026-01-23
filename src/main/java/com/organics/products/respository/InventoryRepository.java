package com.organics.products.respository;

import com.organics.products.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {


    Page<Inventory> findByBranchId(Long branchId,Pageable pageable);

    boolean existsByProductIdAndBranchId(Long productId, Long branchId);

    List<Inventory> findByProductId(Long productId);


    Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

}
