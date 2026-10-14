package com.bookworm.review.controller;

import com.bookworm.common.dto.PageResponse;
import com.bookworm.common.security.JwtProvider;
import com.bookworm.common.security.SecurityUtils;
import com.bookworm.review.dto.ReviewCreateRequest;
import com.bookworm.review.dto.ReviewResponse;
import com.bookworm.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReviewController REST Tests")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("POST /books/{bookId}/reviews - Should submit review within 100 character limit")
    void shouldCreateReviewWithin100Characters() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .reviewText("One of the best books on building habits!") // 42 chars
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .reviewId(UUID.randomUUID())
                .reviewerName("John D.")
                .rating(5)
                .reviewText("One of the best books on building habits!")
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(reviewService.addReview(eq(userId), eq(bookId), any(ReviewCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/books/" + bookId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewerName").value("John D."));
    }

    @Test
    @DisplayName("POST /books/{bookId}/reviews - Should return 400 Bad Request when review exceeds 100 characters")
    void shouldReturnBadRequestWhenReviewExceeds100Characters() throws Exception {
        UUID bookId = UUID.randomUUID();
        String overLimitText = "This review text is deliberately made longer than one hundred characters in total length to ensure bean validation rejects it.";

        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .reviewText(overLimitText)
                .build();

        mockMvc.perform(post("/books/" + bookId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /books/{bookId}/reviews - Should return paginated reviews")
    void shouldListReviewsForBook() throws Exception {
        UUID bookId = UUID.randomUUID();
        PageResponse<ReviewResponse> pageResponse = PageResponse.<ReviewResponse>builder()
                .content(List.of(ReviewResponse.builder().rating(5).reviewText("Great!").build()))
                .totalElements(1)
                .page(0)
                .size(10)
                .build();

        when(reviewService.getReviewsForBook(eq(bookId), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/books/" + bookId + "/reviews?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
