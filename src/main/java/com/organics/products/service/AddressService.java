package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.AddressResponse;
import com.organics.products.dto.LocationResponse;
import com.organics.products.dto.SaveAddressRequest;
import com.organics.products.entity.Address;
import com.organics.products.entity.User;
import com.organics.products.respository.AddressRepository;
import com.organics.products.respository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
            addressRepository.clearPrimary(userId);
            address.setIsPrimary(true);
        } else if (!hasPrimary) {
            address.setIsPrimary(true);
        } else {
            address.setIsPrimary(false);
        }

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }


    public List<AddressResponse> getMyAddresses() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getAddresses()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    public AddressResponse updateAddress(Long addressId, SaveAddressRequest req) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
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
            addressRepository.clearPrimary(userId);
            address.setIsPrimary(true);
        }

        Address updated = addressRepository.save(address);
        return mapToResponse(updated);
    }


    public void deleteAddress(Long addressId) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You cannot delete this address");
        }

        boolean wasPrimary = Boolean.TRUE.equals(address.getIsPrimary());
        addressRepository.delete(address);

        if (wasPrimary) {
            addressRepository.findFirstByUserIdOrderByIdAsc(userId)
                    .ifPresent(a -> {
                        a.setIsPrimary(true);
                        addressRepository.save(a);
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
