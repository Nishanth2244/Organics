    package com.organics.products.controller;

    import com.organics.products.dto.BranchCreateRequest;
    import com.organics.products.dto.BranchResponse;
    import com.organics.products.service.BranchService;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/api/admin/branches")
    public class BranchController {

        private final BranchService branchService;

        public BranchController(BranchService branchService) {
            this.branchService = branchService;
        }

        @PostMapping
        public ResponseEntity<BranchResponse> createBranch(
                @RequestBody BranchCreateRequest request
        ) {
            return ResponseEntity.ok(branchService.createBranch(request));
        }
        @PutMapping("/{branchId}")
        public ResponseEntity<BranchResponse> updateBranch(
                @PathVariable Long branchId,
                @RequestBody  BranchCreateRequest request
        ) {
            return ResponseEntity.ok(
                    branchService.updateBranch(branchId, request)
            );
        }


        @GetMapping
        public ResponseEntity<List<BranchResponse>> getAllBranches() {
            return ResponseEntity.ok(branchService.getAllBranches());
        }

        @PutMapping("/{branchId}/status")
        public ResponseEntity<Void> updateBranchStatus(
                @PathVariable Long branchId,
                @RequestParam Boolean active
        ) {
            branchService.updateBranchStatus(branchId, active);
            return ResponseEntity.noContent().build();
        }


        @GetMapping("/all")
        public ResponseEntity<List<BranchResponse>> getBranchesByStatus(
                @RequestParam Boolean active
        ) {
            return ResponseEntity.ok(branchService.getBranchesByStatus(active));
        }

    }

