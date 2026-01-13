//package com.organics.products.service;
//
//import com.organics.products.dto.CustomerDTO;
//import com.organics.products.dto.AddressDTO;
//import com.organics.products.dto.CustomerWithAddressesDTO;
//import com.organics.products.entity.Customer;
//import com.organics.products.entity.Address;
//
//import com.organics.products.respository.AddressRepository;
//import com.organics.products.respository.CustomerRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CustomerService {
//    
//    private final CustomerRepository customerRepository;
//    private final AddressRepository addressRepository;
//    
//    // Convert Entity to DTO
//    private CustomerDTO convertToCustomerDTO(Customer customer) {
//        CustomerDTO dto = new CustomerDTO();
//        dto.setDisplayName(customer.getDisplayName());
//        dto.setPhoneNumber(customer.getPhoneNumber());
//        dto.setEmailId(customer.getEmailId());
//        dto.setFirstName(customer.getFirstName());
//        dto.setMiddleName(customer.getMiddleName());
//        dto.setLastName(customer.getLastName());
//        dto.setGender(customer.getGender());
//        dto.setDateOfBirth(customer.getDateOfBirth());
//        dto.setAge(customer.getAge());
//        return dto;
//    }
//    
//    // Convert DTO to Entity
//    private Customer convertToCustomerEntity(CustomerDTO dto) {
//        Customer customer = new Customer();
//        customer.setDisplayName(dto.getDisplayName());
//        customer.setPhoneNumber(dto.getPhoneNumber());
//        customer.setEmailId(dto.getEmailId());
//        customer.setFirstName(dto.getFirstName());
//        customer.setMiddleName(dto.getMiddleName());
//        customer.setLastName(dto.getLastName());
//        customer.setGender(dto.getGender());
//        customer.setDateOfBirth(dto.getDateOfBirth());
//        customer.setAge(dto.getAge());
//        return customer;
//    }
//    
//    // Convert Address Entity to DTO
//    private AddressDTO convertToAddressDTO(Address address) {
//        AddressDTO dto = new AddressDTO();
//        dto.setId(address.getId());
//        dto.setPhoneNumber(address.getPhoneNumber());
//        dto.setHouseNumber(address.getHouseNumber());
//        dto.setApartmentName(address.getApartmentName());
//        dto.setStreetName(address.getStreetName());
//        dto.setState(address.getState());
//        dto.setCity(address.getCity());
//        dto.setPinCode(address.getPinCode());
//        dto.setLandMark(address.getLandMark());
//        dto.setCustomerId(address.getCustomer().getId());
//        return dto;
//    }
//    
//    // Convert Address DTO to Entity
//    private Address convertToAddressEntity(AddressDTO dto, Customer customer) {
//        Address address = new Address();
//        address.setId(dto.getId());
//        address.setPhoneNumber(dto.getPhoneNumber());
//        address.setHouseNumber(dto.getHouseNumber());
//        address.setApartmentName(dto.getApartmentName());
//        address.setStreetName(dto.getStreetName());
//        address.setState(dto.getState());
//        address.setCity(dto.getCity());
//        address.setPinCode(dto.getPinCode());
//        address.setLandMark(dto.getLandMark());
//        address.setCustomer(customer);
//        return address;
//    }
//    
//    @Transactional
//    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
//        log.info("Creating new customer with phone number: {}", customerDTO.getPhoneNumber());
//        
//        // Check if phone number already exists
//        if (customerRepository.existsByPhoneNumber(customerDTO.getPhoneNumber())) {
//            throw new IllegalArgumentException("Customer with phone number " + customerDTO.getPhoneNumber() + " already exists");
//        }
//        
//        // Check if email exists (if provided)
//        if (customerDTO.getEmailId() != null && !customerDTO.getEmailId().isEmpty() 
//            && customerRepository.existsByEmailId(customerDTO.getEmailId())) {
//            throw new IllegalArgumentException("Customer with email " + customerDTO.getEmailId() + " already exists");
//        }
//        
//        Customer customer = convertToCustomerEntity(customerDTO);
//        Customer savedCustomer = customerRepository.save(customer);
//        log.info("Customer created with ID: {}", savedCustomer.getId());
//        
//        return convertToCustomerDTO(savedCustomer);
//    }
//    
//    @Transactional(readOnly = true)
//    public CustomerDTO getCustomerById(Long id) {
//        log.info("Fetching customer with ID: {}", id);
//        Customer customer = customerRepository.findById(id)
//            .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
//        return convertToCustomerDTO(customer);
//    }
//    
//    @Transactional(readOnly = true)
//    public CustomerWithAddressesDTO getCustomerWithAddresses(Long id) {
//        log.info("Fetching customer with addresses for ID: {}", id);
//        Customer customer = customerRepository.findByIdWithAddresses(id)
//            .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
//        
//        CustomerWithAddressesDTO dto = new CustomerWithAddressesDTO();
//        dto.setId(customer.getId());
//        dto.setDisplayName(customer.getDisplayName());
//        dto.setPhoneNumber(customer.getPhoneNumber());
//        dto.setEmailId(customer.getEmailId());
//        dto.setFirstName(customer.getFirstName());
//        dto.setMiddleName(customer.getMiddleName());
//        dto.setLastName(customer.getLastName());
//        dto.setGender(customer.getGender());
//        dto.setDateOfBirth(customer.getDateOfBirth());
//        dto.setAge(customer.getAge());
//        
//        // Convert addresses to DTOs
//        List<AddressDTO> addressDTOs = customer.getAddresses().stream()
//            .map(this::convertToAddressDTO)
//            .collect(Collectors.toList());
//        dto.setAddresses(addressDTOs);
//        
//        return dto;
//    }
//    
//    @Transactional(readOnly = true)
//    public List<CustomerDTO> getAllCustomers() {
//        log.info("Fetching all customers");
//        return customerRepository.findAll().stream()
//            .map(this::convertToCustomerDTO)
//            .collect(Collectors.toList());
//    }
//    
//    @Transactional(readOnly = true)
//    public CustomerDTO getCustomerByPhoneNumber(Long phoneNumber) {
//        log.info("Fetching customer with phone number: {}", phoneNumber);
//        Customer customer = customerRepository.findByPhoneNumber(phoneNumber)
//            .orElseThrow(() -> new RuntimeException("Customer not found with phone number: " + phoneNumber));
//        return convertToCustomerDTO(customer);
//    }
//    
//    @Transactional
//    public CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO) {
//        log.info("Updating customer with ID: {}", id);
//        Customer existingCustomer = customerRepository.findById(id)
//            .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
//        
//        // Check if phone number is being changed and if it already exists
//        if (!existingCustomer.getPhoneNumber().equals(customerDTO.getPhoneNumber()) 
//            && customerRepository.existsByPhoneNumber(customerDTO.getPhoneNumber())) {
//            throw new IllegalArgumentException("Phone number already exists");
//        }
//        
//        // Check if email is being changed and if it already exists
//        if (customerDTO.getEmailId() != null && !customerDTO.getEmailId().isEmpty() 
//            && !customerDTO.getEmailId().equals(existingCustomer.getEmailId()) 
//            && customerRepository.existsByEmailId(customerDTO.getEmailId())) {
//            throw new IllegalArgumentException("Email already exists");
//        }
//        
//        // Update fields
//        existingCustomer.setDisplayName(customerDTO.getDisplayName());
//        existingCustomer.setPhoneNumber(customerDTO.getPhoneNumber());
//        existingCustomer.setEmailId(customerDTO.getEmailId());
//        existingCustomer.setFirstName(customerDTO.getFirstName());
//        existingCustomer.setMiddleName(customerDTO.getMiddleName());
//        existingCustomer.setLastName(customerDTO.getLastName());
//        existingCustomer.setGender(customerDTO.getGender());
//        existingCustomer.setDateOfBirth(customerDTO.getDateOfBirth());
//        existingCustomer.setAge(customerDTO.getAge());
//        
//        Customer updatedCustomer = customerRepository.save(existingCustomer);
//        log.info("Customer updated with ID: {}", id);
//        
//        return convertToCustomerDTO(updatedCustomer);
//    }
//    
//    @Transactional
//    public void deleteCustomer(Long id) {
//        log.info("Deleting customer with ID: {}", id);
//        if (!customerRepository.existsById(id)) {
//            throw new RuntimeException("Customer not found with ID: " + id);
//        }
//        
//        // Delete associated addresses first
//        addressRepository.deleteByCustomerId(id);
//        
//        // Delete customer
//        customerRepository.deleteById(id);
//        log.info("Customer deleted with ID: {}", id);
//    }
//    
//    // Address Service Methods
//    @Transactional
//    public AddressDTO addAddress(AddressDTO addressDTO) {
//        log.info("Adding address for customer ID: {}", addressDTO.getCustomerId());
//        
//        Customer customer = customerRepository.findById(addressDTO.getCustomerId())
//            .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + addressDTO.getCustomerId()));
//        
//        Address address = convertToAddressEntity(addressDTO, customer);
//        Address savedAddress = addressRepository.save(address);
//        log.info("Address created with ID: {}", savedAddress.getId());
//        
//        return convertToAddressDTO(savedAddress);
//    }
//    
//    @Transactional(readOnly = true)
//    public List<AddressDTO> getAddressesByCustomerId(Long customerId) {
//        log.info("Fetching addresses for customer ID: {}", customerId);
//        return addressRepository.findByCustomerId(customerId).stream()
//            .map(this::convertToAddressDTO)
//            .collect(Collectors.toList());
//    }
//    
//    @Transactional
//    public AddressDTO updateAddress(Long id, AddressDTO addressDTO) {
//        log.info("Updating address with ID: {}", id);
//        Address existingAddress = addressRepository.findById(id)
//            .orElseThrow(() -> new RuntimeException("Address not found with ID: " + id));
//        
//        Customer customer = customerRepository.findById(addressDTO.getCustomerId())
//            .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + addressDTO.getCustomerId()));
//        
//        existingAddress.setPhoneNumber(addressDTO.getPhoneNumber());
//        existingAddress.setHouseNumber(addressDTO.getHouseNumber());
//        existingAddress.setApartmentName(addressDTO.getApartmentName());
//        existingAddress.setStreetName(addressDTO.getStreetName());
//        existingAddress.setState(addressDTO.getState());
//        existingAddress.setCity(addressDTO.getCity());
//        existingAddress.setPinCode(addressDTO.getPinCode());
//        existingAddress.setLandMark(addressDTO.getLandMark());
//        existingAddress.setCustomer(customer);
//        
//        Address updatedAddress = addressRepository.save(existingAddress);
//        log.info("Address updated with ID: {}", id);
//        
//        return convertToAddressDTO(updatedAddress);
//    }
//    
//    @Transactional
//    public void deleteAddress(Long id) {
//        log.info("Deleting address with ID: {}", id);
//        if (!addressRepository.existsById(id)) {
//            throw new RuntimeException("Address not found with ID: " + id);
//        }
//        addressRepository.deleteById(id);
//        log.info("Address deleted with ID: {}", id);
//    }
//}