package com.bookworm.review.service;

import com.bookworm.common.dto.PageResponse;
import com.bookworm.review.dto.ReviewCreateRequest;
import com.bookworm.review.dto.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse addReview(UUID userId, UUID bookId, ReviewCreateRequest request);

    PageResponse<ReviewResponse> getReviewsForBook(UUID bookId, Pageable pageable);
}
