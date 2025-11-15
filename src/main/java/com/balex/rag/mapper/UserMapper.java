package com.balex.rag.mapper;

import com.balex.rag.model.dto.UserDTO;
import com.balex.rag.model.dto.UserProfileDTO;
import com.balex.rag.model.dto.UserSearchDTO;
import com.balex.rag.model.entity.User;
import com.balex.rag.model.enums.RegistrationStatus;
import com.balex.rag.model.request.user.NewUserRequest;
import com.balex.rag.model.request.user.RegistrationUserRequest;
import com.balex.rag.model.request.user.UpdateUserRequest;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Objects;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {RegistrationStatus.class, Objects.class, DateTimeUtils.class}
)
public interface UserMapper {

    UserDTO toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "registrationStatus", expression = "java(RegistrationStatus.ACTIVE)")
    User createUser(NewUserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    void updateUser(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(source = "deleted", target = "isDeleted")
    UserSearchDTO toUserSearchDto(User user);

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "token", source = "token")
    @Mapping(target = "refreshToken", source = "refreshToken")
    UserProfileDTO toUserProfileDto(User user, String token, String refreshToken);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "registrationStatus", expression = "java(RegistrationStatus.ACTIVE)")
    User fromDto(RegistrationUserRequest request);

}

