package com.organics.products.respository;

import com.organics.products.entity.OtpStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface OtpRepository extends JpaRepository<OtpStore, Long> {


    void deleteByPhoneNumber(String phoneNumber);

    Optional<OtpStore> findTopByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}

