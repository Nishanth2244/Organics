package com.organics.products.respository;

import com.organics.products.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
//    List<Address> findByCustomerId(Long customerId);
//    List<Address> findByPhoneNumber(Long phoneNumber);
//    List<Address> findByCityAndState(String city, String state);
//    
//    @Query("SELECT a FROM Address a WHERE a.customer.id = :customerId AND a.isPrimary = true")
//    Optional<Address> findPrimaryAddressByCustomerId(@Param("customerId") Long customerId);
//    
//    void deleteByCustomerId(Long customerId);
}