// Reports JavaScript functionality

function loadReports() {
    showLoading();
    
    const startDate = getStartOfMonth();
    const endDate = getEndOfMonth();
    
    Promise.all([
        makeApiRequest(`/transactions/date-range?startDate=${startDate}&endDate=${endDate}`),
        makeApiRequest('/categories'),
        makeApiRequest('/budgets'),
        makeApiRequest(`/transactions/summary?startDate=${startDate}&endDate=${endDate}`)
    ])
    .then(([transactionsData, categoriesData, budgetsData, summaryData]) => {
        const transactions = transactionsData;
        const categories = categoriesData;
        const budgets = budgetsData;
        const summary = summaryData;
        
        renderReports(transactions, categories, budgets, summary);
        hideLoading();
    })
    .catch(error => {
        hideLoading();
        showAlert('Failed to load reports: ' + error.message, 'danger');
    });
}

function renderReports(transactions, categories, budgets, summary) {
    const reportsHtml = `
        <div class="row mb-4">
            <div class="col">
                <h2><i class="bi bi-graph-up"></i> Reports & Analytics</h2>
                <p class="text-muted">Detailed financial analysis and insights</p>
            </div>
            <div class="col-auto">
                <div class="btn-group" role="group">
                    <button class="btn btn-outline-primary active" onclick="showMonthlyReport()">Monthly</button>
                    <button class="btn btn-outline-primary" onclick="showYearlyReport()">Yearly</button>
                    <button class="btn btn-outline-primary" onclick="showCustomReport()">Custom</button>
                </div>
            </div>
        </div>
        
        <!-- Financial Summary -->
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <i class="bi bi-arrow-up-circle fs-1 mb-2"></i>
                        <h5>Total Income</h5>
                        <h3>${formatCurrency(summary.totalIncome)}</h3>
                        <small>This month</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <i class="bi bi-arrow-down-circle fs-1 mb-2"></i>
                        <h5>Total Expenses</h5>
                        <h3>${formatCurrency(summary.totalExpenses)}</h3>
                        <small>This month</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <i class="bi bi-wallet2 fs-1 mb-2"></i>
                        <h5>Net Worth</h5>
                        <h3 class="${summary.netWorth >= 0 ? 'text-success' : 'text-danger'}">
                            ${formatCurrency(summary.netWorth)}
                        </h3>
                        <small>This month</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <i class="bi bi-graph-up fs-1 mb-2"></i>
                        <h5>Savings Rate</h5>
                        <h3>${calculateSavingsRate(summary)}%</h3>
                        <small>This month</small>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Charts -->
        <div class="row mb-4">
            <div class="col-lg-8">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-bar-chart"></i> Monthly Trend</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="monthlyTrendChart" width="400" height="200"></canvas>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-pie-chart"></i> Expense Categories</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="expenseCategoryChart" width="400" height="200"></canvas>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Detailed Analysis -->
        <div class="row">
            <div class="col-lg-6">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-list-ul"></i> Top Expense Categories</h5>
                    </div>
                    <div class="card-body">
                        ${renderTopExpenseCategories(transactions, categories)}
                    </div>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-calendar-check"></i> Budget Performance</h5>
                    </div>
                    <div class="card-body">
                        ${renderBudgetPerformance(budgets)}
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Transaction Analysis -->
        <div class="row mt-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-table"></i> Transaction Analysis</h5>
                    </div>
                    <div class="card-body">
                        ${renderTransactionAnalysis(transactions)}
                    </div>
                </div>
            </div>
        </div>
    `;
    
    $('#content').html(reportsHtml);
    
    // Initialize charts after DOM is updated
    setTimeout(() => {
        initializeReportCharts(transactions, categories, summary);
    }, 100);
}

function renderTopExpenseCategories(transactions, categories) {
    const expenseTransactions = transactions.filter(t => t.transactionType === 'EXPENSE');
    const categoryTotals = {};
    
    expenseTransactions.forEach(transaction => {
        const categoryId = transaction.categoryId;
        if (categoryId) {
            const category = categories.find(c => c.id === categoryId);
            const categoryName = category ? category.name : 'Uncategorized';
            categoryTotals[categoryName] = (categoryTotals[categoryName] || 0) + parseFloat(transaction.amount);
        }
    });
    
    const sortedCategories = Object.entries(categoryTotals)
        .sort(([,a], [,b]) => b - a)
        .slice(0, 5);
    
    if (sortedCategories.length === 0) {
        return '<p class="text-muted">No expense data available</p>';
    }
    
    return sortedCategories.map(([categoryName, total]) => {
        const percentage = ((total / Object.values(categoryTotals).reduce((a, b) => a + b, 0)) * 100).toFixed(1);
        return `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <div class="fw-bold">${categoryName}</div>
                    <small class="text-muted">${percentage}% of total expenses</small>
                </div>
                <div class="text-end">
                    <div class="fw-bold">${formatCurrency(total)}</div>
                </div>
            </div>
        `;
    }).join('');
}

function renderBudgetPerformance(budgets) {
    const activeBudgets = budgets.filter(b => b.isActive);
    
    if (activeBudgets.length === 0) {
        return '<p class="text-muted">No active budgets</p>';
    }
    
    return activeBudgets.map(budget => {
        const utilizationPercentage = budget.utilizationPercentage || 0;
        const isOverBudget = budget.overBudget || false;
        const statusClass = isOverBudget ? 'text-danger' : (utilizationPercentage > 80 ? 'text-warning' : 'text-success');
        const statusIcon = isOverBudget ? 'bi-exclamation-triangle' : (utilizationPercentage > 80 ? 'bi-exclamation-circle' : 'bi-check-circle');
        
        return `
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <div class="fw-bold">${budget.name}</div>
                    <small class="text-muted">${formatDate(budget.startDate)} - ${formatDate(budget.endDate)}</small>
                </div>
                <div class="text-end">
                    <div class="${statusClass}">
                        <i class="bi ${statusIcon}"></i>
                        ${utilizationPercentage.toFixed(1)}%
                    </div>
                    <small class="text-muted">${formatCurrency(budget.spentAmount || 0)} / ${formatCurrency(budget.budgetAmount)}</small>
                </div>
            </div>
        `;
    }).join('');
}

function renderTransactionAnalysis(transactions) {
    const totalTransactions = transactions.length;
    const incomeTransactions = transactions.filter(t => t.transactionType === 'INCOME');
    const expenseTransactions = transactions.filter(t => t.transactionType === 'EXPENSE');
    
    const avgIncome = incomeTransactions.length > 0 ? 
        incomeTransactions.reduce((sum, t) => sum + parseFloat(t.amount), 0) / incomeTransactions.length : 0;
    const avgExpense = expenseTransactions.length > 0 ? 
        expenseTransactions.reduce((sum, t) => sum + parseFloat(t.amount), 0) / expenseTransactions.length : 0;
    
    return `
        <div class="row">
            <div class="col-md-3">
                <div class="text-center">
                    <h4>${totalTransactions}</h4>
                    <small class="text-muted">Total Transactions</small>
                </div>
            </div>
            <div class="col-md-3">
                <div class="text-center">
                    <h4>${incomeTransactions.length}</h4>
                    <small class="text-muted">Income Transactions</small>
                </div>
            </div>
            <div class="col-md-3">
                <div class="text-center">
                    <h4>${expenseTransactions.length}</h4>
                    <small class="text-muted">Expense Transactions</small>
                </div>
            </div>
            <div class="col-md-3">
                <div class="text-center">
                    <h4>${formatCurrency(avgExpense)}</h4>
                    <small class="text-muted">Average Expense</small>
                </div>
            </div>
        </div>
    `;
}

function initializeReportCharts(transactions, categories, summary) {
    // Monthly Trend Chart
    const monthlyTrendCtx = document.getElementById('monthlyTrendChart');
    if (monthlyTrendCtx) {
        new Chart(monthlyTrendCtx, {
            type: 'line',
            data: {
                labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
                datasets: [{
                    label: 'Income',
                    data: [summary.totalIncome * 0.3, summary.totalIncome * 0.2, summary.totalIncome * 0.3, summary.totalIncome * 0.2],
                    borderColor: '#28a745',
                    backgroundColor: 'rgba(40, 167, 69, 0.1)',
                    tension: 0.4
                }, {
                    label: 'Expenses',
                    data: [summary.totalExpenses * 0.2, summary.totalExpenses * 0.3, summary.totalExpenses * 0.3, summary.totalExpenses * 0.2],
                    borderColor: '#dc3545',
                    backgroundColor: 'rgba(220, 53, 69, 0.1)',
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return '$' + value.toLocaleString();
                            }
                        }
                    }
                }
            }
        });
    }
    
    // Expense Category Chart
    const expenseCategoryCtx = document.getElementById('expenseCategoryChart');
    if (expenseCategoryCtx) {
        const expenseTransactions = transactions.filter(t => t.transactionType === 'EXPENSE');
        const categoryTotals = {};
        
        expenseTransactions.forEach(transaction => {
            const categoryId = transaction.categoryId;
            if (categoryId) {
                const category = categories.find(c => c.id === categoryId);
                const categoryName = category ? category.name : 'Uncategorized';
                categoryTotals[categoryName] = (categoryTotals[categoryName] || 0) + parseFloat(transaction.amount);
            }
        });
        
        const topCategories = Object.entries(categoryTotals)
            .sort(([,a], [,b]) => b - a)
            .slice(0, 5);
        
        new Chart(expenseCategoryCtx, {
            type: 'doughnut',
            data: {
                labels: topCategories.map(([name]) => name),
                datasets: [{
                    data: topCategories.map(([, total]) => total),
                    backgroundColor: ['#667eea', '#764ba2', '#f093fb', '#f5576c', '#4facfe']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    }
}

function calculateSavingsRate(summary) {
    if (summary.totalIncome === 0) return 0;
    return ((summary.netWorth / summary.totalIncome) * 100).toFixed(1);
}

function showMonthlyReport() {
    // Switch to monthly view
    $('.btn-group .btn').removeClass('active');
    $('.btn-group .btn:first').addClass('active');
    loadReports();
}

function showYearlyReport() {
    // Switch to yearly view
    $('.btn-group .btn').removeClass('active');
    $('.btn-group .btn:nth-child(2)').addClass('active');
    // Implementation for yearly report
    showAlert('Yearly report feature coming soon!', 'info');
}

function showCustomReport() {
    // Switch to custom view
    $('.btn-group .btn').removeClass('active');
    $('.btn-group .btn:last').addClass('active');
    // Implementation for custom report
    showAlert('Custom report feature coming soon!', 'info');
}

function getStartOfMonth() {
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    return startOfMonth.toISOString();
}

function getEndOfMonth() {
    const now = new Date();
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);
    return endOfMonth.toISOString();
}
