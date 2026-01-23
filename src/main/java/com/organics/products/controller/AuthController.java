package com.organics.products.controller;

import com.organics.products.dto.CreateAdminRequest;
import com.organics.products.dto.TokenPair;
import com.organics.products.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiryMs;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestParam String mobileNumber) {

        authService.sendOtp(mobileNumber);

        return ResponseEntity.ok(
                Map.of("message", "OTP sent successfully")
        );
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(
            @RequestParam String mobileNumber,
            @RequestParam String otp,
            HttpServletResponse response
    ) {
        TokenPair tokenPair = authService.verifyOtp(mobileNumber, otp);

        addRefreshTokenCookie(response, tokenPair.getRefreshToken());

        return ResponseEntity.ok(
                Map.of("accessToken", tokenPair.getAccessToken())
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {

        String refreshToken = extractRefreshToken(request);

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token missing");
        }

        TokenPair tokenPair = authService.refresh(refreshToken);

        return ResponseEntity.ok(
                Map.of("accessToken", tokenPair.getAccessToken())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshToken(request);

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully")
        );
    }


    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));

        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    private String extractRefreshToken(HttpServletRequest request) {

        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletResponse response
    ) {
        TokenPair tokenPair = authService.adminLogin(email, password);

        Cookie cookie = new Cookie("refreshToken", tokenPair.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // true in PROD (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));

        response.addCookie(cookie);

        return ResponseEntity.ok(
                Map.of("accessToken", tokenPair.getAccessToken())
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/admin/change-password")
    public ResponseEntity<?> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        authService.changePassword(oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/admin/forgot-password/otp")
        public ResponseEntity<?> sendOtp(
                @RequestParam(required = false) String email,
                @RequestParam(required = false) String phoneNumber
) {
            authService.sendAdminForgotPasswordOtp(email, phoneNumber);
            return ResponseEntity.ok("OTP sent successfully");
        }


    @PostMapping("/admin/forgot-password/reset")
    public ResponseEntity<?> resetPassword(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam String otp,
            @RequestParam String newPassword
    ) {
        authService.resetAdminPassword(email, phoneNumber, otp, newPassword);
        return ResponseEntity.ok(
                Map.of("message", "Admin password reset successful")
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest request) {

        authService.createAdmin(request);

        return ResponseEntity.ok(
                Map.of("message", "Admin created successfully")
        );
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<Page<?>> getAllAdmins(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size ) {

        return ResponseEntity.ok(authService.getAllAdmins(page,size));
    }

}