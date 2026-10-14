package com.bookworm.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {

    private UUID wishlistItemId;
    private UUID bookId;
    private String title;
    private String authorName;
    private BigDecimal price;
    private String format;
    private String coverImageUrl;
    private Instant addedAt;
}
