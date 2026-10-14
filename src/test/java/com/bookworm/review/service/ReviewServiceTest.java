package com.bookworm.review.service;

import com.bookworm.catalog.entity.Book;
import com.bookworm.catalog.repository.BookRepository;
import com.bookworm.common.exception.BadRequestException;
import com.bookworm.common.exception.ConflictException;
import com.bookworm.review.dto.ReviewCreateRequest;
import com.bookworm.review.dto.ReviewResponse;
import com.bookworm.review.entity.Review;
import com.bookworm.review.mapper.ReviewMapper;
import com.bookworm.review.repository.ReviewRepository;
import com.bookworm.user.entity.User;
import com.bookworm.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Book testBook;
    private Review testReview;
    private ReviewResponse testReviewResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        testUser = User.builder()
                .userId(userId)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .build();

        testBook = Book.builder()
                .bookId(bookId)
                .title("Atomic Habits")
                .averageRating(new BigDecimal("4.50"))
                .reviewCount(10)
                .build();

        testReview = Review.builder()
                .reviewId(reviewId)
                .book(testBook)
                .user(testUser)
                .reviewerName("Alice S.")
                .rating(5)
                .reviewText("Outstanding book! Truly life changing habits.")
                .build();

        testReviewResponse = ReviewResponse.builder()
                .reviewId(reviewId)
                .reviewerName("Alice S.")
                .rating(5)
                .reviewText("Outstanding book! Truly life changing habits.")
                .build();
    }

    @Test
    @DisplayName("Should successfully add a review within 100 character limit and update book rating")
    void shouldAddReviewSuccessfullyWithin100Characters() {
        // Arrange
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .reviewText("Outstanding book! Truly life changing habits.") // 46 chars <= 100
                .build();

        when(bookRepository.findById(testBook.getBookId())).thenReturn(Optional.of(testBook));
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(reviewRepository.existsByBookBookIdAndUserUserId(testBook.getBookId(), testUser.getUserId())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewRepository.countReviewsByBookId(testBook.getBookId())).thenReturn(11L);
        when(reviewRepository.calculateAverageRatingByBookId(testBook.getBookId())).thenReturn(Optional.of(4.80));
        when(reviewMapper.toResponse(testReview)).thenReturn(testReviewResponse);

        // Act
        ReviewResponse response = reviewService.addReview(testUser.getUserId(), testBook.getBookId(), request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(testBook.getReviewCount()).isEqualTo(11);
        assertThat(testBook.getAverageRating()).isEqualByComparingTo("4.80");

        verify(bookRepository).save(testBook);
    }

    @Test
    @DisplayName("Should throw BadRequestException when review exceeds strict 100-character constraint")
    void shouldThrowBadRequestWhenReviewExceeds100Characters() {
        // Arrange
        String longComment = "This is a very long review comment that definitely exceeds the strict maximum limit of one hundred characters as enforced by PDP wireframe."; // 142 chars

        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .reviewText(longComment)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> reviewService.addReview(testUser.getUserId(), testBook.getBookId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not exceed 100 characters");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when user tries to review the same book twice")
    void shouldThrowConflictWhenUserAlreadyReviewed() {
        // Arrange
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(4)
                .reviewText("Great read!")
                .build();

        when(bookRepository.findById(testBook.getBookId())).thenReturn(Optional.of(testBook));
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(reviewRepository.existsByBookBookIdAndUserUserId(testBook.getBookId(), testUser.getUserId())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.addReview(testUser.getUserId(), testBook.getBookId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reviewed");
    }
}
