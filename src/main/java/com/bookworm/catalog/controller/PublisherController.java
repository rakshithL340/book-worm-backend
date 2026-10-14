package com.bookworm.catalog.controller;

import com.bookworm.catalog.dto.PublisherResponse;
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
@RequestMapping("/publishers")
@RequiredArgsConstructor
@Tag(name = "Publishers", description = "Book publishers directory")
public class PublisherController {

    private final CatalogService catalogService;

    @Operation(summary = "List publishers", description = "Returns all registered book publishers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of publishers")
    })
    @GetMapping
    public ResponseEntity<List<PublisherResponse>> listPublishers() {
        log.info("REST request to list publishers");
        List<PublisherResponse> publishers = catalogService.getAllPublishers();
        return ResponseEntity.ok(publishers);
    }
}
