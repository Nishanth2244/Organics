package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.AddressResponse;
import com.organics.products.dto.LocationResponse;
import com.organics.products.dto.SaveAddressRequest;
import com.organics.products.entity.Address;
import com.organics.products.entity.User;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.AddressRepository;
import com.organics.products.respository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PincodeService pincodeService;

    public AddressService(AddressRepository addressRepository,
                          UserRepository userRepository,
                          PincodeService pincodeService) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.pincodeService = pincodeService;
    }


    public AddressResponse saveAddress(SaveAddressRequest req) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("Unauthorized attempt to save address");
                    return new RuntimeException("User not authenticated");
                });

        log.info("Saving address for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        LocationResponse location =
                pincodeService.getByPincode(req.getPinCode());

        Address address = new Address();
        address.setAddressType(req.getAddressType());
        address.setAlternatePhoneNumber(req.getAlternatePhoneNumber());
        address.setHouseNumber(req.getHouseNumber());
        address.setApartmentName(req.getApartmentName());
        address.setStreetName(req.getStreetName());
        address.setLandMark(req.getLandMark());
        address.setCity(location.getCity());
        address.setState(location.getState());
        address.setPinCode(req.getPinCode());
        address.setLatitude(req.getLatitude());
        address.setLongitude(req.getLongitude());
        address.setUser(user);

        boolean hasPrimary =
                addressRepository.existsByUserIdAndIsPrimaryTrue(userId);

        if (Boolean.TRUE.equals(req.getIsPrimary())) {
            log.info("Setting this address as primary for userId={}", userId);
            addressRepository.clearPrimary(userId);
            address.setIsPrimary(true);
        } else if (!hasPrimary) {
            log.info("No primary address exists. Setting this as primary for userId={}", userId);
            address.setIsPrimary(true);
        } else {
            address.setIsPrimary(false);
        }

        Address saved = addressRepository.save(address);
        log.info("Address saved successfully. addressId={}, userId={}", saved.getId(), userId);

        return mapToResponse(saved);
    }


    public Page<AddressResponse> getMyAddresses(int page,int size) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("Unauthorized attempt to fetch addresses");
                    return new RuntimeException("User not authenticated");
                });

        log.info("Fetching addresses for userId={}", userId);

//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> {
//                    log.warn("User not found for userId={}", userId);
//                    return new ResourceNotFoundException("User not found with id: " + userId);
//                });
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Address> addressPage =
                addressRepository.findByUserId(userId, pageable);

        if (addressPage.isEmpty()) {
            log.info("No addresses found for userId={}", userId);
        } else {
            log.info("Found {} addresses for userId={}", addressPage.getTotalElements(), userId);
        }

        return addressPage.map(this::mapToResponse);
    }


    public AddressResponse updateAddress(Long addressId, SaveAddressRequest req) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("Unauthorized attempt to update address");
                    return new RuntimeException("User not authenticated");
                });

        log.info("Updating addressId={} for userId={}", addressId, userId);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> {
                    log.warn("Address not found. addressId={}", addressId);
                    return new ResourceNotFoundException("Address not found with id: " + addressId);
                });

        if (!address.getUser().getId().equals(userId)) {
            log.warn("UserId={} tried to update addressId={} not owned by them", userId, addressId);
            throw new RuntimeException("You cannot update this address");
        }

        LocationResponse location =
                pincodeService.getByPincode(req.getPinCode());

        address.setAddressType(req.getAddressType());
        address.setAlternatePhoneNumber(req.getAlternatePhoneNumber());
        address.setHouseNumber(req.getHouseNumber());
        address.setApartmentName(req.getApartmentName());
        address.setStreetName(req.getStreetName());
        address.setLandMark(req.getLandMark());
        address.setCity(location.getCity());
        address.setState(location.getState());
        address.setPinCode(req.getPinCode());
        address.setLatitude(req.getLatitude());
        address.setLongitude(req.getLongitude());

        if (Boolean.TRUE.equals(req.getIsPrimary())) {
            log.info("Setting addressId={} as primary for userId={}", addressId, userId);
            addressRepository.clearPrimary(userId);
            address.setIsPrimary(true);
        }

        Address updated = addressRepository.save(address);
        log.info("Address updated successfully. addressId={}, userId={}", updated.getId(), userId);

        return mapToResponse(updated);
    }


    public void deleteAddress(Long addressId) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("Unauthorized attempt to delete address");
                    return new RuntimeException("User not authenticated");
                });

        log.info("Deleting addressId={} for userId={}", addressId, userId);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> {
                    log.warn("Address not found. addressId={}", addressId);
                    return new ResourceNotFoundException("Address not found with id: " + addressId);
                });

        if (!address.getUser().getId().equals(userId)) {
            log.warn("UserId={} tried to delete addressId={} not owned by them", userId, addressId);
            throw new RuntimeException("You cannot delete this address");
        }

        boolean wasPrimary = Boolean.TRUE.equals(address.getIsPrimary());
        addressRepository.delete(address);

        log.info("Address deleted successfully. addressId={}, userId={}", addressId, userId);

        if (wasPrimary) {
            log.info("Deleted address was primary. Assigning new primary for userId={}", userId);
            addressRepository.findFirstByUserIdOrderByIdAsc(userId)
                    .ifPresent(a -> {
                        a.setIsPrimary(true);
                        addressRepository.save(a);
                        log.info("New primary address set. addressId={}, userId={}", a.getId(), userId);
                    });
        }
    }


    private AddressResponse mapToResponse(Address address) {

        AddressResponse ar = new AddressResponse();
        ar.setId(address.getId());
        ar.setAddressType(address.getAddressType());
        ar.setHouseNumber(address.getHouseNumber());
        ar.setApartmentName(address.getApartmentName());
        ar.setStreetName(address.getStreetName());
        ar.setLandMark(address.getLandMark());
        ar.setCity(address.getCity());
        ar.setState(address.getState());
        ar.setPinCode(address.getPinCode());
        ar.setAlternatePhoneNumber(address.getAlternatePhoneNumber());
        ar.setLatitude(address.getLatitude());
        ar.setLongitude(address.getLongitude());
        ar.setIsPrimary(address.getIsPrimary());

        return ar;
    }
}
