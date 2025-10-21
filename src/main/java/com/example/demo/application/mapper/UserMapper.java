package com.example.demo.application.mapper;

import com.example.demo.application.dto.UserDto;
import com.example.demo.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

/**
 * Mapper interface for User entity and DTO conversions.
 * Uses MapStruct for automatic mapping generation.
 */
@Mapper(componentModel = "spring")
@Component
public interface UserMapper {
    
    /**
     * Convert User entity to UserDto.
     * @param user the user entity
     * @return UserDto
     */
    UserDto toDto(User user);
    
    /**
     * Convert UserDto to User entity.
     * @param userDto the user DTO
     * @return User entity
     */
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "budgets", ignore = true)
    User toEntity(UserDto userDto);
    
    /**
     * Update User entity from UserDto.
     * @param userDto the user DTO
     * @param user the user entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "budgets", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UserDto userDto, @org.mapstruct.MappingTarget User user);
}
