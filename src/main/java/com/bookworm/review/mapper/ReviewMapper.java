package com.bookworm.review.mapper;

import com.bookworm.review.dto.ReviewCreateRequest;
import com.bookworm.review.dto.ReviewResponse;
import com.bookworm.review.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewMapper {

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reviewerName", ignore = true)
    Review toEntity(ReviewCreateRequest request);

    ReviewResponse toResponse(Review review);

    List<ReviewResponse> toResponseList(List<Review> reviews);
}
