package com.organics.products.service;

import com.organics.products.dto.BranchCreateRequest;
import com.organics.products.dto.BranchResponse;
import com.organics.products.entity.Branch;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.BranchRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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


    public List<BranchResponse> getAllBranches() {

        log.info("Fetching all branches");

        List<Branch> branches = branchRepository.findAll();

        if (branches == null || branches.isEmpty()) {
            log.info("No branches found");
            return List.of();
        }

        log.info("Found {} branches", branches.size());

        return branches.stream()
                .map(this::mapToResponse)
                .toList();
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


    public List<BranchResponse> getBranchesByStatus(Boolean active) {

        log.info("Fetching branches by status. active={}", active);

        List<Branch> branches = branchRepository.findByActive(active);

        if (branches == null || branches.isEmpty()) {
            log.info("No branches found with active={}", active);
            return List.of();
        }

        log.info("Found {} branches with active={}", branches.size(), active);

        return branches.stream()
                .map(this::mapToResponse)
                .toList();
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
