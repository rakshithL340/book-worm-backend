package com.bookworm.promotion.repository;

import com.bookworm.promotion.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    List<CouponRedemption> findByUserUserIdAndCouponCouponId(UUID userId, UUID couponId);

    long countByUserUserIdAndCouponCouponId(UUID userId, UUID couponId);
}
