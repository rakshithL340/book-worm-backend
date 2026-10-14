package com.bookworm.promotion.entity;

import com.bookworm.common.model.BaseEntity;
import com.bookworm.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loyalty_accounts")
@SQLDelete(sql = "UPDATE loyalty_accounts SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE loyalty_account_id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class LoyaltyAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "loyalty_account_id", updatable = false, nullable = false)
    private UUID loyaltyAccountId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "points_balance", nullable = false)
    @Builder.Default
    private Integer pointsBalance = 0;

    @OneToMany(mappedBy = "loyaltyAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoyaltyTransaction> transactions = new ArrayList<>();

    public void addTransaction(LoyaltyTransaction transaction) {
        transactions.add(transaction);
        transaction.setLoyaltyAccount(this);
    }
}
