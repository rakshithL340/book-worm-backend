package com.bookworm.auth.mapper;

import com.bookworm.auth.dto.RegisterRequest;
import com.bookworm.auth.dto.UserSummaryDto;
import com.bookworm.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", constant = "ROLE_MEMBER")
    User toEntity(RegisterRequest request);

    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    UserSummaryDto toUserSummaryDto(User user);
}
