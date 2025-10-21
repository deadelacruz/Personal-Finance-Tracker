package com.example.demo.presentation.controller;

import com.example.demo.application.dto.CategoryDto;
import com.example.demo.application.mapper.CategoryMapper;
import com.example.demo.application.service.CategoryService;
import com.example.demo.domain.entity.Category;
import com.example.demo.domain.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Category operations.
 * Follows Clean Architecture principles and provides RESTful API endpoints.
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    
    @Autowired
    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }
    
    /**
     * Create a new category.
     * @param categoryDto the category data
     * @param user the authenticated user
     * @return created category
     */
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto,
                                                     @AuthenticationPrincipal User user) {
        try {
            Category category = categoryMapper.toEntity(categoryDto);
            category.setUser(user);
            
            Category createdCategory = categoryService.createCategory(category);
            CategoryDto responseDto = categoryMapper.toDto(createdCategory);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get category by ID.
     * @param id the category ID
     * @param user the authenticated user
     * @return category if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Long id,
                                                  @AuthenticationPrincipal User user) {
        Optional<Category> category = categoryService.findById(id);
        
        if (category.isEmpty() || !category.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        CategoryDto responseDto = categoryMapper.toDto(category.get());
        return ResponseEntity.ok(responseDto);
    }
    
    /**
     * Get all active categories for user.
     * @param user the authenticated user
     * @return list of active categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getActiveCategories(@AuthenticationPrincipal User user) {
        List<Category> categories = categoryService.findActiveByUser(user);
        List<CategoryDto> responseDtos = categories.stream()
                .map(categoryMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get all categories for user (including inactive).
     * @param user the authenticated user
     * @return list of all categories
     */
    @GetMapping("/all")
    public ResponseEntity<List<CategoryDto>> getAllCategories(@AuthenticationPrincipal User user) {
        List<Category> categories = categoryService.findByUser(user);
        List<CategoryDto> responseDtos = categories.stream()
                .map(categoryMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get categories with transaction count.
     * @param user the authenticated user
     * @return list of categories with transaction counts
     */
    @GetMapping("/with-counts")
    public ResponseEntity<List<CategoryWithCountDto>> getCategoriesWithCounts(@AuthenticationPrincipal User user) {
        List<Object[]> results = categoryService.getCategoriesWithTransactionCount(user);
        List<CategoryWithCountDto> responseDtos = results.stream()
                .map(result -> {
                    Category category = (Category) result[0];
                    Long count = (Long) result[1];
                    CategoryDto categoryDto = categoryMapper.toDto(category);
                    return new CategoryWithCountDto(categoryDto, count.intValue());
                })
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get most used categories.
     * @param limit maximum number of categories
     * @param user the authenticated user
     * @return list of most used categories
     */
    @GetMapping("/most-used")
    public ResponseEntity<List<CategoryDto>> getMostUsedCategories(
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal User user) {
        List<Category> categories = categoryService.getMostUsedCategories(user, limit);
        List<CategoryDto> responseDtos = categories.stream()
                .map(categoryMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Update category.
     * @param id the category ID
     * @param categoryDto the updated category data
     * @param user the authenticated user
     * @return updated category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id,
                                                     @Valid @RequestBody CategoryDto categoryDto,
                                                     @AuthenticationPrincipal User user) {
        try {
            Optional<Category> existingCategory = categoryService.findById(id);
            
            if (existingCategory.isEmpty() || !existingCategory.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            Category category = existingCategory.get();
            categoryMapper.updateEntity(categoryDto, category);
            
            Category updatedCategory = categoryService.updateCategory(category);
            CategoryDto responseDto = categoryMapper.toDto(updatedCategory);
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Deactivate category.
     * @param id the category ID
     * @param user the authenticated user
     * @return no content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id,
                                              @AuthenticationPrincipal User user) {
        try {
            Optional<Category> category = categoryService.findById(id);
            
            if (category.isEmpty() || !category.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Activate category.
     * @param id the category ID
     * @param user the authenticated user
     * @return no content if successful
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateCategory(@PathVariable Long id,
                                                @AuthenticationPrincipal User user) {
        try {
            Optional<Category> category = categoryService.findById(id);
            
            if (category.isEmpty() || !category.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            categoryService.activateCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Create default categories for user.
     * @param user the authenticated user
     * @return list of created default categories
     */
    @PostMapping("/defaults")
    public ResponseEntity<List<CategoryDto>> createDefaultCategories(@AuthenticationPrincipal User user) {
        List<Category> defaultCategories = categoryService.createDefaultCategories(user);
        List<CategoryDto> responseDtos = defaultCategories.stream()
                .map(categoryMapper::toDto)
                .toList();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDtos);
    }
    
    /**
     * Inner class for category with count response.
     */
    public static class CategoryWithCountDto {
        private final CategoryDto category;
        private final int transactionCount;
        
        public CategoryWithCountDto(CategoryDto category, int transactionCount) {
            this.category = category;
            this.transactionCount = transactionCount;
        }
        
        public CategoryDto getCategory() {
            return category;
        }
        
        public int getTransactionCount() {
            return transactionCount;
        }
    }
}
