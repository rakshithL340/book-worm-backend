package com.bookworm.store.entity;

import com.bookworm.common.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stores")
@SQLDelete(sql = "UPDATE stores SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE store_id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id", updatable = false, nullable = false)
    private UUID storeId;

    @Column(name = "store_code", nullable = false, unique = true, length = 50)
    private String storeCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "INR";

    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private String locale = "en_IN";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private StorePolicy storePolicy;
}
