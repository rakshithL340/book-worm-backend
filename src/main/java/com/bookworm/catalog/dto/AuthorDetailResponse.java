package com.bookworm.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDetailResponse {

    private UUID authorId;
    private String name;
    private String bio;
    private String avatarUrl;
    private List<BookCardResponse> books;
}
