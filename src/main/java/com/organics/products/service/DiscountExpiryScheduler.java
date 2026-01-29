package com.organics.products.service;

import com.organics.products.entity.Discount;
import com.organics.products.respository.DiscountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
//@Component
public class DiscountExpiryScheduler {

    private final DiscountRepository discountRepository;

    public DiscountExpiryScheduler(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireDiscounts() {

        List<Discount> expired =
                discountRepository.findByActiveTrueAndValidToBefore(LocalDateTime.now());

        if (expired.isEmpty()) {
            log.info("No expired discounts found");
            return;
        }

        for (Discount discount : expired) {
            discount.setActive(false);
        }

        discountRepository.saveAll(expired);

        log.info("Expired {} discounts", expired.size());
    }
}
