package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Category;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Category entity.
 * Follows Clean Architecture principles and provides category-specific queries.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find all active categories for a specific user.
     * @param user the user
     * @return List of active categories
     */
    List<Category> findByUserAndIsActiveTrueOrderByName(User user);
    
    /**
     * Find all categories for a specific user.
     * @param user the user
     * @return List of categories
     */
    List<Category> findByUserOrderByName(User user);
    
    /**
     * Find category by name and user.
     * @param name the category name
     * @param user the user
     * @return Optional containing the category if found
     */
    Optional<Category> findByNameAndUser(String name, User user);
    
    /**
     * Check if category name exists for user.
     * @param name the category name
     * @param user the user
     * @return true if category name exists, false otherwise
     */
    boolean existsByNameAndUser(String name, User user);
    
    /**
     * Find categories with transaction count for user.
     * @param user the user
     * @return List of categories with transaction counts
     */
    @Query("SELECT c, COUNT(t) as transactionCount FROM Category c LEFT JOIN c.transactions t WHERE c.user = :user AND c.isActive = true GROUP BY c ORDER BY c.name")
    List<Object[]> findCategoriesWithTransactionCount(@Param("user") User user);
    
    /**
     * Find most used categories for user.
     * @param user the user
     * @param pageable pagination information
     * @return List of most used categories
     */
    @Query("SELECT c FROM Category c LEFT JOIN c.transactions t WHERE c.user = :user AND c.isActive = true GROUP BY c ORDER BY COUNT(t) DESC")
    List<Category> findMostUsedCategories(@Param("user") User user, org.springframework.data.domain.Pageable pageable);
}
