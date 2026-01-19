package com.organics.products.respository;

import com.organics.products.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    // List<Banner> findAllByOrderByPriorityAsc();
}
