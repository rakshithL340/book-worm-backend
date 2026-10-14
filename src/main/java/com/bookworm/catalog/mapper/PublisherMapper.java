package com.bookworm.catalog.mapper;

import com.bookworm.catalog.dto.PublisherResponse;
import com.bookworm.catalog.entity.Publisher;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublisherMapper {

    PublisherResponse toResponse(Publisher publisher);

    List<PublisherResponse> toResponseList(List<Publisher> publishers);
}
