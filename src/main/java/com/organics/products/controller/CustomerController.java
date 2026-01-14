//package com.organics.products.controller;
//
//import com.organics.products.dto.CustomerDTO;
//import com.organics.products.dto.AddressDTO;
//import com.organics.products.dto.CustomerWithAddressesDTO;
//import com.organics.products.service.CustomerService;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/customers")
//@RequiredArgsConstructor
//@Slf4j
//public class CustomerController {
//    
//    private final CustomerService customerService;
//    
//    // Customer endpoints
//    
//    @PostMapping
//    public ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerDTO customerDTO) {
//        log.info("Received request to create customer");
//        CustomerDTO createdCustomer = customerService.createCustomer(customerDTO);
//        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
//    }
//    
//    @GetMapping("/{id}")
//    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
//        log.info("Received request to get customer by ID: {}", id);
//        CustomerDTO customer = customerService.getCustomerById(id);
//        return ResponseEntity.ok(customer);
//    }
//    
//    @GetMapping("/{id}/with-addresses")
//    public ResponseEntity<CustomerWithAddressesDTO> getCustomerWithAddresses(@PathVariable Long id) {
//        log.info("Received request to get customer with addresses by ID: {}", id);
//        CustomerWithAddressesDTO customer = customerService.getCustomerWithAddresses(id);
//        return ResponseEntity.ok(customer);
//    }
//    
//    @GetMapping
//    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
//        log.info("Received request to get all customers");
//        List<CustomerDTO> customers = customerService.getAllCustomers();
//        return ResponseEntity.ok(customers);
//    }
//    
//    @GetMapping("/phone/{phoneNumber}")
//    public ResponseEntity<CustomerDTO> getCustomerByPhoneNumber(@PathVariable Long phoneNumber) {
//        log.info("Received request to get customer by phone number: {}", phoneNumber);
//        CustomerDTO customer = customerService.getCustomerByPhoneNumber(phoneNumber);
//        return ResponseEntity.ok(customer);
//    }
//    
//    @PutMapping("/{id}")
//    public ResponseEntity<CustomerDTO> updateCustomer(
//            @PathVariable Long id, 
//            @RequestBody CustomerDTO customerDTO) {
//        log.info("Received request to update customer with ID: {}", id);
//        CustomerDTO updatedCustomer = customerService.updateCustomer(id, customerDTO);
//        return ResponseEntity.ok(updatedCustomer);
//    }
//    
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
//        log.info("Received request to delete customer with ID: {}", id);
//        customerService.deleteCustomer(id);
//        return ResponseEntity.noContent().build();
//    }
//    
//    // Address endpoints
//    
//    @PostMapping("/{customerId}/addresses")
//    public ResponseEntity<AddressDTO> addAddress(
//            @PathVariable Long customerId,
//             @RequestBody AddressDTO addressDTO) {
//        log.info("Received request to add address for customer ID: {}", customerId);
//        addressDTO.setCustomerId(customerId);
//        AddressDTO createdAddress = customerService.addAddress(addressDTO);
//        return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
//    }
//    
//    @GetMapping("/{customerId}/addresses")
//    public ResponseEntity<List<AddressDTO>> getCustomerAddresses(@PathVariable Long customerId) {
//        log.info("Received request to get addresses for customer ID: {}", customerId);
//        List<AddressDTO> addresses = customerService.getAddressesByCustomerId(customerId);
//        return ResponseEntity.ok(addresses);
//    }
//    
//    @PutMapping("/addresses/{id}")
//    public ResponseEntity<AddressDTO> updateAddress(
//            @PathVariable Long id,
//             @RequestBody AddressDTO addressDTO) {
//        log.info("Received request to update address with ID: {}", id);
//        AddressDTO updatedAddress = customerService.updateAddress(id, addressDTO);
//        return ResponseEntity.ok(updatedAddress);
//    }
//    
//    @DeleteMapping("/addresses/{id}")
//    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
//        log.info("Received request to delete address with ID: {}", id);
//        customerService.deleteAddress(id);
//        return ResponseEntity.noContent().build();
//    }
//}