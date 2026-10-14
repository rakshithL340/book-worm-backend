package com.bookworm.review.service;

import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.common.dto.PageResponse;
import com.bookworm.common.exception.BadRequestException;
import com.bookworm.common.exception.ConflictException;
import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.review.dto.ReviewCreateRequest;
import com.bookworm.review.dto.ReviewResponse;
import com.bookworm.review.entity.Review;
import com.bookworm.review.mapper.ReviewMapper;
import com.bookworm.review.repository.ReviewRepository;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponse addReview(UUID userId, UUID bookId, ReviewCreateRequest request) {
        log.info("Adding review for book: {} by user: {}", bookId, userId);

        // Strict Business Constraint: 100-character max review limit
        if (request.getReviewText() != null && request.getReviewText().length() > 100) {
            throw new BadRequestException("Review comment must not exceed 100 characters. Provided: " +
                    request.getReviewText().length() + " characters.");
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "bookId", bookId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        if (reviewRepository.existsByBookBookIdAndUserUserId(bookId, userId)) {
            throw new ConflictException("You have already reviewed this book");
        }

        String reviewerName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName().charAt(0) + "." : "");

        Review review = Review.builder()
                .book(book)
                .user(user)
                .reviewerName(reviewerName.trim().isEmpty() ? "Verified Reader" : reviewerName.trim())
                .rating(request.getRating())
                .reviewText(request.getReviewText().trim())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Recalculate book average rating and total review count
        long totalReviews = reviewRepository.countReviewsByBookId(bookId);
        Double avgRating = reviewRepository.calculateAverageRatingByBookId(bookId).orElse((double) request.getRating());

        book.setReviewCount((int) totalReviews);
        book.setAverageRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        bookRepository.save(book);

        log.info("Saved review ID: {} for book ID: {}. Updated average rating: {}",
                savedReview.getReviewId(), bookId, book.getAverageRating());

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsForBook(UUID bookId, Pageable pageable) {
        log.info("Fetching reviews for book: {}", bookId);
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book", "bookId", bookId);
        }

        Page<Review> reviewPage = reviewRepository.findByBookBookIdOrderByCreatedAtDesc(bookId, pageable);
        List<ReviewResponse> content = reviewMapper.toResponseList(reviewPage.getContent());
        return PageResponse.of(content, reviewPage);
    }
}
