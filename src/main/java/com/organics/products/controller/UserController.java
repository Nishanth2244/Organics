package com.organics.products.controller;

import com.organics.products.dto.UserDTO;
import com.organics.products.dto.UserDisplayNameRequest;
import com.organics.products.dto.UserProfileResponse;
import com.organics.products.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.getMyProfile();
    }

    @PutMapping("/display-name")
    public void updateDisplayName(@RequestBody UserDTO request) {
        userService.updateDisplayName(request.getDisplayName());
    }
    @GetMapping("/getAllUsers")
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }



}
