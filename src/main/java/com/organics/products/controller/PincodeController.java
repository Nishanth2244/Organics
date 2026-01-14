package com.organics.products.controller;

import com.organics.products.dto.LocationResponse;
import com.organics.products.service.PincodeService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/address")
public class PincodeController {

    private final PincodeService pincodeService;

    public PincodeController(PincodeService pincodeService) {
        this.pincodeService = pincodeService;
    }

    @GetMapping("/pincode/{pinCode}")
    public LocationResponse getByPin(@PathVariable String pinCode) {
        return pincodeService.getByPincode(pinCode);
    }
}
