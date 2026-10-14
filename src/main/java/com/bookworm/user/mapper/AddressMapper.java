package com.bookworm.user.mapper;

import com.bookworm.user.dto.AddressCreateRequest;
import com.bookworm.user.dto.AddressResponse;
import com.bookworm.user.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "addressId", ignore = true)
    @Mapping(target = "user", ignore = true)
    Address toEntity(AddressCreateRequest request);

    AddressResponse toResponse(Address address);

    List<AddressResponse> toResponseList(List<Address> addresses);

    @Mapping(target = "addressId", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(AddressCreateRequest request, @MappingTarget Address address);
}
