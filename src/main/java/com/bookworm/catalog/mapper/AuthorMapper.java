package com.bookworm.catalog.mapper;

import com.bookworm.catalog.dto.AuthorDetailResponse;
import com.bookworm.catalog.dto.AuthorSummaryResponse;
import com.bookworm.catalog.entity.Author;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {BookMapper.class})
public interface AuthorMapper {

    AuthorSummaryResponse toSummaryResponse(Author author);

    List<AuthorSummaryResponse> toSummaryResponseList(List<Author> authors);

    @Mapping(target = "books", ignore = true)
    AuthorDetailResponse toDetailResponse(Author author);
}
