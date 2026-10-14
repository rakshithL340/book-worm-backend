package com.bookworm.store.entity;

import com.bookworm.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_policies")
@SQLDelete(sql = "UPDATE store_policies SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE policy_id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class StorePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id", updatable = false, nullable = false)
    private UUID policyId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(name = "cancellation_window_hours", nullable = false)
    @Builder.Default
    private Integer cancellationWindowHours = 48;

    @Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRatePercent = new BigDecimal("12.00");

    @Column(name = "free_delivery_threshold", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal freeDeliveryThreshold = new BigDecimal("500.00");

    @Column(name = "standard_delivery_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal standardDeliveryFee = new BigDecimal("50.00");

    @Column(name = "loyalty_redemption_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal loyaltyRedemptionRate = new BigDecimal("1.00");

    @Column(name = "return_window_days", nullable = false)
    @Builder.Default
    private Integer returnWindowDays = 7;
}
