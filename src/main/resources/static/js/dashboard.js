// Dashboard JavaScript functionality

function loadDashboard() {
    showLoading();
    
    Promise.all([
        makeApiRequest('/transactions?size=10'),
        makeApiRequest('/categories'),
        makeApiRequest('/budgets/current'),
        makeApiRequest('/transactions/summary?startDate=' + getStartOfMonth() + '&endDate=' + getEndOfMonth())
    ])
    .then(([transactionsData, categoriesData, budgetsData, summaryData]) => {
        const transactions = transactionsData.content || transactionsData;
        const categories = categoriesData;
        const budgets = budgetsData;
        const summary = summaryData;
        
        renderDashboard(transactions, categories, budgets, summary);
        hideLoading();
    })
    .catch(error => {
        hideLoading();
        showAlert('Failed to load dashboard: ' + error.message, 'danger');
    });
}

function renderDashboard(transactions, categories, budgets, summary) {
    const dashboardHtml = `
        <div class="row mb-4">
            <div class="col">
                <h2><i class="bi bi-speedometer2"></i> Dashboard</h2>
                <p class="text-muted">Welcome back, ${currentUser.firstName}! Here's your financial overview.</p>
            </div>
        </div>
        
        <!-- Financial Summary Cards -->
        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <i class="bi bi-arrow-up-circle fs-1 mb-2"></i>
                        <h5>Total Income</h5>
                        <h3>${formatCurrency(summary.totalIncome)}</h3>
                        <small>This month</small>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <i class="bi bi-arrow-down-circle fs-1 mb-2"></i>
                        <h5>Total Expenses</h5>
                        <h3>${formatCurrency(summary.totalExpenses)}</h3>
                        <small>This month</small>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
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
        </div>
        
        <div class="row">
            <!-- Recent Transactions -->
            <div class="col-lg-8">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5><i class="bi bi-clock-history"></i> Recent Transactions</h5>
                        <a href="#" onclick="showTransactions()" class="btn btn-sm btn-outline-primary">View All</a>
                    </div>
                    <div class="card-body p-0">
                        ${renderRecentTransactions(transactions, categories)}
                    </div>
                </div>
            </div>
            
            <!-- Quick Actions & Budgets -->
            <div class="col-lg-4">
                <!-- Quick Actions -->
                <div class="card mb-4">
                    <div class="card-header">
                        <h5><i class="bi bi-lightning"></i> Other Actions</h5>
                    </div>
                    <div class="card-body">
                        <div class="d-grid gap-2">
                            <a href="/income-expenses" class="btn btn-outline-primary">
                                <i class="bi bi-bar-chart"></i> Income vs Expenses
                            </a>
                            <a href="/expenses-by-category" class="btn btn-outline-primary">
                                <i class="bi bi-pie-chart"></i> Expenses by Category
                            </a>
                            <button class="btn btn-outline-primary" onclick="showBudgetModal()">
                                <i class="bi bi-pie-chart"></i> Add Budget
                            </button>
                        </div>
                    </div>
                </div>
                
            </div>
        </div>
        
    `;
    
    $('#content').html(dashboardHtml);
    
}

function renderRecentTransactions(transactions, categories) {
    if (transactions.length === 0) {
        return `
            <div class="text-center py-4">
                <i class="bi bi-inbox fs-1 text-muted"></i>
                <p class="text-muted mt-2">No recent transactions</p>
                <button class="btn btn-primary" onclick="showTransactionModal()">Add Your First Transaction</button>
            </div>
        `;
    }
    
    return `
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Description</th>
                        <th>Category</th>
                        <th>Amount</th>
                    </tr>
                </thead>
                <tbody>
                    ${transactions.map(transaction => {
                        const category = categories.find(c => c.id === transaction.categoryId);
                        const typeClass = transaction.transactionType === 'INCOME' ? 'text-success' : 'text-danger';
                        const typeIcon = transaction.transactionType === 'INCOME' ? 'bi-arrow-up' : 'bi-arrow-down';
                        
                        return `
                            <tr>
                                <td>${formatDate(transaction.transactionDate)}</td>
                                <td>${transaction.description}</td>
                                <td>
                                    ${category ? `<span class="badge" style="background-color: ${category.colorCode || '#667eea'}">${category.name}</span>` : '-'}
                                </td>
                                <td class="${typeClass}">
                                    <i class="bi ${typeIcon}"></i>
                                    ${transaction.transactionType === 'INCOME' ? '+' : '-'}${formatCurrency(transaction.amount)}
                                </td>
                            </tr>
                        `;
                    }).join('')}
                </tbody>
            </table>
        </div>
    `;
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
