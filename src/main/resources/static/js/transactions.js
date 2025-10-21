// Transactions JavaScript functionality

let transactions = [];
let categories = [];

function loadTransactions() {
    showLoading();
    
    Promise.all([
        makeApiRequest('/transactions?size=100'),
        makeApiRequest('/categories')
    ])
    .then(([transactionsData, categoriesData]) => {
        transactions = transactionsData.content || transactionsData;
        categories = categoriesData;
        
        renderTransactions();
        populateCategorySelects();
        hideLoading();
    })
    .catch(error => {
        hideLoading();
        showAlert('Failed to load transactions: ' + error.message, 'danger');
    });
}

function renderTransactions() {
    const transactionsHtml = `
        <div class="row mb-4">
            <div class="col">
                <h2><i class="bi bi-arrow-left-right"></i> Transactions</h2>
            </div>
            <div class="col-auto">
                <button class="btn btn-primary" onclick="showTransactionModal()">
                    <i class="bi bi-plus"></i> Add Transaction
                </button>
            </div>
        </div>
        
        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <h5>Total Income</h5>
                        <h3>${formatCurrency(calculateTotalIncome())}</h3>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <h5>Total Expenses</h5>
                        <h3>${formatCurrency(calculateTotalExpenses())}</h3>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <h5>Net Worth</h5>
                        <h3>${formatCurrency(calculateNetWorth())}</h3>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="card">
            <div class="card-header">
                <div class="row align-items-center">
                    <div class="col">
                        <h5 class="mb-0">Recent Transactions</h5>
                    </div>
                    <div class="col-auto">
                        <div class="btn-group" role="group">
                            <button class="btn btn-outline-primary btn-sm" onclick="filterTransactions('all')">All</button>
                            <button class="btn btn-outline-primary btn-sm" onclick="filterTransactions('INCOME')">Income</button>
                            <button class="btn btn-outline-primary btn-sm" onclick="filterTransactions('EXPENSE')">Expenses</button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Description</th>
                                <th>Category</th>
                                <th>Type</th>
                                <th>Amount</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="transactionsTableBody">
                            ${renderTransactionsTable()}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    `;
    
    $('#content').html(transactionsHtml);
}

function renderTransactionsTable() {
    if (transactions.length === 0) {
        return `
            <tr>
                <td colspan="6" class="text-center py-4">
                    <i class="bi bi-inbox"></i> No transactions found
                </td>
            </tr>
        `;
    }
    
    return transactions.map(transaction => {
        const category = categories.find(c => c.id === transaction.categoryId);
        const typeClass = transaction.transactionType === 'INCOME' ? 'text-success' : 'text-danger';
        const typeIcon = transaction.transactionType === 'INCOME' ? 'bi-arrow-up' : 'bi-arrow-down';
        
        return `
            <tr>
                <td>${formatDateTime(transaction.transactionDate)}</td>
                <td>${transaction.description}</td>
                <td>
                    ${category ? `<span class="badge" style="background-color: ${category.colorCode || '#667eea'}">${category.name}</span>` : '-'}
                </td>
                <td>
                    <i class="bi ${typeIcon} ${typeClass}"></i>
                    ${transaction.transactionType}
                </td>
                <td class="${typeClass}">
                    ${transaction.transactionType === 'INCOME' ? '+' : '-'}${formatCurrency(transaction.amount)}
                </td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" onclick="editTransaction(${transaction.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-outline-danger" onclick="deleteTransaction(${transaction.id})">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function calculateTotalIncome() {
    return transactions
        .filter(t => t.transactionType === 'INCOME')
        .reduce((sum, t) => sum + parseFloat(t.amount), 0);
}

function calculateTotalExpenses() {
    return transactions
        .filter(t => t.transactionType === 'EXPENSE')
        .reduce((sum, t) => sum + parseFloat(t.amount), 0);
}

function calculateNetWorth() {
    return calculateTotalIncome() - calculateTotalExpenses();
}

function filterTransactions(type) {
    // This would filter the transactions based on type
    // For now, we'll just reload all transactions
    loadTransactions();
}

function editTransaction(id) {
    const transaction = transactions.find(t => t.id === id);
    if (transaction) {
        showTransactionModal(transaction);
    }
}

function deleteTransaction(id) {
    if (confirm('Are you sure you want to delete this transaction?')) {
        makeApiRequest(`/transactions/${id}`, {
            method: 'DELETE'
        })
        .then(() => {
            showAlert('Transaction deleted successfully', 'success');
            loadTransactions();
        })
        .catch(error => {
            showAlert('Failed to delete transaction: ' + error.message, 'danger');
        });
    }
}

function populateCategorySelects() {
    const categoryOptions = categories
        .filter(c => c.isActive)
        .map(c => `<option value="${c.id}">${c.name}</option>`)
        .join('');
    
    $('#transactionCategory').html('<option value="">Select Category</option>' + categoryOptions);
    $('#budgetCategory').html('<option value="">General Budget</option>' + categoryOptions);
}

// Transaction form handler
$(document).ready(function() {
    $('#transactionForm').on('submit', function(e) {
        e.preventDefault();
        
        const transactionData = {
            description: $('#transactionDescription').val(),
            amount: parseFloat($('#transactionAmount').val()),
            transactionType: $('#transactionType').val(),
            transactionDate: new Date($('#transactionDate').val()).toISOString(),
            categoryId: $('#transactionCategory').val() ? parseInt($('#transactionCategory').val()) : null,
            notes: $('#transactionNotes').val()
        };
        
        const transactionId = $('#transactionId').val();
        const url = transactionId ? `/transactions/${transactionId}` : '/transactions';
        const method = transactionId ? 'PUT' : 'POST';
        
        makeApiRequest(url, {
            method: method,
            body: JSON.stringify(transactionData)
        })
        .then(() => {
            $('#transactionModal').modal('hide');
            showAlert('Transaction saved successfully', 'success');
            loadTransactions();
        })
        .catch(error => {
            showAlert('Failed to save transaction: ' + error.message, 'danger');
        });
    });
});
