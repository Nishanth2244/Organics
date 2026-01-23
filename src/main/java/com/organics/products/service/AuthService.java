package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.AdminResponseDTO;
import com.organics.products.dto.CreateAdminRequest;
import com.organics.products.dto.TokenPair;
import com.organics.products.entity.Admin;
import com.organics.products.entity.OtpStore;
import com.organics.products.entity.RefreshToken;
import com.organics.products.entity.User;
import com.organics.products.exception.ResourceNotFoundException;
import com.organics.products.respository.AdminRepository;
import com.organics.products.respository.OtpRepository;
import com.organics.products.respository.RefreshTokenRepository;
import com.organics.products.respository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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
            log.warn("sendOtp called with empty phone number");
            throw new RuntimeException("Phone number is required");
        }

        log.info("Sending OTP to phoneNumber={}", phoneNumber);

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

        log.info("OTP sent successfully to phoneNumber={}", phoneNumber);
    }


    public TokenPair verifyOtp(String phoneNumber, String otp) {

        log.info("Verifying OTP for phoneNumber={}", phoneNumber);

        OtpStore store = otpRepository
                .findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .orElseThrow(() -> {
                    log.warn("OTP not sent for phoneNumber={}", phoneNumber);
                    return new RuntimeException("OTP not sent");
                });

        if (store.isVerified()) {
            log.warn("OTP already used for phoneNumber={}", phoneNumber);
            throw new RuntimeException("OTP already used");
        }

        if (store.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for phoneNumber={}", phoneNumber);
            throw new RuntimeException("OTP expired");
        }

        if (!store.getOtp().equals(otp)) {
            log.warn("Invalid OTP for phoneNumber={}", phoneNumber);
            throw new RuntimeException("Invalid OTP");
        }

        store.setVerified(true);
        otpRepository.save(store);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    log.info("New user registration for phoneNumber={}", phoneNumber);
                    User u = new User();
                    u.setPhoneNumber(phoneNumber);
                    return userRepository.save(u);
                });

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("OTP verified successfully. Issuing tokens for userId={}", user.getId());

        return issueUserTokens(user);
    }


    public TokenPair adminLogin(String email, String password) {

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("adminLogin called with empty email or password");
            throw new RuntimeException("Email and password are required");
        }

        log.info("Admin login attempt for email={}", email);

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Admin not found for email={}", email);
                    return new ResourceNotFoundException("Admin not found");
                });

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            log.warn("Invalid credentials for admin email={}", email);
            throw new RuntimeException("Invalid credentials");
        }

        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        log.info("Admin login successful. Issuing tokens for adminId={}", admin.getId());

        return issueAdminTokens(admin);
    }


    public TokenPair refresh(String refreshToken) {

        log.info("Refreshing token");

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Invalid refresh token");
                    return new RuntimeException("Invalid refresh token");
                });

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token expired");
            throw new RuntimeException("Refresh token expired");
        }

        if (token.getUser() != null) {
            log.info("Issuing new tokens for userId={}", token.getUser().getId());
            return issueUserTokens(token.getUser());
        }

        log.info("Issuing new tokens for adminId={}", token.getAdmin().getId());
        return issueAdminTokens(token.getAdmin());
    }


    public void logout(String refreshToken) {

        log.info("Logout request");

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                    log.info("Refresh token deleted successfully");
                });
    }


    private TokenPair issueUserTokens(User user) {

        log.info("Issuing tokens for userId={}", user.getId());

        String accessToken = jwtService.generateAccessToken(user.getId(), "USER");
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setUser(user);
        token.setExpiryTime(LocalDateTime.now().plusYears(10));

        refreshTokenRepository.save(token);

        return new TokenPair(accessToken, refreshToken);
    }

    private TokenPair issueAdminTokens(Admin admin) {

        log.info("Issuing tokens for adminId={}", admin.getId());

        String accessToken = jwtService.generateAccessToken(admin.getId(), "ADMIN");
        String refreshToken = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setAdmin(admin);
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
                .orElseThrow(() -> {
                    log.warn("Unauthorized password change attempt");
                    return new RuntimeException("Admin not authenticated");
                });

        log.info("Changing password for adminId={}", adminId);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.warn("Admin not found for adminId={}", adminId);
                    return new ResourceNotFoundException("Admin not found");
                });

        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            log.warn("Old password incorrect for adminId={}", adminId);
            throw new RuntimeException("Old password is incorrect");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        refreshTokenRepository.deleteByAdminId(admin.getId());

        log.info("Password changed successfully for adminId={}", adminId);
    }


    public void sendAdminForgotPasswordOtp(String email, String phoneNumber) {

        if ((email == null && phoneNumber == null) || (email != null && phoneNumber != null)) {
            log.warn("Invalid forgot password request. email={}, phoneNumber={}", email, phoneNumber);
            throw new RuntimeException("Provide either email or phone number");
        }

        Admin admin;
        String otp = generateOtp();

        if (email != null) {
            log.info("Sending forgot password OTP via email for {}", email);
            admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
            otpRepository.deleteByPhoneNumber(admin.getPhoneNumber());
        } else {
            log.info("Sending forgot password OTP via SMS for {}", phoneNumber);
            admin = adminRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
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

        log.info("Forgot password OTP sent successfully for adminId={}", admin.getId());
    }


    public void resetAdminPassword(String email, String phoneNumber, String otp, String newPassword) {

        if ((email == null && phoneNumber == null) || (email != null && phoneNumber != null)) {
            log.warn("Invalid reset password request. email={}, phoneNumber={}", email, phoneNumber);
            throw new RuntimeException("Provide either email or phone number");
        }

        Admin admin = (email != null)
                ? adminRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Admin not found"))
                : adminRepository.findByPhoneNumber(phoneNumber).orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        log.info("Resetting password for adminId={}", admin.getId());

        OtpStore store = otpRepository
                .findTopByPhoneNumberOrderByCreatedAtDesc(admin.getPhoneNumber())
                .orElseThrow(() -> {
                    log.warn("OTP not sent for admin phone={}", admin.getPhoneNumber());
                    return new RuntimeException("OTP not sent");
                });

        if (store.isVerified()) {
            log.warn("OTP already used for adminId={}", admin.getId());
            throw new RuntimeException("OTP already used");
        }

        if (store.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for adminId={}", admin.getId());
            throw new RuntimeException("OTP expired");
        }

        if (!store.getOtp().equals(otp)) {
            log.warn("Invalid OTP for adminId={}", admin.getId());
            throw new RuntimeException("Invalid OTP");
        }

        store.setVerified(true);
        otpRepository.save(store);

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        refreshTokenRepository.deleteByAdminId(admin.getId());

        log.info("Admin password reset successfully for adminId={}", admin.getId());
    }

    public void createAdmin(CreateAdminRequest request) {

        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Admin already exists with this email");
        }

        Admin admin = new Admin();
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setPhoneNumber(request.getPhoneNumber());

        adminRepository.save(admin);
    }

    public Page<AdminResponseDTO> getAllAdmins(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Admin> adminPage = adminRepository.findAll(pageable);

        return adminPage.map(admin -> {
            AdminResponseDTO dto = new AdminResponseDTO();
            dto.setId(admin.getId());
            dto.setEmail(admin.getEmail());
            dto.setPhoneNumber(admin.getPhoneNumber());
            return dto;
        });
    }

}
