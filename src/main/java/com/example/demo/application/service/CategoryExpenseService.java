package com.example.demo.application.service;

import com.example.demo.domain.entity.Category;
import com.example.demo.domain.entity.Transaction;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.repository.CategoryRepository;
import com.example.demo.domain.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for Category-based Expense analysis operations.
 * Follows SOLID principles and Clean Architecture by handling business logic.
 */
@Service
@Transactional
public class CategoryExpenseService {
    
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public CategoryExpenseService(TransactionRepository transactionRepository, 
                                 CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Get comprehensive category expense analysis.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return CategoryExpenseAnalysis
     */
    @Transactional(readOnly = true)
    public CategoryExpenseAnalysis getCategoryExpenseAnalysis(User user, LocalDateTime startDate, LocalDateTime endDate) {
        // Get all expense transactions in date range
        List<Transaction> allTransactions = transactionRepository.findByUserAndDateRange(user, startDate, endDate);
        List<Transaction> expenses = allTransactions.stream()
            .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.toList());
        
        // Group by category
        Map<String, CategoryExpenseData> categoryData = new HashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        for (Transaction expense : expenses) {
            String categoryName = expense.getCategory() != null ? 
                expense.getCategory().getName() : "Uncategorized";
            String categoryColor = expense.getCategory() != null ? 
                expense.getCategory().getColorCode() : "#6c757d";
            
            CategoryExpenseData data = categoryData.computeIfAbsent(categoryName, 
                k -> new CategoryExpenseData(categoryName, categoryColor, BigDecimal.ZERO, 0));
            
            data.addAmount(expense.getAmount());
            data.incrementTransactionCount();
            totalExpenses = totalExpenses.add(expense.getAmount());
        }
        
        // Calculate percentages and sort by amount
        final BigDecimal finalTotalExpenses = totalExpenses;
        List<CategoryExpenseData> sortedCategories = categoryData.values().stream()
            .peek(data -> data.calculatePercentage(finalTotalExpenses))
            .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
            .collect(Collectors.toList());
        
        return new CategoryExpenseAnalysis(sortedCategories, totalExpenses, expenses.size());
    }
    
    /**
     * Get monthly category trends for charts.
     * @param user the user
     * @param months number of months to analyze
     * @return MonthlyCategoryTrends
     */
    @Transactional(readOnly = true)
    public MonthlyCategoryTrends getMonthlyCategoryTrends(User user, int months) {
        Map<String, List<BigDecimal>> monthlyData = new HashMap<>();
        List<String> monthLabels = new ArrayList<>();
        
        LocalDateTime endDate = LocalDateTime.now();
        
        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime monthStart = endDate.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            String monthLabel = monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            monthLabels.add(monthLabel);
            
            // Get expenses for this month
            List<Transaction> allMonthTransactions = transactionRepository.findByUserAndDateRange(user, monthStart, monthEnd);
            List<Transaction> monthExpenses = allMonthTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE)
                .collect(Collectors.toList());
            
            // Group by category
            Map<String, BigDecimal> monthCategoryTotals = new HashMap<>();
            for (Transaction expense : monthExpenses) {
                String categoryName = expense.getCategory() != null ? 
                    expense.getCategory().getName() : "Uncategorized";
                monthCategoryTotals.merge(categoryName, expense.getAmount(), BigDecimal::add);
            }
            
            // Add to monthly data
            for (Map.Entry<String, BigDecimal> entry : monthCategoryTotals.entrySet()) {
                monthlyData.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }
        
        return new MonthlyCategoryTrends(monthlyData, monthLabels);
    }
    
    /**
     * Get category growth analysis (current vs previous period).
     * @param user the user
     * @param periodMonths number of months for each period
     * @return CategoryGrowthAnalysis
     */
    @Transactional(readOnly = true)
    public CategoryGrowthAnalysis getCategoryGrowthAnalysis(User user, int periodMonths) {
        LocalDateTime now = LocalDateTime.now();
        
        // Current period
        LocalDateTime currentStart = now.minusMonths(periodMonths);
        LocalDateTime currentEnd = now;
        CategoryExpenseAnalysis current = getCategoryExpenseAnalysis(user, currentStart, currentEnd);
        
        // Previous period
        LocalDateTime previousStart = currentStart.minusMonths(periodMonths);
        LocalDateTime previousEnd = currentStart;
        CategoryExpenseAnalysis previous = getCategoryExpenseAnalysis(user, previousStart, previousEnd);
        
        return new CategoryGrowthAnalysis(current, previous);
    }
    
    /**
     * Get top spending categories.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @param limit number of top categories to return
     * @return List of top categories
     */
    @Transactional(readOnly = true)
    public List<CategoryExpenseData> getTopSpendingCategories(User user, LocalDateTime startDate, 
                                                           LocalDateTime endDate, int limit) {
        CategoryExpenseAnalysis analysis = getCategoryExpenseAnalysis(user, startDate, endDate);
        return analysis.getCategories().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get category budget comparison.
     * @param user the user
     * @param startDate start date
     * @param endDate end date
     * @return List of budget comparisons
     */
    @Transactional(readOnly = true)
    public List<CategoryBudgetComparison> getCategoryBudgetComparison(User user, LocalDateTime startDate, LocalDateTime endDate) {
        // Get all active categories for user
        List<Category> categories = categoryRepository.findByUserAndIsActiveTrueOrderByName(user);
        List<CategoryBudgetComparison> comparisons = new ArrayList<>();
        
        for (Category category : categories) {
            // Get actual spending for this category
            BigDecimal actualSpending = transactionRepository.calculateTotalByUserAndCategoryAndDateRange(
                user, category.getId(), startDate, endDate);
            
            // Get budget for this category (if exists)
            BigDecimal budgetAmount = BigDecimal.ZERO; // This would need to be implemented based on your budget logic
            
            comparisons.add(new CategoryBudgetComparison(
                category.getName(),
                category.getColorCode(),
                actualSpending,
                budgetAmount
            ));
        }
        
        return comparisons.stream()
            .sorted((a, b) -> b.getActualSpending().compareTo(a.getActualSpending()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get category spending insights and recommendations.
     * @param analysis the category expense analysis
     * @return List of insights
     */
    public List<CategoryInsight> getCategoryInsights(CategoryExpenseAnalysis analysis) {
        List<CategoryInsight> insights = new ArrayList<>();
        
        if (analysis.getCategories().isEmpty()) {
            insights.add(new CategoryInsight(
                "No Data", 
                "You haven't recorded any expenses in this period.", 
                "info"
            ));
            return insights;
        }
        
        CategoryExpenseData topCategory = analysis.getCategories().get(0);
        double topCategoryPercentage = topCategory.getPercentage();
        
        // Top spending category insight
        if (topCategoryPercentage > 40) {
            insights.add(new CategoryInsight(
                "High Concentration", 
                topCategory.getName() + " accounts for " + String.format("%.1f", topCategoryPercentage) + "% of your expenses. Consider diversifying your spending.", 
                "warning"
            ));
        }
        
        // Uncategorized expenses insight
        Optional<CategoryExpenseData> uncategorized = analysis.getCategories().stream()
            .filter(cat -> "Uncategorized".equals(cat.getName()))
            .findFirst();
        
        if (uncategorized.isPresent() && uncategorized.get().getPercentage() > 20) {
            insights.add(new CategoryInsight(
                "Uncategorized Expenses", 
                "You have " + String.format("%.1f", uncategorized.get().getPercentage()) + "% uncategorized expenses. Consider creating categories for better tracking.", 
                "info"
            ));
        }
        
        // Spending distribution insight
        long categoriesWithSignificantSpending = analysis.getCategories().stream()
            .filter(cat -> cat.getPercentage() > 10)
            .count();
        
        if (categoriesWithSignificantSpending > 5) {
            insights.add(new CategoryInsight(
                "Diverse Spending", 
                "Your expenses are well distributed across " + categoriesWithSignificantSpending + " categories. Great job maintaining balanced spending!", 
                "success"
            ));
        }
        
        return insights;
    }
    
    /**
     * Inner class representing category expense data.
     */
    public static class CategoryExpenseData {
        private final String name;
        private final String colorCode;
        private BigDecimal amount;
        private int transactionCount;
        private double percentage;
        
        public CategoryExpenseData(String name, String colorCode, BigDecimal amount, int transactionCount) {
            this.name = name;
            this.colorCode = colorCode;
            this.amount = amount;
            this.transactionCount = transactionCount;
        }
        
        public void addAmount(BigDecimal amount) {
            this.amount = this.amount.add(amount);
        }
        
        public void incrementTransactionCount() {
            this.transactionCount++;
        }
        
        public void calculatePercentage(BigDecimal totalExpenses) {
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                this.percentage = amount.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            }
        }
        
        public BigDecimal getAverageTransactionAmount() {
            if (transactionCount == 0) {
                return BigDecimal.ZERO;
            }
            return amount.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP);
        }
        
        // Getters
        public String getName() { return name; }
        public String getColorCode() { return colorCode; }
        public BigDecimal getAmount() { return amount; }
        public int getTransactionCount() { return transactionCount; }
        public double getPercentage() { return percentage; }
    }
    
    /**
     * Inner class representing category expense analysis.
     */
    public static class CategoryExpenseAnalysis {
        private final List<CategoryExpenseData> categories;
        private final BigDecimal totalExpenses;
        private final int totalTransactions;
        
        public CategoryExpenseAnalysis(List<CategoryExpenseData> categories, BigDecimal totalExpenses, int totalTransactions) {
            this.categories = categories;
            this.totalExpenses = totalExpenses;
            this.totalTransactions = totalTransactions;
        }
        
        public List<CategoryExpenseData> getCategories() { return categories; }
        public BigDecimal getTotalExpenses() { return totalExpenses; }
        public int getTotalTransactions() { return totalTransactions; }
    }
    
    /**
     * Inner class representing monthly category trends.
     */
    public static class MonthlyCategoryTrends {
        private final Map<String, List<BigDecimal>> monthlyData;
        private final List<String> monthLabels;
        
        public MonthlyCategoryTrends(Map<String, List<BigDecimal>> monthlyData, List<String> monthLabels) {
            this.monthlyData = monthlyData;
            this.monthLabels = monthLabels;
        }
        
        public Map<String, List<BigDecimal>> getMonthlyData() { return monthlyData; }
        public List<String> getMonthLabels() { return monthLabels; }
    }
    
    /**
     * Inner class representing category growth analysis.
     */
    public static class CategoryGrowthAnalysis {
        private final CategoryExpenseAnalysis current;
        private final CategoryExpenseAnalysis previous;
        
        public CategoryGrowthAnalysis(CategoryExpenseAnalysis current, CategoryExpenseAnalysis previous) {
            this.current = current;
            this.previous = previous;
        }
        
        public CategoryExpenseAnalysis getCurrent() { return current; }
        public CategoryExpenseAnalysis getPrevious() { return previous; }
        
        public double getTotalGrowthRate() {
            if (previous == null || previous.getTotalExpenses().compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }
            return current.getTotalExpenses().subtract(previous.getTotalExpenses())
                .divide(previous.getTotalExpenses(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        }
    }
    
    /**
     * Inner class representing category budget comparison.
     */
    public static class CategoryBudgetComparison {
        private final String categoryName;
        private final String colorCode;
        private final BigDecimal actualSpending;
        private final BigDecimal budgetAmount;
        
        public CategoryBudgetComparison(String categoryName, String colorCode, 
                                      BigDecimal actualSpending, BigDecimal budgetAmount) {
            this.categoryName = categoryName;
            this.colorCode = colorCode;
            this.actualSpending = actualSpending;
            this.budgetAmount = budgetAmount;
        }
        
        public String getCategoryName() { return categoryName; }
        public String getColorCode() { return colorCode; }
        public BigDecimal getActualSpending() { return actualSpending; }
        public BigDecimal getBudgetAmount() { return budgetAmount; }
        
        public double getBudgetUtilization() {
            if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }
            return actualSpending.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        }
        
        public boolean isOverBudget() {
            return actualSpending.compareTo(budgetAmount) > 0;
        }
    }
    
    /**
     * Inner class representing category insights.
     */
    public static class CategoryInsight {
        private final String title;
        private final String message;
        private final String type; // success, warning, info, danger
        
        public CategoryInsight(String title, String message, String type) {
            this.title = title;
            this.message = message;
            this.type = type;
        }
        
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getType() { return type; }
    }
}
