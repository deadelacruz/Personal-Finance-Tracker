package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Budget;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Budget entity.
 * Follows Clean Architecture principles and provides budget-specific queries.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    /**
     * Find all active budgets for a specific user.
     * @param user the user
     * @return List of active budgets
     */
    List<Budget> findByUserAndIsActiveTrueOrderByStartDateDesc(User user);
    
    /**
     * Find all budgets for a specific user.
     * @param user the user
     * @return List of budgets
     */
    List<Budget> findByUserOrderByStartDateDesc(User user);
    
    /**
     * Find current budgets for user (active and within date range).
     * @param user the user
     * @param currentDate the current date
     * @return List of current budgets
     */
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND b.startDate <= :currentDate AND b.endDate >= :currentDate ORDER BY b.startDate DESC")
    List<Budget> findCurrentBudgets(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    
    /**
     * Find budget by name and user.
     * @param name the budget name
     * @param user the user
     * @return Optional containing the budget if found
     */
    Optional<Budget> findByNameAndUser(String name, User user);
    
    /**
     * Check if budget name exists for user.
     * @param name the budget name
     * @param user the user
     * @return true if budget name exists, false otherwise
     */
    boolean existsByNameAndUser(String name, User user);
    
    
    /**
     * Find overlapping budgets for user in date range.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return List of overlapping budgets
     */
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND " +
           "((b.startDate <= :startDate AND b.endDate >= :startDate) OR " +
           "(b.startDate <= :endDate AND b.endDate >= :endDate) OR " +
           "(b.startDate >= :startDate AND b.endDate <= :endDate))")
    List<Budget> findOverlappingBudgets(@Param("user") User user, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
}
