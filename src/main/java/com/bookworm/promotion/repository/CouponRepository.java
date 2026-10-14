package com.bookworm.promotion.repository;

import com.bookworm.promotion.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCaseAndActiveTrue(String code);

    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code) " +
           "AND c.active = true AND c.validFrom <= :now AND c.validUntil >= :now")
    Optional<Coupon> findValidCoupon(@Param("code") String code, @Param("now") Instant now);

    boolean existsByCodeIgnoreCase(String code);
}
