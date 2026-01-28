package com.organics.products.respository;

import com.organics.products.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);


    @Query("SELECT u.expoPushToken FROM User u WHERE u.expoPushToken IS NOT NULL AND u.expoPushToken <> ''")
    List<String> findAllPushTokens();
}

