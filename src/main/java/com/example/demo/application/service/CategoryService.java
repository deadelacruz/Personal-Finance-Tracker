package com.example.demo.application.service;

import com.example.demo.domain.entity.Category;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Category operations.
 * Follows SOLID principles and Clean Architecture by handling business logic.
 */
@Service
@Transactional
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Create a new category.
     * @param category the category to create
     * @return the created category
     */
    public Category createCategory(Category category) {
        validateCategory(category);
        return categoryRepository.save(category);
    }
    
    /**
     * Find category by ID.
     * @param id the category ID
     * @return Optional containing the category if found
     */
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
    
    /**
     * Find all active categories for user.
     * @param user the user
     * @return List of active categories
     */
    @Transactional(readOnly = true)
    public List<Category> findActiveByUser(User user) {
        return categoryRepository.findByUserAndIsActiveTrueOrderByName(user);
    }
    
    /**
     * Find all categories for user.
     * @param user the user
     * @return List of categories
     */
    @Transactional(readOnly = true)
    public List<Category> findByUser(User user) {
        return categoryRepository.findByUserOrderByName(user);
    }
    
    /**
     * Find category by name and user.
     * @param name the category name
     * @param user the user
     * @return Optional containing the category if found
     */
    @Transactional(readOnly = true)
    public Optional<Category> findByNameAndUser(String name, User user) {
        return categoryRepository.findByNameAndUser(name, user);
    }
    
    /**
     * Update category.
     * @param category the category to update
     * @return the updated category
     */
    public Category updateCategory(Category category) {
        validateCategory(category);
        return categoryRepository.save(category);
    }
    
    /**
     * Deactivate category.
     * @param id the category ID
     */
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
        
        category.setIsActive(false);
        categoryRepository.save(category);
    }
    
    /**
     * Activate category.
     * @param id the category ID
     */
    public void activateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
        
        category.setIsActive(true);
        categoryRepository.save(category);
    }
    
    /**
     * Delete category (soft delete by deactivating).
     * @param id the category ID
     */
    public void deleteCategory(Long id) {
        deactivateCategory(id);
    }
    
    /**
     * Get categories with transaction count for user.
     * @param user the user
     * @return List of categories with transaction counts
     */
    @Transactional(readOnly = true)
    public List<Object[]> getCategoriesWithTransactionCount(User user) {
        return categoryRepository.findCategoriesWithTransactionCount(user);
    }
    
    /**
     * Get most used categories for user.
     * @param user the user
     * @param limit maximum number of categories
     * @return List of most used categories
     */
    @Transactional(readOnly = true)
    public List<Category> getMostUsedCategories(User user, int limit) {
        return categoryRepository.findMostUsedCategories(user, org.springframework.data.domain.Pageable.ofSize(limit));
    }
    
    /**
     * Create default categories for new user.
     * @param user the user
     * @return List of created default categories
     */
    public List<Category> createDefaultCategories(User user) {
        List<Category> defaultCategories = List.of(
            new Category("Food & Dining", "Restaurants, groceries, and food expenses", user),
            new Category("Transportation", "Gas, public transport, car maintenance", user),
            new Category("Entertainment", "Movies, games, hobbies, and leisure activities", user),
            new Category("Shopping", "Clothing, electronics, and general shopping", user),
            new Category("Bills & Utilities", "Electricity, water, internet, phone bills", user),
            new Category("Healthcare", "Medical expenses, pharmacy, health insurance", user),
            new Category("Education", "Books, courses, school fees", user),
            new Category("Travel", "Vacation, business trips, accommodation", user),
            new Category("Salary", "Monthly salary and regular income", user),
            new Category("Freelance", "Freelance work and side projects", user),
            new Category("Investment", "Investment returns and dividends", user),
            new Category("Other", "Miscellaneous income and expenses", user)
        );
        
        return categoryRepository.saveAll(defaultCategories);
    }
    
    /**
     * Validate category.
     * @param category the category to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        
        if (category.getUser() == null) {
            throw new IllegalArgumentException("Category user is required");
        }
        
        // Check for duplicate name within user's categories
        if (category.getId() == null) {
            // New category - check if name already exists
            if (categoryRepository.existsByNameAndUser(category.getName(), category.getUser())) {
                throw new IllegalArgumentException("Category name already exists: " + category.getName());
            }
        } else {
            // Existing category - check if name exists for other categories
            Optional<Category> existingCategory = categoryRepository.findByNameAndUser(category.getName(), category.getUser());
            if (existingCategory.isPresent() && !existingCategory.get().getId().equals(category.getId())) {
                throw new IllegalArgumentException("Category name already exists: " + category.getName());
            }
        }
    }
}
