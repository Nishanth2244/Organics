package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.AddressResponse;
import com.organics.products.dto.UserDTO;
import com.organics.products.dto.UserProfileResponse;
import com.organics.products.entity.Address;
import com.organics.products.entity.User;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public UserProfileResponse getMyProfile() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("Unauthorized access attempt to get profile");
                    return new RuntimeException("User not authenticated");
                });

        log.info("Fetching profile for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setEmailId(user.getEmailId());
        response.setDisplayName(user.getDisplayName());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setGender(user.getGender());
        response.setDateOfBirth(user.getDateOfBirth());

        if (user.getAddresses() == null || user.getAddresses().isEmpty()) {
            log.info("No addresses found for userId={}", userId);
            response.setAddresses(List.of());
        } else {
            log.info("Found {} addresses for userId={}", user.getAddresses().size(), userId);
            response.setAddresses(
                    user.getAddresses()
                            .stream()
                            .map(this::mapToAddressResponse)
                            .collect(Collectors.toList())
            );
        }

        log.info("Profile fetched successfully for userId={}", userId);
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

    // =========================
    // Update Display Name
    // =========================
    public void updateDisplayName(String displayName) {

        if (displayName == null || displayName.trim().isEmpty()) {
            log.warn("Attempt to update display name with empty value");
            throw new RuntimeException("Display name cannot be empty");
        }

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("Unauthorized access attempt to update display name");
                    return new RuntimeException("User not authenticated");
                });

        log.info("Updating display name for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        String oldName = user.getDisplayName();
        user.setDisplayName(displayName.trim());
        userRepository.save(user);

        log.info("Display name updated for userId={}, old='{}', new='{}'",
                userId, oldName, displayName.trim());
    }


    public List<UserDTO> getAllUsers() {

        log.info("Fetching all users");

        List<User> users = userRepository.findAll();

        if (users == null || users.isEmpty()) {
            log.info("No users found in database");
            return List.of();
        }

        log.info("Found {} users", users.size());

        return users.stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(user.getId());
                    dto.setDisplayName(user.getDisplayName());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setPhoneNumber(user.getPhoneNumber());
                    dto.setEmailId(user.getEmailId());
                    dto.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
                    return dto;
                })
                .toList();
    }


    public UserDTO getMyDisplayName() {

        Long userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("Unauthorized access attempt to get display name");
                    return new RuntimeException("User not authenticated");
                });

        log.info("Fetching display name for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setDisplayName(user.getDisplayName() != null ? user.getDisplayName() : "");

        log.info("Display name fetched for userId={}, displayName='{}'",
                userId, dto.getDisplayName());

        return dto;
    }
}
