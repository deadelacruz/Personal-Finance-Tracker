package com.example.demo.application.mapper;

import com.example.demo.application.dto.BudgetDto;
import com.example.demo.domain.entity.Budget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

/**
 * Mapper interface for Budget entity and DTO conversions.
 * Uses MapStruct for automatic mapping generation.
 */
@Mapper(componentModel = "spring")
@Component
public interface BudgetMapper {
    
    /**
     * Convert Budget entity to BudgetDto.
     * @param budget the budget entity
     * @return BudgetDto
     */
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "spentAmount", ignore = true)
    @Mapping(target = "remainingAmount", ignore = true)
    @Mapping(target = "utilizationPercentage", ignore = true)
    @Mapping(target = "overBudget", ignore = true)
    @Mapping(target = "daysRemaining", ignore = true)
    BudgetDto toDto(Budget budget);
    
    /**
     * Convert BudgetDto to Budget entity.
     * @param budgetDto the budget DTO
     * @return Budget entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Budget toEntity(BudgetDto budgetDto);
    
    /**
     * Update Budget entity from BudgetDto.
     * @param budgetDto the budget DTO
     * @param budget the budget entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(BudgetDto budgetDto, @org.mapstruct.MappingTarget Budget budget);
}
