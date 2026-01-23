package com.organics.products.service;

import com.organics.products.dto.BranchCreateRequest;
import com.organics.products.dto.BranchResponse;
import com.organics.products.entity.Branch;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.BranchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }


    public BranchResponse createBranch(BranchCreateRequest request) {

        log.info("Creating new branch with code={}", request.getBranchCode());

        if (branchRepository.existsByBranchCode(request.getBranchCode())) {
            log.warn("Branch code already exists: {}", request.getBranchCode());
            throw new RuntimeException("Branch code already exists");
        }

        Branch branch = new Branch();
        branch.setBranchName(request.getBranchName());
        branch.setBranchCode(request.getBranchCode());
        branch.setLocation(request.getLocation());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());
        branch.setChargePerKm(request.getChargePerKm());
        branch.setActive(true);

        Branch saved = branchRepository.save(branch);

        log.info("Branch created successfully. branchId={}, code={}",
                saved.getId(), saved.getBranchCode());

        return mapToResponse(saved);
    }


    public Page<BranchResponse> getAllBranches(int page, int size) {

        log.info("Fetching all branches: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Branch> branchesPage = branchRepository.findAll(pageable);

        if (branchesPage.isEmpty()) {
            log.info("No branches found");
            return Page.empty(pageable);
        }

        log.info("Found {} branches", branchesPage.getTotalElements());

        return branchesPage.map(this::mapToResponse);
    }




    public void updateBranchStatus(Long branchId, Boolean active) {

        log.info("Updating branch status. branchId={}, active={}", branchId, active);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> {
                    log.warn("Branch not found for id={}", branchId);
                    return new ResourceNotFoundException("Branch not found with id: " + branchId);
                });

        branch.setActive(active);
        branchRepository.save(branch);

        log.info("Branch status updated successfully. branchId={}, active={}",
                branchId, active);
    }


    public BranchResponse updateBranch(Long branchId, BranchCreateRequest request) {

        log.info("Updating branch. branchId={}", branchId);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> {
                    log.warn("Branch not found for id={}", branchId);
                    return new ResourceNotFoundException("Branch not found with id: " + branchId);
                });

        branch.setBranchName(request.getBranchName());
        branch.setLocation(request.getLocation());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());
        branch.setChargePerKm(request.getChargePerKm());

        Branch updated = branchRepository.save(branch);

        log.info("Branch updated successfully. branchId={}", branchId);

        return mapToResponse(updated);
    }


    public Page<BranchResponse> getBranchesByStatus(int page,int size,Boolean active) {

        log.info("Fetching branches by status. active={}, page={}, size={}", active, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Branch> branches = branchRepository.findByActive(active,pageable);


        if (branches.isEmpty()) {
            log.info("No branches found with active={}", active);
            return Page.empty(pageable);
        }

        log.info("Found {} branches with active={}", branches.getTotalElements(), active);

        return branches.map(this::mapToResponse);

    }


    private BranchResponse mapToResponse(Branch branch) {

        BranchResponse r = new BranchResponse();
        r.setId(branch.getId());
        r.setBranchName(branch.getBranchName());
        r.setBranchCode(branch.getBranchCode());
        r.setLocation(branch.getLocation());
        r.setLatitude(branch.getLatitude());
        r.setLongitude(branch.getLongitude());
        r.setChargePerKm(branch.getChargePerKm());
        r.setActive(branch.getActive());

        return r;
    }
}
