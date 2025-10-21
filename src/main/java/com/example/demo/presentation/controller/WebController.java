package com.example.demo.presentation.controller;

import com.example.demo.application.dto.TransactionDto;
import com.example.demo.application.dto.CategoryDto;
import com.example.demo.application.dto.BudgetDto;
import com.example.demo.application.dto.UserDto;
import com.example.demo.application.service.TransactionService;
import com.example.demo.application.service.CategoryService;
import com.example.demo.application.service.BudgetService;
import com.example.demo.application.service.UserService;
import com.example.demo.application.service.IncomeExpenseService;
import com.example.demo.application.service.CategoryExpenseService;
import com.example.demo.domain.entity.Transaction;
import com.example.demo.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Web Controller for Thymeleaf views.
 * Handles page rendering and form submissions for the web interface.
 */
@Controller
public class WebController {
    
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final BudgetService budgetService;
    private final UserService userService;
    private final IncomeExpenseService incomeExpenseService;
    private final CategoryExpenseService categoryExpenseService;
    
    @Autowired
    public WebController(TransactionService transactionService, 
                        CategoryService categoryService,
                        BudgetService budgetService,
                        UserService userService,
                        IncomeExpenseService incomeExpenseService,
                        CategoryExpenseService categoryExpenseService) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.budgetService = budgetService;
        this.userService = userService;
        this.incomeExpenseService = incomeExpenseService;
        this.categoryExpenseService = categoryExpenseService;
    }
    
    /**
     * Dashboard page.
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        // Get recent transactions
        Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionService.findByUser(user, pageable);
        
        // Get categories
        List<CategoryDto> categories = categoryService.findActiveByUser(user).stream()
                .map(category -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(category.getId());
                    dto.setName(category.getName());
                    dto.setColorCode(category.getColorCode());
                    return dto;
                })
                .toList();
        
        // Get current budgets
        List<BudgetDto> budgets = budgetService.findCurrentBudgets(user).stream()
                .map(budget -> {
                    BudgetDto dto = new BudgetDto();
                    dto.setId(budget.getId());
                    dto.setName(budget.getName());
                    dto.setBudgetAmount(budget.getBudgetAmount());
                    dto.setSpentAmount(budgetService.calculateSpentAmount(budget));
                    dto.setUtilizationPercentage(budget.getUtilizationPercentage(dto.getSpentAmount()));
                    dto.setOverBudget(dto.getSpentAmount().compareTo(budget.getBudgetAmount()) > 0);
                    return dto;
                })
                .toList();
        
        // Calculate financial summary for current month
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
        
        BigDecimal totalIncome = transactionService.calculateTotalByUserAndTypeAndDateRange(user, Transaction.TransactionType.INCOME, startOfMonth, endOfMonth);
        BigDecimal totalExpenses = transactionService.calculateTotalByUserAndTypeAndDateRange(user, Transaction.TransactionType.EXPENSE, startOfMonth, endOfMonth);
        BigDecimal netWorth = totalIncome.subtract(totalExpenses);
        
        model.addAttribute("currentUser", user);
        model.addAttribute("transactions", transactions.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("budgets", budgets);
        model.addAttribute("summary", new FinancialSummary(totalIncome, totalExpenses, netWorth));
        model.addAttribute("pageTitle", "Dashboard");
        
        return "pages/dashboard";
    }
    
    /**
     * Transactions page.
     */
    @GetMapping("/transactions")
    public String transactions(@AuthenticationPrincipal User user, 
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(required = false) String type,
                              @RequestParam(required = false) Long category,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              Model model) {
        
        Pageable pageable = PageRequest.of(page, 20, Sort.by("transactionDate").descending());
        Page<Transaction> transactions;
        
        if (type != null && !type.isEmpty()) {
            Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(type);
            transactions = transactionService.findByUserAndType(user, transactionType, pageable);
        } else {
            transactions = transactionService.findByUser(user, pageable);
        }
        
        // Get categories for filter dropdown
        List<CategoryDto> categories = categoryService.findActiveByUser(user).stream()
                .map(cat -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    dto.setColorCode(cat.getColorCode());
                    return dto;
                })
                .toList();
        
        // Calculate totals
        BigDecimal totalIncome = transactionService.calculateTotalByUserAndTypeAndDateRange(user, Transaction.TransactionType.INCOME, null, null);
        BigDecimal totalExpenses = transactionService.calculateTotalByUserAndTypeAndDateRange(user, Transaction.TransactionType.EXPENSE, null, null);
        BigDecimal netWorth = totalIncome.subtract(totalExpenses);
        
        // Create empty transaction object for modal form
        TransactionDto transaction = new TransactionDto();
        transaction.setTransactionDate(LocalDateTime.now());
        
        model.addAttribute("currentUser", user);
        model.addAttribute("transactions", transactions.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("transaction", transaction);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("netWorth", netWorth);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        model.addAttribute("type", type);
        model.addAttribute("category", category);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("pageTitle", "Transactions");
        
        return "pages/transactions";
    }
    
    /**
     * New transaction form.
     */
    @GetMapping("/transactions/new")
    public String newTransaction(@AuthenticationPrincipal User user, Model model) {
        List<CategoryDto> categories = categoryService.findActiveByUser(user).stream()
                .map(cat -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    return dto;
                })
                .toList();
        
        TransactionDto transaction = new TransactionDto();
        transaction.setTransactionDate(LocalDateTime.now());
        
        model.addAttribute("currentUser", user);
        model.addAttribute("transaction", transaction);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Add Transaction");
        
        return "pages/transaction-form";
    }
    
    /**
     * Edit transaction form.
     */
    @GetMapping("/transactions/{id}/edit")
    public String editTransaction(@PathVariable Long id, @AuthenticationPrincipal User user, Model model, RedirectAttributes redirectAttributes) {
        Transaction transaction = transactionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        
        List<CategoryDto> categories = categoryService.findActiveByUser(user).stream()
                .map(cat -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    return dto;
                })
                .toList();
        
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setId(transaction.getId());
        transactionDto.setDescription(transaction.getDescription());
        transactionDto.setAmount(transaction.getAmount());
        transactionDto.setTransactionType(transaction.getTransactionType());
        transactionDto.setTransactionDate(transaction.getTransactionDate());
        transactionDto.setNotes(transaction.getNotes());
        if (transaction.getCategory() != null) {
            transactionDto.setCategoryId(transaction.getCategory().getId());
        }
        
        model.addAttribute("currentUser", user);
        model.addAttribute("transaction", transactionDto);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Edit Transaction");
        
        return "pages/transaction-form";
    }
    
    /**
     * Save transaction.
     */
    @PostMapping("/transactions")
    public String saveTransaction(@ModelAttribute("transaction") TransactionDto transactionDto, 
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {
        try {
            Transaction transaction = new Transaction();
            transaction.setDescription(transactionDto.getDescription());
            transaction.setAmount(transactionDto.getAmount());
            transaction.setTransactionType(transactionDto.getTransactionType());
            transaction.setTransactionDate(transactionDto.getTransactionDate());
            transaction.setNotes(transactionDto.getNotes());
            transaction.setUser(user);
            
            if (transactionDto.getCategoryId() != null) {
                categoryService.findById(transactionDto.getCategoryId())
                        .ifPresent(transaction::setCategory);
            }
            
            if (transactionDto.getId() != null) {
                // Update existing transaction
                Transaction existingTransaction = transactionService.findById(transactionDto.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
                
                if (!existingTransaction.getUser().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Access denied");
                }
                
                existingTransaction.setDescription(transaction.getDescription());
                existingTransaction.setAmount(transaction.getAmount());
                existingTransaction.setTransactionType(transaction.getTransactionType());
                existingTransaction.setTransactionDate(transaction.getTransactionDate());
                existingTransaction.setNotes(transaction.getNotes());
                existingTransaction.setCategory(transaction.getCategory());
                
                transactionService.updateTransaction(existingTransaction);
                redirectAttributes.addFlashAttribute("successMessage", "Transaction updated successfully!");
            } else {
                // Create new transaction
                transactionService.createTransaction(transaction);
                redirectAttributes.addFlashAttribute("successMessage", "Transaction created successfully!");
            }
            
            return "redirect:/transactions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save transaction: " + e.getMessage());
            return "redirect:/transactions";
        }
    }
    
    /**
     * Delete transaction.
     */
    @GetMapping("/transactions/{id}/delete")
    public String deleteTransaction(@PathVariable Long id, @AuthenticationPrincipal User user,
                                   RedirectAttributes redirectAttributes) {
        try {
            Transaction transaction = transactionService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
            
            if (!transaction.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Access denied");
            }
            
            transactionService.deleteTransaction(id);
            redirectAttributes.addFlashAttribute("successMessage", "Transaction deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete transaction: " + e.getMessage());
        }
        
        return "redirect:/transactions";
    }
    
    /**
     * Budgets page.
     */
    @GetMapping("/budgets")
    public String budgets(@AuthenticationPrincipal User user, Model model) {
        // Get all budgets for user
        List<BudgetDto> budgets = budgetService.findByUser(user).stream()
                .map(budget -> {
                    BudgetDto dto = new BudgetDto();
                    dto.setId(budget.getId());
                    dto.setName(budget.getName());
                    dto.setDescription(budget.getDescription());
                    dto.setBudgetAmount(budget.getBudgetAmount());
                    dto.setStartDate(budget.getStartDate());
                    dto.setEndDate(budget.getEndDate());
                    dto.setIsActive(budget.getIsActive());
                    dto.setSpentAmount(budgetService.calculateSpentAmount(budget));
                    dto.setUtilizationPercentage(budget.getUtilizationPercentage(dto.getSpentAmount()));
                    dto.setOverBudget(dto.getSpentAmount().compareTo(budget.getBudgetAmount()) > 0);
                    return dto;
                })
                .toList();
        
        // Get categories for budget form
        List<CategoryDto> categories = categoryService.findActiveByUser(user).stream()
                .map(cat -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    dto.setColorCode(cat.getColorCode());
                    return dto;
                })
                .toList();
        
        // Create empty budget object for modal form
        BudgetDto budget = new BudgetDto();
        budget.setStartDate(LocalDateTime.now().toLocalDate());
        budget.setEndDate(LocalDateTime.now().plusMonths(1).toLocalDate());
        budget.setIsActive(true);
        
        // Get current budgets for summary cards
        List<BudgetDto> currentBudgets = budgetService.findCurrentBudgets(user).stream()
                .map(budgetEntity -> {
                    BudgetDto dto = new BudgetDto();
                    dto.setId(budgetEntity.getId());
                    dto.setName(budgetEntity.getName());
                    dto.setBudgetAmount(budgetEntity.getBudgetAmount());
                    dto.setSpentAmount(budgetService.calculateSpentAmount(budgetEntity));
                    dto.setUtilizationPercentage(budgetEntity.getUtilizationPercentage(dto.getSpentAmount()));
                    dto.setOverBudget(dto.getSpentAmount().compareTo(budgetEntity.getBudgetAmount()) > 0);
                    return dto;
                })
                .toList();
        
        // Calculate summary statistics
        BigDecimal totalBudget = currentBudgets.stream()
                .map(BudgetDto::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double averageUtilization = currentBudgets.isEmpty() ? 0.0 : 
                currentBudgets.stream()
                        .mapToDouble(BudgetDto::getUtilizationPercentage)
                        .average()
                        .orElse(0.0);
        
        long overBudgetCount = currentBudgets.stream()
                .mapToLong(budgetDto -> budgetDto.isOverBudget() ? 1 : 0)
                .sum();
        
        model.addAttribute("currentUser", user);
        model.addAttribute("budgets", budgets);
        model.addAttribute("currentBudgets", currentBudgets);
        model.addAttribute("categories", categories);
        model.addAttribute("budget", budget);
        model.addAttribute("totalBudget", totalBudget);
        model.addAttribute("averageUtilization", averageUtilization);
        model.addAttribute("overBudgetCount", overBudgetCount);
        model.addAttribute("pageTitle", "Budgets");
        
        return "pages/budgets";
    }
    
    /**
     * Budget form page (new budget).
     */
    @GetMapping("/budgets/new")
    public String newBudget(@AuthenticationPrincipal User user, Model model) {
        // Get categories for budget form
        List<CategoryDto> categories = categoryService.findActiveByUser(user).stream()
                .map(cat -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    dto.setColorCode(cat.getColorCode());
                    return dto;
                })
                .toList();
        
        // Create empty budget object for form
        BudgetDto budget = new BudgetDto();
        budget.setStartDate(LocalDateTime.now().toLocalDate());
        budget.setEndDate(LocalDateTime.now().plusMonths(1).toLocalDate());
        budget.setIsActive(true);
        
        model.addAttribute("currentUser", user);
        model.addAttribute("budget", budget);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Create New Budget");
        
        return "pages/budget-form";
    }
    
    /**
     * Income vs Expenses analysis page.
     */
    @GetMapping("/income-expenses")
    public String incomeExpenses(@AuthenticationPrincipal User user, Model model) {
        // Get current month data
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
        
        // Get income vs expenses summary
        IncomeExpenseService.IncomeExpenseSummary summary = incomeExpenseService.getIncomeExpenseSummary(user, monthStart, monthEnd);
        
        // Get monthly data for charts (last 6 months)
        List<IncomeExpenseService.MonthlyData> monthlyData = incomeExpenseService.getMonthlyIncomeExpenseData(user, 6);
        
        // Get category expense breakdown
        Map<String, BigDecimal> categoryExpenses = incomeExpenseService.getCategoryExpenseBreakdown(user, monthStart, monthEnd);
        
        // Get period comparison (current vs previous month)
        IncomeExpenseService.PeriodComparison comparison = incomeExpenseService.getPeriodComparison(user, 1);
        
        // Get financial health status
        IncomeExpenseService.FinancialHealthStatus healthStatus = incomeExpenseService.getFinancialHealthStatus(summary.getSavingsRate());
        
        model.addAttribute("currentUser", user);
        model.addAttribute("summary", summary);
        model.addAttribute("monthlyData", monthlyData);
        model.addAttribute("categoryExpenses", categoryExpenses);
        model.addAttribute("comparison", comparison);
        model.addAttribute("healthStatus", healthStatus);
        model.addAttribute("pageTitle", "Income vs Expenses Analysis");
        
        return "pages/income-expenses";
    }
    
    /**
     * Expenses by Category analysis page.
     */
    @GetMapping("/expenses-by-category")
    public String expensesByCategory(@AuthenticationPrincipal User user, Model model) {
        // Get current month data
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
        
        // Get category expense analysis
        CategoryExpenseService.CategoryExpenseAnalysis analysis = categoryExpenseService.getCategoryExpenseAnalysis(user, monthStart, monthEnd);
        
        // Get monthly trends for charts (last 6 months)
        CategoryExpenseService.MonthlyCategoryTrends trends = categoryExpenseService.getMonthlyCategoryTrends(user, 6);
        
        // Get category growth analysis (current vs previous month)
        CategoryExpenseService.CategoryGrowthAnalysis growth = categoryExpenseService.getCategoryGrowthAnalysis(user, 1);
        
        // Get top spending categories
        List<CategoryExpenseService.CategoryExpenseData> topCategories = categoryExpenseService.getTopSpendingCategories(user, monthStart, monthEnd, 10);
        
        // Get category insights
        List<CategoryExpenseService.CategoryInsight> insights = categoryExpenseService.getCategoryInsights(analysis);
        
        model.addAttribute("currentUser", user);
        model.addAttribute("analysis", analysis);
        model.addAttribute("trends", trends);
        model.addAttribute("growth", growth);
        model.addAttribute("topCategories", topCategories);
        model.addAttribute("insights", insights);
        model.addAttribute("pageTitle", "Expenses by Category Analysis");
        
        return "pages/expenses-by-category";
    }
    
    /**
     * Categories page.
     */
    @GetMapping("/categories")
    public String categories(@AuthenticationPrincipal User user, Model model) {
        // Get all categories for user
        List<CategoryDto> categories = categoryService.findByUser(user).stream()
                .map(category -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(category.getId());
                    dto.setName(category.getName());
                    dto.setDescription(category.getDescription());
                    dto.setColorCode(category.getColorCode());
                    dto.setIsActive(category.getIsActive());
                    return dto;
                })
                .toList();
        
        // Create empty category object for modal form
        CategoryDto category = new CategoryDto();
        category.setIsActive(true);
        category.setColorCode("#007bff"); // Default blue color
        
        model.addAttribute("currentUser", user);
        model.addAttribute("categories", categories);
        model.addAttribute("category", category);
        model.addAttribute("pageTitle", "Categories");
        
        return "pages/categories";
    }
    
    /**
     * Login page.
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, 
                       @RequestParam(required = false) String logout, 
                       Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully!");
        }
        model.addAttribute("pageTitle", "Login");
        return "pages/login";
    }
    
    /**
     * Profile overview page.
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Profile Overview");
        
        // Add profile data for freelancer showcase
        model.addAttribute("experience", "9 years");
        model.addAttribute("specialization", "Full Stack Java Development");
        model.addAttribute("availability", "Available for Freelance Projects");
        
        return "pages/profile";
    }
    
    /**
     * Logout handler.
     */
    @PostMapping("/logout")
    public String logout() {
        return "redirect:/login?logout=true";
    }
    
    /**
     * Register page.
     */
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new UserDto());
        model.addAttribute("pageTitle", "Register");
        return "pages/register";
    }
    
    /**
     * Inner class for financial summary.
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
