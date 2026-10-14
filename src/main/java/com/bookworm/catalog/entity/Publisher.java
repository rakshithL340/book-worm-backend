package com.bookworm.catalog.entity;

import com.bookworm.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@Table(name = "publishers")
@SQLDelete(sql = "UPDATE publishers SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE publisher_id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
public class Publisher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "publisher_id", updatable = false, nullable = false)
    private UUID publisherId;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;
}
