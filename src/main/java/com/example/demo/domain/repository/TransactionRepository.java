package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Transaction;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Transaction entity.
 * Follows Clean Architecture principles and provides transaction-specific queries.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find all transactions for a specific user.
     * @param user the user
     * @param pageable pagination information
     * @return Page of transactions
     */
    Page<Transaction> findByUserOrderByTransactionDateDesc(User user, Pageable pageable);
    
    /**
     * Find transactions by user and date range.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return List of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndDateRange(@Param("user") User user, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find transactions by user and transaction type.
     * @param user the user
     * @param transactionType the transaction type
     * @param pageable pagination information
     * @return Page of transactions
     */
    Page<Transaction> findByUserAndTransactionTypeOrderByTransactionDateDesc(User user, 
                                                                           Transaction.TransactionType transactionType, 
                                                                           Pageable pageable);
    
    /**
     * Calculate total amount for user by transaction type in date range.
     * @param user the user
     * @param transactionType the transaction type
     * @param startDate start date
     * @param endDate end date
     * @return total amount
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.transactionType = :transactionType AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalByUserAndTypeAndDateRange(@Param("user") User user,
                                                      @Param("transactionType") Transaction.TransactionType transactionType,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find recent transactions for a user.
     * @param user the user
     * @param limit maximum number of transactions
     * @return List of recent transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.user = :user ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactionsByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * Find transactions by user and category.
     * @param user the user
     * @param categoryId the category ID
     * @param pageable pagination information
     * @return Page of transactions
     */
    Page<Transaction> findByUserAndCategoryIdOrderByTransactionDateDesc(User user, Long categoryId, Pageable pageable);
    
    /**
     * Calculate total amount by category for user in date range.
     * @param user the user
     * @param categoryId the category ID
     * @param startDate start date
     * @param endDate end date
     * @return total amount
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.category.id = :categoryId AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalByUserAndCategoryAndDateRange(@Param("user") User user,
                                                          @Param("categoryId") Long categoryId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);
}
