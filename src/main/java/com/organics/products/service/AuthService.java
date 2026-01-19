package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.TokenPair;
import com.organics.products.entity.Admin;
import com.organics.products.entity.OtpStore;
import com.organics.products.entity.RefreshToken;
import com.organics.products.entity.User;
import com.organics.products.respository.AdminRepository;
import com.organics.products.respository.OtpRepository;
import com.organics.products.respository.RefreshTokenRepository;
import com.organics.products.respository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final OtpRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            AdminRepository adminRepository,
            OtpRepository otpRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            SmsService smsService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.otpRepository = otpRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.smsService = smsService;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendOtp(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new RuntimeException("Phone number is required");
        }

        otpRepository.deleteByPhoneNumber(phoneNumber);

        String otp = generateOtp();

        OtpStore otpStore = new OtpStore();
        otpStore.setPhoneNumber(phoneNumber);
        otpStore.setOtp(otp);
        otpStore.setExpiryTime(LocalDateTime.now().plusMinutes(3));
        otpStore.setVerified(false);

        otpRepository.save(otpStore);

        String message = "Your JJR Organics OTP is " + otp + ". It is valid for 3 minutes.";
        smsService.sendOtpSms(phoneNumber, message);
    }

    public TokenPair verifyOtp(String phoneNumber, String otp) {
        OtpStore store = otpRepository
                .findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .orElseThrow(() -> new RuntimeException("OTP not sent"));

        if (store.isVerified()) {
            throw new RuntimeException("OTP already used");
        }

        if (store.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!store.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        store.setVerified(true);
        otpRepository.save(store);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    User u = new User();
                    u.setPhoneNumber(phoneNumber);
                    return userRepository.save(u);
                });

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // REMOVED: refreshTokenRepository.deleteByUserId(user.getId());
        // Removing this line allows the user to stay logged in on multiple devices indefinitely.

        return issueUserTokens(user);
    }

    public TokenPair adminLogin(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Email and password are required");
        }

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        // REMOVED: refreshTokenRepository.deleteByAdminId(admin.getId());

        return issueAdminTokens(admin);
    }

    public TokenPair refresh(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        if (token.getUser() != null) {
            return issueUserTokens(token.getUser());
        }

        return issueAdminTokens(token.getAdmin());
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    private TokenPair issueUserTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), "USER");
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setUser(user);
        // Set expiry to 10 years to effectively keep the user logged in until manual logout
        token.setExpiryTime(LocalDateTime.now().plusYears(10));

        refreshTokenRepository.save(token);

        return new TokenPair(accessToken, refreshToken);
    }

    private TokenPair issueAdminTokens(Admin admin) {
        String accessToken = jwtService.generateAccessToken(admin.getId(), "ADMIN");
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setAdmin(admin);
        // Set expiry to 10 years to effectively keep the admin logged in until manual logout
        token.setExpiryTime(LocalDateTime.now().plusYears(10));

        refreshTokenRepository.save(token);

        return new TokenPair(accessToken, refreshToken);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    public void changePassword(String oldPassword, String newPassword) {
        Long adminId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Admin not authenticated"));

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        refreshTokenRepository.deleteByAdminId(admin.getId());
    }

    public void sendAdminForgotPasswordOtp(String email, String phoneNumber) {
        if ((email == null && phoneNumber == null) || (email != null && phoneNumber != null)) {
            throw new RuntimeException("Provide either email or phone number");
        }

        Admin admin;
        String otp = generateOtp();

        if (email != null) {
            admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            otpRepository.deleteByPhoneNumber(admin.getPhoneNumber());
        } else {
            admin = adminRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            otpRepository.deleteByPhoneNumber(phoneNumber);
        }

        OtpStore store = new OtpStore();
        store.setPhoneNumber(admin.getPhoneNumber());
        store.setOtp(otp);
        store.setExpiryTime(LocalDateTime.now().plusMinutes(3));
        store.setVerified(false);
        otpRepository.save(store);

        if (email != null) {
            smsService.sendOtpEmail("arn:aws:sns:ap-southeast-1:xxxx:admin-forgot-password-topic", otp);
        } else {
            smsService.sendOtpSms(phoneNumber, "Admin Password Reset OTP: " + otp + ". Valid for 3 minutes.");
        }
    }

    public void resetAdminPassword(String email, String phoneNumber, String otp, String newPassword) {
        if ((email == null && phoneNumber == null) || (email != null && phoneNumber != null)) {
            throw new RuntimeException("Provide either email or phone number");
        }

        Admin admin = (email != null)
                ? adminRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Admin not found"))
                : adminRepository.findByPhoneNumber(phoneNumber).orElseThrow(() -> new RuntimeException("Admin not found"));

        OtpStore store = otpRepository
                .findTopByPhoneNumberOrderByCreatedAtDesc(admin.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("OTP not sent"));

        if (store.isVerified() || store.getExpiryTime().isBefore(LocalDateTime.now()) || !store.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        store.setVerified(true);
        otpRepository.save(store);

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        refreshTokenRepository.deleteByAdminId(admin.getId());
    }
}