package com.organics.products.respository;

import com.organics.products.entity.Customer;
import com.organics.products.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhoneNumber(Long phoneNumber);
    Optional<Customer> findByEmailId(String emailId);
    List<Customer> findByDisplayNameContainingIgnoreCase(String displayName);
    boolean existsByPhoneNumber(Long phoneNumber);
    boolean existsByEmailId(String emailId);
    
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.addresses WHERE c.id = :id")
    Optional<Customer> findByIdWithAddresses(@Param("id") Long id);
}