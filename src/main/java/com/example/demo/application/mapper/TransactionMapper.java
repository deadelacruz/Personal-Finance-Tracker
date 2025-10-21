package com.example.demo.application.mapper;

import com.example.demo.application.dto.TransactionDto;
import com.example.demo.domain.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

/**
 * Mapper interface for Transaction entity and DTO conversions.
 * Uses MapStruct for automatic mapping generation.
 */
@Mapper(componentModel = "spring")
@Component
public interface TransactionMapper {
    
    /**
     * Convert Transaction entity to TransactionDto.
     * @param transaction the transaction entity
     * @return TransactionDto
     */
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    TransactionDto toDto(Transaction transaction);
    
    /**
     * Convert TransactionDto to Transaction entity.
     * @param transactionDto the transaction DTO
     * @return Transaction entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(TransactionDto transactionDto);
    
    /**
     * Update Transaction entity from TransactionDto.
     * @param transactionDto the transaction DTO
     * @param transaction the transaction entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(TransactionDto transactionDto, @org.mapstruct.MappingTarget Transaction transaction);
}
