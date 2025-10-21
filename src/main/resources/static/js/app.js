// Main application JavaScript file
// Global variables and utility functions

let currentUser = null;
let authToken = null;
const API_BASE_URL = 'http://localhost:8080/api';

// Utility functions
function showLoading() {
    $('.loading').addClass('show');
}

function hideLoading() {
    $('.loading').removeClass('show');
}

function showAlert(message, type = 'info') {
    const alertHtml = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    $('#content').prepend(alertHtml);
    
    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        $('.alert').alert('close');
    }, 5000);
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function formatDateTime(dateString) {
    return new Date(dateString).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// API helper functions
function makeApiRequest(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            ...(authToken && { 'Authorization': `Bearer ${authToken}` })
        }
    };
    
    return fetch(API_BASE_URL + url, { ...defaultOptions, ...options })
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    logout();
                    throw new Error('Unauthorized');
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        });
}

// Navigation functions
function showDashboard() {
    setActiveNavItem('dashboard');
    loadDashboard();
}

function showTransactions() {
    setActiveNavItem('transactions');
    loadTransactions();
}

function showCategories() {
    setActiveNavItem('categories');
    loadCategories();
}

function showBudgets() {
    setActiveNavItem('budgets');
    loadBudgets();
}

function showReports() {
    setActiveNavItem('reports');
    loadReports();
}

function setActiveNavItem(item) {
    $('.sidebar .nav-link').removeClass('active');
    $(`.sidebar .nav-link[onclick*="${item}"]`).addClass('active');
}

// Modal functions
function showLoginModal() {
    $('#registerModal').modal('hide');
    $('#loginModal').modal('show');
}

function showRegisterModal() {
    $('#loginModal').modal('hide');
    $('#registerModal').modal('show');
}

function showTransactionModal(transaction = null) {
    if (transaction) {
        $('#transactionModalTitle').text('Edit Transaction');
        $('#transactionId').val(transaction.id);
        $('#transactionDescription').val(transaction.description);
        $('#transactionAmount').val(transaction.amount);
        $('#transactionType').val(transaction.transactionType);
        $('#transactionDate').val(new Date(transaction.transactionDate).toISOString().slice(0, 16));
        $('#transactionCategory').val(transaction.categoryId || '');
        $('#transactionNotes').val(transaction.notes || '');
    } else {
        $('#transactionModalTitle').text('Add Transaction');
        $('#transactionForm')[0].reset();
        $('#transactionId').val('');
        $('#transactionDate').val(new Date().toISOString().slice(0, 16));
    }
    $('#transactionModal').modal('show');
}

function showCategoryModal(category = null) {
    if (category) {
        $('#categoryModalTitle').text('Edit Category');
        $('#categoryId').val(category.id);
        $('#categoryName').val(category.name);
        $('#categoryDescription').val(category.description || '');
        $('#categoryColor').val(category.colorCode || '#667eea');
    } else {
        $('#categoryModalTitle').text('Add Category');
        $('#categoryForm')[0].reset();
        $('#categoryId').val('');
        $('#categoryColor').val('#667eea');
    }
    $('#categoryModal').modal('show');
}

function showBudgetModal(budget = null) {
    if (budget) {
        $('#budgetModalTitle').text('Edit Budget');
        $('#budgetId').val(budget.id);
        $('#budgetName').val(budget.name);
        $('#budgetDescription').val(budget.description || '');
        $('#budgetAmount').val(budget.budgetAmount);
        $('#budgetStartDate').val(budget.startDate);
        $('#budgetEndDate').val(budget.endDate);
        $('#budgetCategory').val(budget.categoryId || '');
    } else {
        $('#budgetModalTitle').text('Add Budget');
        $('#budgetForm')[0].reset();
        $('#budgetId').val('');
        const today = new Date().toISOString().split('T')[0];
        $('#budgetStartDate').val(today);
    }
    $('#budgetModal').modal('show');
}

// Profile functions
function showProfile() {
    if (!currentUser) return;
    
    const profileHtml = `
        <div class="row">
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="bi bi-person"></i> Profile Information</h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-6">
                                <p><strong>Username:</strong> ${currentUser.username}</p>
                                <p><strong>Email:</strong> ${currentUser.email}</p>
                            </div>
                            <div class="col-md-6">
                                <p><strong>Name:</strong> ${currentUser.firstName} ${currentUser.lastName}</p>
                                <p><strong>Member since:</strong> ${formatDate(currentUser.createdAt)}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    $('#content').html(profileHtml);
    setActiveNavItem('profile');
}

function changePassword() {
    const newPassword = prompt('Enter new password:');
    if (newPassword && newPassword.length >= 8) {
        const changePasswordData = {
            currentPassword: prompt('Enter current password:'),
            newPassword: newPassword
        };
        
        makeApiRequest('/auth/change-password', {
            method: 'POST',
            body: JSON.stringify(changePasswordData)
        })
        .then(() => {
            showAlert('Password changed successfully!', 'success');
        })
        .catch(error => {
            showAlert('Failed to change password: ' + error.message, 'danger');
        });
    } else if (newPassword.length < 8) {
        showAlert('Password must be at least 8 characters long', 'warning');
    }
}

// Initialize application
$(document).ready(function() {
    // Check if user is logged in
    const token = localStorage.getItem('authToken');
    if (token) {
        authToken = token;
        // Verify token and get user info
        makeApiRequest('/auth/me')
            .then(user => {
                currentUser = user;
                $('#userName').text(user.firstName + ' ' + user.lastName);
                showDashboard();
            })
            .catch(() => {
                logout();
            });
    } else {
        showLoginModal();
    }
    
    // Set up form handlers
    setupFormHandlers();
});

function setupFormHandlers() {
    // Login form
    $('#loginForm').on('submit', function(e) {
        e.preventDefault();
        const loginData = {
            username: $('#loginUsername').val(),
            password: $('#loginPassword').val()
        };
        
        makeApiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify(loginData)
        })
        .then(data => {
            authToken = data.token;
            currentUser = data.user;
            localStorage.setItem('authToken', authToken);
            $('#userName').text(currentUser.firstName + ' ' + currentUser.lastName);
            $('#loginModal').modal('hide');
            showDashboard();
            showAlert('Welcome back, ' + currentUser.firstName + '!', 'success');
        })
        .catch(error => {
            showAlert('Login failed: ' + error.message, 'danger');
        });
    });
    
    // Register form
    $('#registerForm').on('submit', function(e) {
        e.preventDefault();
        const registerData = {
            username: $('#registerUsername').val(),
            email: $('#registerEmail').val(),
            firstName: $('#registerFirstName').val(),
            lastName: $('#registerLastName').val(),
            password: $('#registerPassword').val()
        };
        
        makeApiRequest('/auth/register', {
            method: 'POST',
            body: JSON.stringify(registerData)
        })
        .then(data => {
            authToken = data.token;
            currentUser = data.user;
            localStorage.setItem('authToken', authToken);
            $('#userName').text(currentUser.firstName + ' ' + currentUser.lastName);
            $('#registerModal').modal('hide');
            showDashboard();
            showAlert('Welcome to Personal Finance Tracker!', 'success');
        })
        .catch(error => {
            showAlert('Registration failed: ' + error.message, 'danger');
        });
    });
}

function logout() {
    // Clear local storage
    authToken = null;
    currentUser = null;
    localStorage.removeItem('authToken');
    
    // Clear user display
    $('#userName').text('User');
    
    // Get CSRF token from meta tag or cookie
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || 
                     getCookie('XSRF-TOKEN');
    
    // Call server-side logout endpoint
    const formData = new FormData();
    if (csrfToken) {
        formData.append('_csrf', csrfToken);
    }
    
    fetch('/logout', {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
    })
    .then(() => {
        // Show login modal after successful logout
        showLoginModal();
        showAlert('You have been logged out', 'info');
    })
    .catch(error => {
        console.error('Logout error:', error);
        // Still show login modal even if server logout fails
        showLoginModal();
        showAlert('You have been logged out', 'info');
    });
}

// Helper function to get cookie value
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}
