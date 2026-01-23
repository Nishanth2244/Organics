    package com.organics.products.controller;

    import com.organics.products.dto.BranchCreateRequest;
    import com.organics.products.dto.BranchResponse;
    import com.organics.products.service.BranchService;
    import org.springframework.data.domain.Page;
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
        public ResponseEntity<Page<BranchResponse>> getAllBranches(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

            return ResponseEntity.ok(branchService.getAllBranches(page, size));
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
        public ResponseEntity<Page<BranchResponse>> getBranchesByStatus(
                @RequestParam(defaultValue = "0")int  page,@RequestParam(defaultValue = "10")int size,
                @RequestParam Boolean active
        ) {
            return ResponseEntity.ok(branchService.getBranchesByStatus(page,size,active));
        }

    }

