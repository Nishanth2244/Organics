package com.organics.products.respository;

import com.organics.products.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByBranchCode(String branchCode);
}
