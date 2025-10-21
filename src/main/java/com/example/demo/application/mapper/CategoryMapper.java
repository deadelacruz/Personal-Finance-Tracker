package com.example.demo.application.mapper;

import com.example.demo.application.dto.CategoryDto;
import com.example.demo.domain.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

/**
 * Mapper interface for Category entity and DTO conversions.
 * Uses MapStruct for automatic mapping generation.
 */
@Mapper(componentModel = "spring")
@Component
public interface CategoryMapper {
    
    /**
     * Convert Category entity to CategoryDto.
     * @param category the category entity
     * @return CategoryDto
     */
    @Mapping(target = "transactionCount", ignore = true)
    CategoryDto toDto(Category category);
    
    /**
     * Convert CategoryDto to Category entity.
     * @param categoryDto the category DTO
     * @return Category entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryDto categoryDto);
    
    /**
     * Update Category entity from CategoryDto.
     * @param categoryDto the category DTO
     * @param category the category entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CategoryDto categoryDto, @org.mapstruct.MappingTarget Category category);
}
