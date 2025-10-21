package com.example.demo.application.service;

import com.example.demo.domain.entity.Transaction;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for Income vs Expenses analysis operations.
 * Follows SOLID principles and Clean Architecture by handling business logic.
 */
@Service
@Transactional
public class IncomeExpenseService {
    
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public IncomeExpenseService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Calculate total income for user in date range.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return total income
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalIncome(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.calculateTotalByUserAndTypeAndDateRange(
            user, Transaction.TransactionType.INCOME, startDate, endDate);
    }
    
    /**
     * Calculate total expenses for user in date range.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return total expenses
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalExpenses(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.calculateTotalByUserAndTypeAndDateRange(
            user, Transaction.TransactionType.EXPENSE, startDate, endDate);
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
        BigDecimal totalIncome = calculateTotalIncome(user, startDate, endDate);
        BigDecimal totalExpenses = calculateTotalExpenses(user, startDate, endDate);
        return totalIncome.subtract(totalExpenses);
    }
    
    /**
     * Calculate savings rate percentage.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return savings rate as percentage
     */
    @Transactional(readOnly = true)
    public double calculateSavingsRate(User user, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalIncome = calculateTotalIncome(user, startDate, endDate);
        BigDecimal totalExpenses = calculateTotalExpenses(user, startDate, endDate);
        
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        BigDecimal savings = totalIncome.subtract(totalExpenses);
        return savings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                     .multiply(BigDecimal.valueOf(100))
                     .doubleValue();
    }
    
    /**
     * Calculate expense ratio percentage.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return expense ratio as percentage
     */
    @Transactional(readOnly = true)
    public double calculateExpenseRatio(User user, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalIncome = calculateTotalIncome(user, startDate, endDate);
        BigDecimal totalExpenses = calculateTotalExpenses(user, startDate, endDate);
        
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        return totalExpenses.divide(totalIncome, 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100))
                           .doubleValue();
    }
    
    /**
     * Get monthly income vs expenses data for chart.
     * @param user the user
     * @param months number of months to analyze
     * @return MonthlyData list
     */
    @Transactional(readOnly = true)
    public List<MonthlyData> getMonthlyIncomeExpenseData(User user, int months) {
        List<MonthlyData> monthlyData = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();
        
        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime monthStart = endDate.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            BigDecimal monthlyIncome = calculateTotalIncome(user, monthStart, monthEnd);
            BigDecimal monthlyExpenses = calculateTotalExpenses(user, monthStart, monthEnd);
            BigDecimal monthlyNetWorth = monthlyIncome.subtract(monthlyExpenses);
            
            monthlyData.add(new MonthlyData(
                monthStart.getMonth().name() + " " + monthStart.getYear(),
                monthlyIncome,
                monthlyExpenses,
                monthlyNetWorth
            ));
        }
        
        return monthlyData;
    }
    
    /**
     * Get income vs expenses summary for current period.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return IncomeExpenseSummary
     */
    @Transactional(readOnly = true)
    public IncomeExpenseSummary getIncomeExpenseSummary(User user, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalIncome = calculateTotalIncome(user, startDate, endDate);
        BigDecimal totalExpenses = calculateTotalExpenses(user, startDate, endDate);
        BigDecimal netWorth = calculateNetWorth(user, startDate, endDate);
        double savingsRate = calculateSavingsRate(user, startDate, endDate);
        double expenseRatio = calculateExpenseRatio(user, startDate, endDate);
        
        return new IncomeExpenseSummary(totalIncome, totalExpenses, netWorth, savingsRate, expenseRatio);
    }
    
    /**
     * Get category-wise expense breakdown.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return Map of category name to expense amount
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getCategoryExpenseBreakdown(User user, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, BigDecimal> categoryExpenses = new HashMap<>();
        
        // Get all transactions in date range and filter for expenses
        List<Transaction> allTransactions = transactionRepository.findByUserAndDateRange(user, startDate, endDate);
        List<Transaction> expenses = allTransactions.stream()
            .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.toList());
        
        for (Transaction expense : expenses) {
            String categoryName = expense.getCategory() != null ? 
                expense.getCategory().getName() : "Uncategorized";
            
            categoryExpenses.merge(categoryName, expense.getAmount(), BigDecimal::add);
        }
        
        return categoryExpenses;
    }
    
    /**
     * Get financial health status based on savings rate.
     * @param savingsRate the savings rate percentage
     * @return FinancialHealthStatus
     */
    public FinancialHealthStatus getFinancialHealthStatus(double savingsRate) {
        if (savingsRate >= 20) {
            return FinancialHealthStatus.HEALTHY;
        } else if (savingsRate >= 10) {
            return FinancialHealthStatus.CAUTION;
        } else {
            return FinancialHealthStatus.CRITICAL;
        }
    }
    
    /**
     * Get period comparison (current vs previous period).
     * @param user the user
     * @param periodMonths number of months for each period
     * @return PeriodComparison
     */
    @Transactional(readOnly = true)
    public PeriodComparison getPeriodComparison(User user, int periodMonths) {
        LocalDateTime now = LocalDateTime.now();
        
        // Current period
        LocalDateTime currentStart = now.minusMonths(periodMonths);
        LocalDateTime currentEnd = now;
        IncomeExpenseSummary current = getIncomeExpenseSummary(user, currentStart, currentEnd);
        
        // Previous period
        LocalDateTime previousStart = currentStart.minusMonths(periodMonths);
        LocalDateTime previousEnd = currentStart;
        IncomeExpenseSummary previous = getIncomeExpenseSummary(user, previousStart, previousEnd);
        
        return new PeriodComparison(current, previous);
    }
    
    /**
     * Inner class representing monthly data for charts.
     */
    public static class MonthlyData {
        private final String month;
        private final BigDecimal income;
        private final BigDecimal expenses;
        private final BigDecimal netWorth;
        
        public MonthlyData(String month, BigDecimal income, BigDecimal expenses, BigDecimal netWorth) {
            this.month = month;
            this.income = income;
            this.expenses = expenses;
            this.netWorth = netWorth;
        }
        
        public String getMonth() { return month; }
        public BigDecimal getIncome() { return income; }
        public BigDecimal getExpenses() { return expenses; }
        public BigDecimal getNetWorth() { return netWorth; }
    }
    
    /**
     * Inner class representing income vs expenses summary.
     */
    public static class IncomeExpenseSummary {
        private final BigDecimal totalIncome;
        private final BigDecimal totalExpenses;
        private final BigDecimal netWorth;
        private final double savingsRate;
        private final double expenseRatio;
        
        public IncomeExpenseSummary(BigDecimal totalIncome, BigDecimal totalExpenses, 
                                  BigDecimal netWorth, double savingsRate, double expenseRatio) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netWorth = netWorth;
            this.savingsRate = savingsRate;
            this.expenseRatio = expenseRatio;
        }
        
        public BigDecimal getTotalIncome() { return totalIncome; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public BigDecimal getNetWorth() { return netWorth; }
        public double getSavingsRate() { return savingsRate; }
        public double getExpenseRatio() { return expenseRatio; }
    }
    
    /**
     * Inner class representing financial health status.
     */
    public enum FinancialHealthStatus {
        HEALTHY("Healthy", "text-success", "fas fa-check-circle"),
        CAUTION("Caution", "text-warning", "fas fa-exclamation-triangle"),
        CRITICAL("Critical", "text-danger", "fas fa-times-circle");
        
        private final String displayName;
        private final String cssClass;
        private final String iconClass;
        
        FinancialHealthStatus(String displayName, String cssClass, String iconClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.iconClass = iconClass;
        }
        
        public String getDisplayName() { return displayName; }
        public String getCssClass() { return cssClass; }
        public String getIconClass() { return iconClass; }
    }
    
    /**
     * Inner class representing period comparison.
     */
    public static class PeriodComparison {
        private final IncomeExpenseSummary current;
        private final IncomeExpenseSummary previous;
        
        public PeriodComparison(IncomeExpenseSummary current, IncomeExpenseSummary previous) {
            this.current = current;
            this.previous = previous;
        }
        
        public IncomeExpenseSummary getCurrent() { return current; }
        public IncomeExpenseSummary getPrevious() { return previous; }
        
        public double getIncomeGrowthRate() {
            if (previous == null || previous.getTotalIncome().compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }
            return current.getTotalIncome().subtract(previous.getTotalIncome())
                         .divide(previous.getTotalIncome(), 4, RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100))
                         .doubleValue();
        }
        
        public double getExpenseGrowthRate() {
            if (previous == null || previous.getTotalExpenses().compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }
            return current.getTotalExpenses().subtract(previous.getTotalExpenses())
                         .divide(previous.getTotalExpenses(), 4, RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100))
                         .doubleValue();
        }
    }
}
