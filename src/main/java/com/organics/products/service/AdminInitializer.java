package com.organics.products.service;

import com.organics.products.entity.Admin;
import com.organics.products.respository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

//@Configuration
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!adminRepository.existsByEmail("admin@organics.com")) {

            Admin admin = new Admin();
            admin.setEmail("admin@organics.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setPhoneNumber("+917097997221");
            adminRepository.save(admin);

            System.out.println("DEFAULT ADMIN CREATED");
        }
    }
}
