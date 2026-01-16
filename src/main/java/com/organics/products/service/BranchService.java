package com.organics.products.service;

import com.organics.products.dto.BranchCreateRequest;
import com.organics.products.dto.BranchResponse;
import com.organics.products.entity.Branch;
import com.organics.products.respository.BranchRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public BranchResponse createBranch(BranchCreateRequest request) {

        if (branchRepository.existsByBranchCode(request.getBranchCode())) {
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
        return mapToResponse(saved);
    }

    public List<BranchResponse> getAllBranches() {

        return branchRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void updateBranchStatus(Long branchId, Boolean active) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setActive(active);
        branchRepository.save(branch);
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

    public BranchResponse updateBranch(Long branchId,BranchCreateRequest  request) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setBranchName(request.getBranchName());
        branch.setLocation(request.getLocation());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());
        branch.setChargePerKm(request.getChargePerKm());

        Branch updated = branchRepository.save(branch);
        return mapToResponse(updated);
    }

}
