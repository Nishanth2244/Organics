package com.organics.products.controller;

import com.organics.products.dto.AddressResponse;
import com.organics.products.dto.SaveAddressRequest;
import com.organics.products.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/user/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<AddressResponse> saveAddress(
            @Valid @RequestBody SaveAddressRequest request
    ) {
        return ResponseEntity.ok(addressService.saveAddress(request));
    }

    @GetMapping
    public ResponseEntity<Page<AddressResponse>> getMyAddresses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(addressService.getMyAddresses(page,size));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody SaveAddressRequest request
    ) {
        return ResponseEntity.ok(
                addressService.updateAddress(addressId, request)
        );
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }
}
