package com.bookworm.catalog.controller;

import com.bookworm.catalog.dto.AuthorDetailResponse;
import com.bookworm.catalog.dto.AuthorSummaryResponse;
import com.bookworm.catalog.service.CatalogService;
import com.bookworm.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "Author directory and biography details ('About the writer')")
public class AuthorController {

    private final CatalogService catalogService;

    @Operation(summary = "List authors", description = "Retrieves a paginated list of registered book authors.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated authors list")
    })
    @GetMapping
    public ResponseEntity<PageResponse<AuthorSummaryResponse>> listAuthors(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "12") int size
    ) {
        log.info("REST request to list authors: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<AuthorSummaryResponse> authors = catalogService.getAuthors(pageable);
        return ResponseEntity.ok(authors);
    }

    @Operation(summary = "Get author profile details", description = "Returns author biography and associated book titles ('About the writer').")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author details"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @GetMapping("/{authorId}")
    public ResponseEntity<AuthorDetailResponse> getAuthorById(
            @Parameter(description = "Author UUID", required = true)
            @PathVariable UUID authorId
    ) {
        log.info("REST request to get author details: {}", authorId);
        AuthorDetailResponse response = catalogService.getAuthorById(authorId);
        return ResponseEntity.ok(response);
    }
}
