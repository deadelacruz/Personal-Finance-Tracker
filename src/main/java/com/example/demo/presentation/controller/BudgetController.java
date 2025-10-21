package com.example.demo.presentation.controller;

import com.example.demo.application.dto.BudgetDto;
import com.example.demo.application.mapper.BudgetMapper;
import com.example.demo.application.service.BudgetService;
import com.example.demo.domain.entity.Budget;
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
 * REST Controller for Budget operations.
 * Follows Clean Architecture principles and provides RESTful API endpoints.
 */
@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {
    
    private final BudgetService budgetService;
    private final BudgetMapper budgetMapper;
    
    @Autowired
    public BudgetController(BudgetService budgetService, BudgetMapper budgetMapper) {
        this.budgetService = budgetService;
        this.budgetMapper = budgetMapper;
    }
    
    /**
     * Create a new budget.
     * @param budgetDto the budget data
     * @param user the authenticated user
     * @return created budget
     */
    @PostMapping
    public ResponseEntity<BudgetDto> createBudget(@Valid @RequestBody BudgetDto budgetDto,
                                                 @AuthenticationPrincipal User user) {
        try {
            Budget budget = budgetMapper.toEntity(budgetDto);
            budget.setUser(user);
            
            Budget createdBudget = budgetService.createBudget(budget);
            BudgetDto responseDto = budgetMapper.toDto(createdBudget);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get budget by ID.
     * @param id the budget ID
     * @param user the authenticated user
     * @return budget if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto> getBudget(@PathVariable Long id,
                                              @AuthenticationPrincipal User user) {
        Optional<Budget> budget = budgetService.findById(id);
        
        if (budget.isEmpty() || !budget.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        BudgetDto responseDto = budgetMapper.toDto(budget.get());
        return ResponseEntity.ok(responseDto);
    }
    
    /**
     * Get all active budgets for user.
     * @param user the authenticated user
     * @return list of active budgets
     */
    @GetMapping
    public ResponseEntity<List<BudgetDto>> getActiveBudgets(@AuthenticationPrincipal User user) {
        List<Budget> budgets = budgetService.findActiveByUser(user);
        List<BudgetDto> responseDtos = budgets.stream()
                .map(budgetMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get all budgets for user (including inactive).
     * @param user the authenticated user
     * @return list of all budgets
     */
    @GetMapping("/all")
    public ResponseEntity<List<BudgetDto>> getAllBudgets(@AuthenticationPrincipal User user) {
        List<Budget> budgets = budgetService.findByUser(user);
        List<BudgetDto> responseDtos = budgets.stream()
                .map(budgetMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get current budgets for user.
     * @param user the authenticated user
     * @return list of current budgets
     */
    @GetMapping("/current")
    public ResponseEntity<List<BudgetDto>> getCurrentBudgets(@AuthenticationPrincipal User user) {
        List<Budget> budgets = budgetService.findCurrentBudgets(user);
        List<BudgetDto> responseDtos = budgets.stream()
                .map(budgetMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    
    /**
     * Get budget summaries for current budgets.
     * @param user the authenticated user
     * @return list of budget summaries
     */
    @GetMapping("/summaries")
    public ResponseEntity<List<BudgetSummaryDto>> getBudgetSummaries(@AuthenticationPrincipal User user) {
        List<BudgetService.BudgetSummary> summaries = budgetService.getCurrentBudgetSummaries(user);
        List<BudgetSummaryDto> responseDtos = summaries.stream()
                .map(summary -> {
                    BudgetDto budgetDto = budgetMapper.toDto(summary.getBudget());
                    return new BudgetSummaryDto(
                        budgetDto,
                        summary.getSpentAmount(),
                        summary.getRemainingAmount(),
                        summary.getUtilizationPercentage(),
                        summary.isOverBudget()
                    );
                })
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Update budget.
     * @param id the budget ID
     * @param budgetDto the updated budget data
     * @param user the authenticated user
     * @return updated budget
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto> updateBudget(@PathVariable Long id,
                                                 @Valid @RequestBody BudgetDto budgetDto,
                                                 @AuthenticationPrincipal User user) {
        try {
            Optional<Budget> existingBudget = budgetService.findById(id);
            
            if (existingBudget.isEmpty() || !existingBudget.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            Budget budget = existingBudget.get();
            budgetMapper.updateEntity(budgetDto, budget);
            
            Budget updatedBudget = budgetService.updateBudget(budget);
            BudgetDto responseDto = budgetMapper.toDto(updatedBudget);
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Deactivate budget.
     * @param id the budget ID
     * @param user the authenticated user
     * @return no content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id,
                                            @AuthenticationPrincipal User user) {
        try {
            Optional<Budget> budget = budgetService.findById(id);
            
            if (budget.isEmpty() || !budget.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            budgetService.deleteBudget(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Activate budget.
     * @param id the budget ID
     * @param user the authenticated user
     * @return no content if successful
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateBudget(@PathVariable Long id,
                                              @AuthenticationPrincipal User user) {
        try {
            Optional<Budget> budget = budgetService.findById(id);
            
            if (budget.isEmpty() || !budget.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            budgetService.activateBudget(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Inner class for budget summary response.
     */
    public static class BudgetSummaryDto {
        private final BudgetDto budget;
        private final java.math.BigDecimal spentAmount;
        private final java.math.BigDecimal remainingAmount;
        private final double utilizationPercentage;
        private final boolean overBudget;
        
        public BudgetSummaryDto(BudgetDto budget, java.math.BigDecimal spentAmount, 
                               java.math.BigDecimal remainingAmount, double utilizationPercentage, 
                               boolean overBudget) {
            this.budget = budget;
            this.spentAmount = spentAmount;
            this.remainingAmount = remainingAmount;
            this.utilizationPercentage = utilizationPercentage;
            this.overBudget = overBudget;
        }
        
        public BudgetDto getBudget() {
            return budget;
        }
        
        public java.math.BigDecimal getSpentAmount() {
            return spentAmount;
        }
        
        public java.math.BigDecimal getRemainingAmount() {
            return remainingAmount;
        }
        
        public double getUtilizationPercentage() {
            return utilizationPercentage;
        }
        
        public boolean isOverBudget() {
            return overBudget;
        }
    }
}
