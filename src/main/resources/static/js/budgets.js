// Budgets JavaScript functionality

let budgets = [];
let categories = [];

function loadBudgets() {
    showLoading();
    
    Promise.all([
        makeApiRequest('/budgets'),
        makeApiRequest('/categories')
    ])
    .then(([budgetsData, categoriesData]) => {
        budgets = budgetsData;
        categories = categoriesData;
        
        renderBudgets();
        populateBudgetCategorySelect();
        hideLoading();
    })
    .catch(error => {
        hideLoading();
        showAlert('Failed to load budgets: ' + error.message, 'danger');
    });
}

function renderBudgets() {
    const currentBudgets = budgets.filter(b => b.isCurrent && b.isActive);
    const expiredBudgets = budgets.filter(b => b.isExpired && b.isActive);
    const futureBudgets = budgets.filter(b => b.isFuture && b.isActive);
    
    const budgetsHtml = `
        <div class="row mb-4">
            <div class="col">
                <h2><i class="bi bi-pie-chart"></i> Budgets</h2>
            </div>
            <div class="col-auto">
                <button class="btn btn-primary" onclick="showBudgetModal()">
                    <i class="bi bi-plus"></i> Add Budget
                </button>
            </div>
        </div>
        
        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <h5>Active Budgets</h5>
                        <h3>${currentBudgets.length}</h3>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <h5>Total Budget</h5>
                        <h3>${formatCurrency(calculateTotalBudget())}</h3>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card stat-card">
                    <div class="card-body text-center">
                        <h5>Budget Utilization</h5>
                        <h3>${calculateAverageUtilization()}%</h3>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Current Budgets -->
        <div class="card mb-4">
            <div class="card-header">
                <h5><i class="bi bi-clock"></i> Current Budgets</h5>
            </div>
            <div class="card-body">
                ${currentBudgets.length > 0 ? 
                    `<div class="row">${currentBudgets.map(budget => renderBudgetCard(budget)).join('')}</div>` :
                    '<p class="text-muted">No current budgets</p>'
                }
            </div>
        </div>
        
        <!-- Future Budgets -->
        <div class="card mb-4">
            <div class="card-header">
                <h5><i class="bi bi-calendar-plus"></i> Future Budgets</h5>
            </div>
            <div class="card-body">
                ${futureBudgets.length > 0 ? 
                    `<div class="row">${futureBudgets.map(budget => renderBudgetCard(budget)).join('')}</div>` :
                    '<p class="text-muted">No future budgets</p>'
                }
            </div>
        </div>
        
        <!-- Expired Budgets -->
        <div class="card">
            <div class="card-header">
                <h5><i class="bi bi-calendar-x"></i> Expired Budgets</h5>
            </div>
            <div class="card-body">
                ${expiredBudgets.length > 0 ? 
                    `<div class="row">${expiredBudgets.map(budget => renderBudgetCard(budget)).join('')}</div>` :
                    '<p class="text-muted">No expired budgets</p>'
                }
            </div>
        </div>
    `;
    
    $('#content').html(budgetsHtml);
}

function renderBudgetCard(budget) {
    const category = categories.find(c => c.id === budget.categoryId);
    const utilizationPercentage = budget.utilizationPercentage || 0;
    const isOverBudget = budget.overBudget || false;
    const progressClass = isOverBudget ? 'bg-danger' : (utilizationPercentage > 80 ? 'bg-warning' : 'bg-success');
    
    return `
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <h5 class="card-title">${budget.name}</h5>
                        <span class="badge bg-${budget.isActive ? 'success' : 'secondary'}">
                            ${budget.isActive ? 'Active' : 'Inactive'}
                        </span>
                    </div>
                    
                    <p class="card-text text-muted">${budget.description || 'No description'}</p>
                    
                    ${category ? `
                        <p class="card-text">
                            <small class="text-muted">
                                <i class="bi bi-tag"></i> ${category.name}
                            </small>
                        </p>
                    ` : ''}
                    
                    <div class="mb-3">
                        <div class="d-flex justify-content-between mb-1">
                            <small>Progress</small>
                            <small>${utilizationPercentage.toFixed(1)}%</small>
                        </div>
                        <div class="progress">
                            <div class="progress-bar ${progressClass}" 
                                 style="width: ${Math.min(utilizationPercentage, 100)}%"></div>
                        </div>
                    </div>
                    
                    <div class="row text-center mb-3">
                        <div class="col-6">
                            <small class="text-muted">Budget</small>
                            <div class="fw-bold">${formatCurrency(budget.budgetAmount)}</div>
                        </div>
                        <div class="col-6">
                            <small class="text-muted">Spent</small>
                            <div class="fw-bold ${isOverBudget ? 'text-danger' : ''}">
                                ${formatCurrency(budget.spentAmount || 0)}
                            </div>
                        </div>
                    </div>
                    
                    <div class="d-flex justify-content-between align-items-center">
                        <small class="text-muted">
                            <i class="bi bi-calendar"></i> 
                            ${formatDate(budget.startDate)} - ${formatDate(budget.endDate)}
                        </small>
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-primary" onclick="editBudget(${budget.id})">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-outline-danger" onclick="deleteBudget(${budget.id})">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function calculateTotalBudget() {
    return budgets
        .filter(b => b.isActive)
        .reduce((sum, b) => sum + parseFloat(b.budgetAmount), 0);
}

function calculateAverageUtilization() {
    const activeBudgets = budgets.filter(b => b.isActive && b.utilizationPercentage !== undefined);
    if (activeBudgets.length === 0) return 0;
    
    const totalUtilization = activeBudgets.reduce((sum, b) => sum + b.utilizationPercentage, 0);
    return (totalUtilization / activeBudgets.length).toFixed(1);
}

function editBudget(id) {
    const budget = budgets.find(b => b.id === id);
    if (budget) {
        showBudgetModal(budget);
    }
}

function deleteBudget(id) {
    if (confirm('Are you sure you want to delete this budget?')) {
        makeApiRequest(`/budgets/${id}`, {
            method: 'DELETE'
        })
        .then(() => {
            showAlert('Budget deleted successfully', 'success');
            loadBudgets();
        })
        .catch(error => {
            showAlert('Failed to delete budget: ' + error.message, 'danger');
        });
    }
}

function populateBudgetCategorySelect() {
    const categoryOptions = categories
        .filter(c => c.isActive)
        .map(c => `<option value="${c.id}">${c.name}</option>`)
        .join('');
    
    $('#budgetCategory').html('<option value="">General Budget</option>' + categoryOptions);
}

// Budget form handler
$(document).ready(function() {
    $('#budgetForm').on('submit', function(e) {
        e.preventDefault();
        
        const budgetData = {
            name: $('#budgetName').val(),
            description: $('#budgetDescription').val(),
            budgetAmount: parseFloat($('#budgetAmount').val()),
            startDate: $('#budgetStartDate').val(),
            endDate: $('#budgetEndDate').val(),
            categoryId: $('#budgetCategory').val() ? parseInt($('#budgetCategory').val()) : null
        };
        
        const budgetId = $('#budgetId').val();
        const url = budgetId ? `/budgets/${budgetId}` : '/budgets';
        const method = budgetId ? 'PUT' : 'POST';
        
        makeApiRequest(url, {
            method: method,
            body: JSON.stringify(budgetData)
        })
        .then(() => {
            $('#budgetModal').modal('hide');
            showAlert('Budget saved successfully', 'success');
            loadBudgets();
        })
        .catch(error => {
            showAlert('Failed to save budget: ' + error.message, 'danger');
        });
    });
    
    // Set end date to one month from start date by default
    $('#budgetStartDate').on('change', function() {
        if (!$('#budgetEndDate').val()) {
            const startDate = new Date($(this).val());
            const endDate = new Date(startDate);
            endDate.setMonth(endDate.getMonth() + 1);
            $('#budgetEndDate').val(endDate.toISOString().split('T')[0]);
        }
    });
});
