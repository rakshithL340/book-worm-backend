package com.bookworm.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailResponse {

    private UUID bookId;
    private String title;
    private String isbn;
    private AuthorSummaryResponse author;
    private PublisherResponse publisher;
    private CategoryResponse category;
    private String format;
    private BigDecimal price;
    private String language;
    private String description;
    private Long salesCount;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private Integer stockQuantity;
    private String coverImageUrl;
    private LocalDate tentativeDeliveryDate;
}
