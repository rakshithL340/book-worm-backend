package com.bookworm.catalog.controller;

import com.bookworm.catalog.dto.CategoryResponse;
import com.bookworm.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "20-category taxonomy navigation")
public class CategoryController {

    private final CatalogService catalogService;

    @Operation(summary = "List all 20 book categories", description = "Returns the structured category taxonomy for navigation and filtering.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        log.info("REST request to list all catalog categories");
        List<CategoryResponse> categories = catalogService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
}
