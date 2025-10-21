package com.example.demo.presentation.controller;

import com.example.demo.application.dto.TransactionDto;
import com.example.demo.application.mapper.TransactionMapper;
import com.example.demo.application.service.TransactionService;
import com.example.demo.domain.entity.Transaction;
import com.example.demo.domain.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Transaction operations.
 * Follows Clean Architecture principles and provides RESTful API endpoints.
 */
@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    
    @Autowired
    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }
    
    /**
     * Create a new transaction.
     * @param transactionDto the transaction data
     * @param user the authenticated user
     * @return created transaction
     */
    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(@Valid @RequestBody TransactionDto transactionDto,
                                                          @AuthenticationPrincipal User user) {
        try {
            Transaction transaction = transactionMapper.toEntity(transactionDto);
            transaction.setUser(user);
            
            Transaction createdTransaction = transactionService.createTransaction(transaction);
            TransactionDto responseDto = transactionMapper.toDto(createdTransaction);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get transaction by ID.
     * @param id the transaction ID
     * @param user the authenticated user
     * @return transaction if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable Long id,
                                                       @AuthenticationPrincipal User user) {
        Optional<Transaction> transaction = transactionService.findById(id);
        
        if (transaction.isEmpty() || !transaction.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        TransactionDto responseDto = transactionMapper.toDto(transaction.get());
        return ResponseEntity.ok(responseDto);
    }
    
    /**
     * Get transactions for user with pagination.
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @param user the authenticated user
     * @return page of transactions
     */
    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal User user) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Transaction> transactions = transactionService.findByUser(user, pageable);
        Page<TransactionDto> responseDtos = transactions.map(transactionMapper::toDto);
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get transactions by date range.
     * @param startDate start date
     * @param endDate end date
     * @param user the authenticated user
     * @return list of transactions
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionDto>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal User user) {
        
        List<Transaction> transactions = transactionService.findByUserAndDateRange(user, startDate, endDate);
        List<TransactionDto> responseDtos = transactions.stream()
                .map(transactionMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get transactions by type.
     * @param type the transaction type
     * @param page page number (0-based)
     * @param size page size
     * @param user the authenticated user
     * @return page of transactions
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByType(
            @PathVariable Transaction.TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.findByUserAndType(user, type, pageable);
        Page<TransactionDto> responseDtos = transactions.map(transactionMapper::toDto);
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Get transactions by category.
     * @param categoryId the category ID
     * @param page page number (0-based)
     * @param size page size
     * @param user the authenticated user
     * @return page of transactions
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.findByUserAndCategory(user, categoryId, pageable);
        Page<TransactionDto> responseDtos = transactions.map(transactionMapper::toDto);
        
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * Update transaction.
     * @param id the transaction ID
     * @param transactionDto the updated transaction data
     * @param user the authenticated user
     * @return updated transaction
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(@PathVariable Long id,
                                                          @Valid @RequestBody TransactionDto transactionDto,
                                                          @AuthenticationPrincipal User user) {
        try {
            Optional<Transaction> existingTransaction = transactionService.findById(id);
            
            if (existingTransaction.isEmpty() || !existingTransaction.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            Transaction transaction = existingTransaction.get();
            transactionMapper.updateEntity(transactionDto, transaction);
            
            Transaction updatedTransaction = transactionService.updateTransaction(transaction);
            TransactionDto responseDto = transactionMapper.toDto(updatedTransaction);
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete transaction.
     * @param id the transaction ID
     * @param user the authenticated user
     * @return no content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id,
                                                @AuthenticationPrincipal User user) {
        try {
            Optional<Transaction> transaction = transactionService.findById(id);
            
            if (transaction.isEmpty() || !transaction.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            transactionService.deleteTransaction(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get financial summary for user.
     * @param startDate start date
     * @param endDate end date
     * @param user the authenticated user
     * @return financial summary
     */
    @GetMapping("/summary")
    public ResponseEntity<FinancialSummary> getFinancialSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal User user) {
        
        BigDecimal totalIncome = transactionService.calculateTotalByUserAndTypeAndDateRange(
            user, Transaction.TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = transactionService.calculateTotalByUserAndTypeAndDateRange(
            user, Transaction.TransactionType.EXPENSE, startDate, endDate);
        BigDecimal netWorth = transactionService.calculateNetWorth(user, startDate, endDate);
        
        FinancialSummary summary = new FinancialSummary(totalIncome, totalExpenses, netWorth);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Inner class for financial summary response.
     */
    public static class FinancialSummary {
        private final BigDecimal totalIncome;
        private final BigDecimal totalExpenses;
        private final BigDecimal netWorth;
        
        public FinancialSummary(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netWorth) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netWorth = netWorth;
        }
        
        public BigDecimal getTotalIncome() {
            return totalIncome;
        }
        
        public BigDecimal getTotalExpenses() {
            return totalExpenses;
        }
        
        public BigDecimal getNetWorth() {
            return netWorth;
        }
    }
}
