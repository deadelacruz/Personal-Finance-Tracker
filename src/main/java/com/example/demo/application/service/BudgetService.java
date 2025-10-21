package com.example.demo.application.service;

import com.example.demo.domain.entity.Budget;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.Category;
import com.example.demo.domain.repository.BudgetRepository;
import com.example.demo.domain.repository.CategoryRepository;
import com.example.demo.domain.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Budget operations.
 * Follows SOLID principles and Clean Architecture by handling business logic.
 */
@Service
@Transactional
public class BudgetService {
    
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public BudgetService(BudgetRepository budgetRepository, 
                        CategoryRepository categoryRepository,
                        TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Create a new budget.
     * @param budget the budget to create
     * @return the created budget
     */
    public Budget createBudget(Budget budget) {
        validateBudget(budget);
        return budgetRepository.save(budget);
    }
    
    /**
     * Find budget by ID.
     * @param id the budget ID
     * @return Optional containing the budget if found
     */
    @Transactional(readOnly = true)
    public Optional<Budget> findById(Long id) {
        return budgetRepository.findById(id);
    }
    
    /**
     * Find all active budgets for user.
     * @param user the user
     * @return List of active budgets
     */
    @Transactional(readOnly = true)
    public List<Budget> findActiveByUser(User user) {
        return budgetRepository.findByUserAndIsActiveTrueOrderByStartDateDesc(user);
    }
    
    /**
     * Find all budgets for user.
     * @param user the user
     * @return List of budgets
     */
    @Transactional(readOnly = true)
    public List<Budget> findByUser(User user) {
        return budgetRepository.findByUserOrderByStartDateDesc(user);
    }
    
    /**
     * Find current budgets for user.
     * @param user the user
     * @return List of current budgets
     */
    @Transactional(readOnly = true)
    public List<Budget> findCurrentBudgets(User user) {
        return budgetRepository.findCurrentBudgets(user, LocalDate.now());
    }
    
    
    /**
     * Update budget.
     * @param budget the budget to update
     * @return the updated budget
     */
    public Budget updateBudget(Budget budget) {
        validateBudget(budget);
        return budgetRepository.save(budget);
    }
    
    /**
     * Deactivate budget.
     * @param id the budget ID
     */
    public void deactivateBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + id));
        
        budget.setIsActive(false);
        budgetRepository.save(budget);
    }
    
    /**
     * Activate budget.
     * @param id the budget ID
     */
    public void activateBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found with id: " + id));
        
        budget.setIsActive(true);
        budgetRepository.save(budget);
    }
    
    /**
     * Delete budget (soft delete by deactivating).
     * @param id the budget ID
     */
    public void deleteBudget(Long id) {
        deactivateBudget(id);
    }
    
    /**
     * Calculate spent amount for budget.
     * @param budget the budget
     * @return spent amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSpentAmount(Budget budget) {
        if (budget.getCategory() != null) {
            // Category-specific budget
            return transactionRepository.calculateTotalByUserAndCategoryAndDateRange(
                budget.getUser(),
                budget.getCategory().getId(),
                budget.getStartDate().atStartOfDay(),
                budget.getEndDate().atTime(23, 59, 59)
            );
        } else {
            // General budget - calculate all expenses
            return transactionRepository.calculateTotalByUserAndTypeAndDateRange(
                budget.getUser(),
                com.example.demo.domain.entity.Transaction.TransactionType.EXPENSE,
                budget.getStartDate().atStartOfDay(),
                budget.getEndDate().atTime(23, 59, 59)
            );
        }
    }
    
    /**
     * Get budget summary with spent amount and remaining amount.
     * @param budget the budget
     * @return BudgetSummary object
     */
    @Transactional(readOnly = true)
    public BudgetSummary getBudgetSummary(Budget budget) {
        BigDecimal spentAmount = calculateSpentAmount(budget);
        BigDecimal remainingAmount = budget.getRemainingAmount(spentAmount);
        double utilizationPercentage = budget.getUtilizationPercentage(spentAmount);
        
        return new BudgetSummary(budget, spentAmount, remainingAmount, utilizationPercentage);
    }
    
    /**
     * Get budget summaries for user's current budgets.
     * @param user the user
     * @return List of budget summaries
     */
    @Transactional(readOnly = true)
    public List<BudgetSummary> getCurrentBudgetSummaries(User user) {
        List<Budget> currentBudgets = findCurrentBudgets(user);
        return currentBudgets.stream()
                .map(this::getBudgetSummary)
                .toList();
    }
    
    /**
     * Validate budget.
     * @param budget the budget to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateBudget(Budget budget) {
        if (budget == null) {
            throw new IllegalArgumentException("Budget cannot be null");
        }
        
        if (budget.getName() == null || budget.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Budget name is required");
        }
        
        if (budget.getBudgetAmount() == null || budget.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget amount must be greater than 0");
        }
        
        if (budget.getStartDate() == null) {
            throw new IllegalArgumentException("Budget start date is required");
        }
        
        if (budget.getEndDate() == null) {
            throw new IllegalArgumentException("Budget end date is required");
        }
        
        if (budget.getStartDate().isAfter(budget.getEndDate())) {
            throw new IllegalArgumentException("Budget start date cannot be after end date");
        }
        
        if (budget.getUser() == null) {
            throw new IllegalArgumentException("Budget user is required");
        }
        
        // Check for duplicate name within user's budgets
        if (budget.getId() == null) {
            // New budget - check if name already exists
            if (budgetRepository.existsByNameAndUser(budget.getName(), budget.getUser())) {
                throw new IllegalArgumentException("Budget name already exists: " + budget.getName());
            }
        } else {
            // Existing budget - check if name exists for other budgets
            Optional<Budget> existingBudget = budgetRepository.findByNameAndUser(budget.getName(), budget.getUser());
            if (existingBudget.isPresent() && !existingBudget.get().getId().equals(budget.getId())) {
                throw new IllegalArgumentException("Budget name already exists: " + budget.getName());
            }
        }
        
        // Validate category if provided
        if (budget.getCategory() != null && budget.getCategory().getId() != null) {
            Optional<Category> category = categoryRepository.findById(budget.getCategory().getId());
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category not found with id: " + budget.getCategory().getId());
            }
        }
        
        // Check for overlapping budgets
        List<Budget> overlappingBudgets = budgetRepository.findOverlappingBudgets(
            budget.getUser(), budget.getStartDate(), budget.getEndDate());
        
        if (budget.getId() != null) {
            // For updates, exclude the current budget from overlap check
            overlappingBudgets = overlappingBudgets.stream()
                    .filter(b -> !b.getId().equals(budget.getId()))
                    .toList();
        }
        
        if (!overlappingBudgets.isEmpty()) {
            throw new IllegalArgumentException("Budget overlaps with existing budget: " + 
                overlappingBudgets.get(0).getName());
        }
    }
    
    /**
     * Inner class representing budget summary information.
     */
    public static class BudgetSummary {
        private final Budget budget;
        private final BigDecimal spentAmount;
        private final BigDecimal remainingAmount;
        private final double utilizationPercentage;
        
        public BudgetSummary(Budget budget, BigDecimal spentAmount, BigDecimal remainingAmount, double utilizationPercentage) {
            this.budget = budget;
            this.spentAmount = spentAmount;
            this.remainingAmount = remainingAmount;
            this.utilizationPercentage = utilizationPercentage;
        }
        
        public Budget getBudget() {
            return budget;
        }
        
        public BigDecimal getSpentAmount() {
            return spentAmount;
        }
        
        public BigDecimal getRemainingAmount() {
            return remainingAmount;
        }
        
        public double getUtilizationPercentage() {
            return utilizationPercentage;
        }
        
        public boolean isOverBudget() {
            return spentAmount.compareTo(budget.getBudgetAmount()) > 0;
        }
    }
}
