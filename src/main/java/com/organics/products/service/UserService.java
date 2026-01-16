package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.AddressResponse;
import com.organics.products.dto.UserDTO;
import com.organics.products.dto.UserProfileResponse;
import com.organics.products.entity.Address;
import com.organics.products.entity.User;
import com.organics.products.respository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public UserProfileResponse getMyProfile() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setEmailId(user.getEmailId());
        response.setDisplayName(user.getDisplayName());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setGender(user.getGender());
        response.setDateOfBirth(user.getDateOfBirth());

        response.setAddresses(
                user.getAddresses()
                        .stream()
                        .map(this::mapToAddressResponse)
                        .collect(Collectors.toList())
        );

        return response;
    }

    private AddressResponse mapToAddressResponse(Address a) {

        AddressResponse ar = new AddressResponse();
        ar.setId(a.getId());
        ar.setAddressType(a.getAddressType());
        ar.setHouseNumber(a.getHouseNumber());
        ar.setApartmentName(a.getApartmentName());
        ar.setStreetName(a.getStreetName());
        ar.setLandMark(a.getLandMark());
        ar.setCity(a.getCity());
        ar.setState(a.getState());
        ar.setPinCode(a.getPinCode());
        ar.setAlternatePhoneNumber(a.getAlternatePhoneNumber());
        ar.setLatitude(a.getLatitude());
        ar.setLongitude(a.getLongitude());
        ar.setIsPrimary(a.getIsPrimary());

        return ar;
    }

    public void updateDisplayName(String displayName) {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDisplayName(displayName);
        userRepository.save(user);
    }

    public List<UserDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(user.getId());
                    dto.setDisplayName(user.getDisplayName());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setPhoneNumber(user.getPhoneNumber());
                    dto.setEmailId(user.getEmailId());
                    dto.setStatus(user.getStatus().name());
                    return dto;
                })
                .toList();
    }




    public UserDTO getMyDisplayName() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setDisplayName(user.getDisplayName());

        return dto;
    }

}
