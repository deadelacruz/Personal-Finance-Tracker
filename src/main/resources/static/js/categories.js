// Categories JavaScript functionality

let categories = [];

function loadCategories() {
    showLoading();
    
    makeApiRequest('/categories')
    .then(data => {
        categories = data;
        renderCategories();
        hideLoading();
    })
    .catch(error => {
        hideLoading();
        showAlert('Failed to load categories: ' + error.message, 'danger');
    });
}

function renderCategories() {
    const categoriesHtml = `
        <div class="row mb-4">
            <div class="col">
                <h2><i class="bi bi-tags"></i> Categories</h2>
            </div>
            <div class="col-auto">
                <button class="btn btn-primary" onclick="showCategoryModal()">
                    <i class="bi bi-plus"></i> Add Category
                </button>
                <button class="btn btn-outline-secondary" onclick="createDefaultCategories()">
                    <i class="bi bi-download"></i> Create Defaults
                </button>
            </div>
        </div>
        
        <div class="row">
            ${categories.map(category => renderCategoryCard(category)).join('')}
        </div>
    `;
    
    $('#content').html(categoriesHtml);
}

function renderCategoryCard(category) {
    const isActive = category.isActive;
    const statusClass = isActive ? 'success' : 'secondary';
    const statusText = isActive ? 'Active' : 'Inactive';
    
    return `
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <h5 class="card-title d-flex align-items-center">
                            <span class="badge me-2" style="background-color: ${category.colorCode || '#667eea'}"></span>
                            ${category.name}
                        </h5>
                        <span class="badge bg-${statusClass}">${statusText}</span>
                    </div>
                    <p class="card-text text-muted">${category.description || 'No description'}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <small class="text-muted">
                            <i class="bi bi-arrow-left-right"></i> ${category.transactionCount || 0} transactions
                        </small>
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-primary" onclick="editCategory(${category.id})">
                                <i class="bi bi-pencil"></i>
                            </button>
                            ${isActive ? 
                                `<button class="btn btn-outline-warning" onclick="deactivateCategory(${category.id})">
                                    <i class="bi bi-pause"></i>
                                </button>` :
                                `<button class="btn btn-outline-success" onclick="activateCategory(${category.id})">
                                    <i class="bi bi-play"></i>
                                </button>`
                            }
                            <button class="btn btn-outline-danger" onclick="deleteCategory(${category.id})">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function editCategory(id) {
    const category = categories.find(c => c.id === id);
    if (category) {
        showCategoryModal(category);
    }
}

function deleteCategory(id) {
    if (confirm('Are you sure you want to delete this category?')) {
        makeApiRequest(`/categories/${id}`, {
            method: 'DELETE'
        })
        .then(() => {
            showAlert('Category deleted successfully', 'success');
            loadCategories();
        })
        .catch(error => {
            showAlert('Failed to delete category: ' + error.message, 'danger');
        });
    }
}

function activateCategory(id) {
    makeApiRequest(`/categories/${id}/activate`, {
        method: 'PUT'
    })
    .then(() => {
        showAlert('Category activated successfully', 'success');
        loadCategories();
    })
    .catch(error => {
        showAlert('Failed to activate category: ' + error.message, 'danger');
    });
}

function deactivateCategory(id) {
    makeApiRequest(`/categories/${id}`, {
        method: 'DELETE'
    })
    .then(() => {
        showAlert('Category deactivated successfully', 'success');
        loadCategories();
    })
    .catch(error => {
        showAlert('Failed to deactivate category: ' + error.message, 'danger');
    });
}

function createDefaultCategories() {
    if (confirm('This will create default categories. Continue?')) {
        makeApiRequest('/categories/defaults', {
            method: 'POST'
        })
        .then(() => {
            showAlert('Default categories created successfully', 'success');
            loadCategories();
        })
        .catch(error => {
            showAlert('Failed to create default categories: ' + error.message, 'danger');
        });
    }
}

// Category form handler
$(document).ready(function() {
    $('#categoryForm').on('submit', function(e) {
        e.preventDefault();
        
        const categoryData = {
            name: $('#categoryName').val(),
            description: $('#categoryDescription').val(),
            colorCode: $('#categoryColor').val()
        };
        
        const categoryId = $('#categoryId').val();
        const url = categoryId ? `/categories/${categoryId}` : '/categories';
        const method = categoryId ? 'PUT' : 'POST';
        
        makeApiRequest(url, {
            method: method,
            body: JSON.stringify(categoryData)
        })
        .then(() => {
            $('#categoryModal').modal('hide');
            showAlert('Category saved successfully', 'success');
            loadCategories();
        })
        .catch(error => {
            showAlert('Failed to save category: ' + error.message, 'danger');
        });
    });
});
