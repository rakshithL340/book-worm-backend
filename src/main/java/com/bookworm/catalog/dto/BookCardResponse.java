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
public class BookCardResponse {

    private UUID bookId;
    private String title;
    private String authorName;
    private UUID authorId;
    private BigDecimal price;
    private String format;
    private String categoryName;
    private String coverImageUrl;
    private BigDecimal averageRating;
    private LocalDate tentativeDeliveryDate;
}
