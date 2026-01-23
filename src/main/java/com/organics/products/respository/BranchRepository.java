package com.organics.products.respository;

import com.organics.products.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByBranchCode(String branchCode);

    Page<Branch> findByActive(Boolean active,Pageable pageable);
}
