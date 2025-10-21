package com.example.demo.application.service;

import com.example.demo.domain.entity.Transaction;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.Category;
import com.example.demo.domain.repository.TransactionRepository;
import com.example.demo.domain.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Transaction operations.
 * Follows SOLID principles and Clean Architecture by handling business logic.
 */
@Service
@Transactional
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public TransactionService(TransactionRepository transactionRepository, 
                            CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Create a new transaction.
     * @param transaction the transaction to create
     * @return the created transaction
     */
    public Transaction createTransaction(Transaction transaction) {
        validateTransaction(transaction);
        return transactionRepository.save(transaction);
    }
    
    /**
     * Find transaction by ID.
     * @param id the transaction ID
     * @return Optional containing the transaction if found
     */
    @Transactional(readOnly = true)
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }
    
    /**
     * Find transactions by user with pagination.
     * @param user the user
     * @param pageable pagination information
     * @return Page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> findByUser(User user, Pageable pageable) {
        return transactionRepository.findByUserOrderByTransactionDateDesc(user, pageable);
    }
    
    /**
     * Find transactions by user and date range.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return List of transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> findByUserAndDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByUserAndDateRange(user, startDate, endDate);
    }
    
    /**
     * Find transactions by user and type.
     * @param user the user
     * @param transactionType the transaction type
     * @param pageable pagination information
     * @return Page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> findByUserAndType(User user, Transaction.TransactionType transactionType, Pageable pageable) {
        return transactionRepository.findByUserAndTransactionTypeOrderByTransactionDateDesc(user, transactionType, pageable);
    }
    
    /**
     * Find transactions by user and category.
     * @param user the user
     * @param categoryId the category ID
     * @param pageable pagination information
     * @return Page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> findByUserAndCategory(User user, Long categoryId, Pageable pageable) {
        return transactionRepository.findByUserAndCategoryIdOrderByTransactionDateDesc(user, categoryId, pageable);
    }
    
    /**
     * Update transaction.
     * @param transaction the transaction to update
     * @return the updated transaction
     */
    public Transaction updateTransaction(Transaction transaction) {
        validateTransaction(transaction);
        return transactionRepository.save(transaction);
    }
    
    /**
     * Delete transaction.
     * @param id the transaction ID
     */
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new IllegalArgumentException("Transaction not found with id: " + id);
        }
        transactionRepository.deleteById(id);
    }
    
    /**
     * Calculate total amount for user by type in date range.
     * @param user the user
     * @param transactionType the transaction type
     * @param startDate start date
     * @param endDate end date
     * @return total amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalByUserAndTypeAndDateRange(User user, 
                                                            Transaction.TransactionType transactionType,
                                                            LocalDateTime startDate, 
                                                            LocalDateTime endDate) {
        return transactionRepository.calculateTotalByUserAndTypeAndDateRange(user, transactionType, startDate, endDate);
    }
    
    /**
     * Calculate total amount by category for user in date range.
     * @param user the user
     * @param categoryId the category ID
     * @param startDate start date
     * @param endDate end date
     * @return total amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalByUserAndCategoryAndDateRange(User user, 
                                                                Long categoryId,
                                                                LocalDateTime startDate, 
                                                                LocalDateTime endDate) {
        return transactionRepository.calculateTotalByUserAndCategoryAndDateRange(user, categoryId, startDate, endDate);
    }
    
    /**
     * Get recent transactions for user.
     * @param user the user
     * @param pageable pagination information
     * @return List of recent transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> getRecentTransactions(User user, Pageable pageable) {
        return transactionRepository.findRecentTransactionsByUser(user, pageable);
    }
    
    /**
     * Calculate net worth for user in date range.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return net worth (income - expenses)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateNetWorth(User user, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalIncome = calculateTotalByUserAndTypeAndDateRange(user, Transaction.TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = calculateTotalByUserAndTypeAndDateRange(user, Transaction.TransactionType.EXPENSE, startDate, endDate);
        
        return totalIncome.subtract(totalExpenses);
    }
    
    /**
     * Validate transaction.
     * @param transaction the transaction to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction description is required");
        }
        
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than 0");
        }
        
        if (transaction.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        
        if (transaction.getTransactionDate() == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }
        
        if (transaction.getUser() == null) {
            throw new IllegalArgumentException("Transaction user is required");
        }
        
        // Validate category if provided
        if (transaction.getCategory() != null && transaction.getCategory().getId() != null) {
            Optional<Category> category = categoryRepository.findById(transaction.getCategory().getId());
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category not found with id: " + transaction.getCategory().getId());
            }
        }
    }
}
