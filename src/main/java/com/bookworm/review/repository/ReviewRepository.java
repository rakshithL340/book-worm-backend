package com.bookworm.review.repository;

import com.bookworm.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByBookBookIdOrderByCreatedAtDesc(UUID bookId, Pageable pageable);

    boolean existsByBookBookIdAndUserUserId(UUID bookId, UUID userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.bookId = :bookId")
    Optional<Double> calculateAverageRatingByBookId(@Param("bookId") UUID bookId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.bookId = :bookId")
    long countReviewsByBookId(@Param("bookId") UUID bookId);
}
