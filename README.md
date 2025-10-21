# Personal Finance Tracker

A comprehensive personal finance management application built with Spring Boot 4.0 and modern web technologies.

## 🚀 Features

### Backend Features (Spring Boot)
- **REST API endpoints** for all financial operations
- **JPA/Hibernate** for data persistence with ACID compliance
- **Spring Security** with JWT authentication
- **Comprehensive validation** and error handling
- **Advanced reporting** with Spring Data analytics
- **Clean Architecture** design following SOLID principles
- **Microservices-ready** architecture

### Frontend Features
- **Modern React-like** JavaScript with TypeScript-style coding
- **Bootstrap 5** for responsive design
- **jQuery** for DOM manipulation and AJAX
- **Chart.js** for data visualization
- **Real-time updates** and interactive dashboards

### Core Functionality
1. **Transaction Management** - Add, edit, delete income and expenses
2. **Category Organization** - Organize transactions with custom categories
3. **Budget Tracking** - Set and monitor spending budgets
4. **Financial Reporting** - Comprehensive analytics and insights
5. **User Authentication** - Secure login and registration
6. **Dashboard** - Real-time financial overview

## 🏗️ Architecture

### Clean Architecture Layers
```
├── Domain Layer (Entities, Repositories)
├── Application Layer (Services, DTOs, Mappers)
├── Infrastructure Layer (Security, Database, Config)
└── Presentation Layer (Controllers, Frontend)
```

### Technology Stack
- **Backend**: Spring Boot 4.0, Java 25, JPA/Hibernate
- **Database**: H2 (development), Flyway migrations
- **Security**: Spring Security, JWT tokens
- **Frontend**: HTML5, CSS3, JavaScript (ES6+), Bootstrap 5, jQuery
- **Build Tool**: Gradle with Kotlin DSL

## 🚀 Getting Started

### Prerequisites
- Java 25 JDK
- Gradle 8.0+
- Modern web browser

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd demo
   ```

2. **Build the application**
   ```bash
   ./gradlew build
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**
   - Web Interface: http://localhost:8080
   - H2 Database Console: http://localhost:8080/h2-console
   - API Documentation: http://localhost:8080/actuator

### Default Database Configuration
- **URL**: jdbc:h2:mem:testdb
- **Username**: sa
- **Password**: password

## 📚 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/auth/me` - Get current user
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/change-password` - Change password

### Transactions
- `GET /api/transactions` - Get user transactions (paginated)
- `POST /api/transactions` - Create new transaction
- `GET /api/transactions/{id}` - Get transaction by ID
- `PUT /api/transactions/{id}` - Update transaction
- `DELETE /api/transactions/{id}` - Delete transaction
- `GET /api/transactions/summary` - Get financial summary

### Categories
- `GET /api/categories` - Get user categories
- `POST /api/categories` - Create new category
- `PUT /api/categories/{id}` - Update category
- `DELETE /api/categories/{id}` - Delete category
- `POST /api/categories/defaults` - Create default categories

### Budgets
- `GET /api/budgets` - Get user budgets
- `POST /api/budgets` - Create new budget
- `PUT /api/budgets/{id}` - Update budget
- `DELETE /api/budgets/{id}` - Delete budget
- `GET /api/budgets/summaries` - Get budget summaries

## 🎯 Key Features

### 1. Transaction Management
- Add income and expense transactions
- Categorize transactions for better organization
- Date-based filtering and search
- Bulk operations support

### 2. Category System
- Create custom categories with colors
- Default category templates
- Category-based reporting
- Soft delete functionality

### 3. Budget Tracking
- Set monthly/yearly budgets
- Category-specific budgets
- Real-time budget monitoring
- Over-budget alerts

### 4. Financial Reporting
- Income vs expenses analysis
- Category-wise spending breakdown
- Monthly/yearly trends
- Savings rate calculation

### 5. Dashboard
- Real-time financial overview
- Quick action buttons
- Recent transactions
- Budget progress indicators

## 🔒 Security Features

- **JWT Authentication** with configurable expiration
- **Password encryption** using BCrypt
- **CORS configuration** for cross-origin requests
- **Input validation** and sanitization
- **SQL injection protection** via JPA
- **XSS protection** through proper encoding

## 📊 Database Schema

### Core Tables
- **users** - User accounts and profiles
- **categories** - Transaction categories
- **transactions** - Financial transactions
- **budgets** - Budget definitions and tracking

### Key Relationships
- Users have many transactions, categories, and budgets
- Transactions belong to users and optionally to categories
- Budgets belong to users and optionally to categories

## 🧪 Testing

Run the test suite:
```bash
./gradlew test
```

## 🚀 Deployment

### Development
```bash
./gradlew bootRun
```

### Production
```bash
./gradlew build
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

## 📈 Performance Considerations

- **Database indexing** on frequently queried fields
- **Pagination** for large data sets
- **Lazy loading** for entity relationships
- **Connection pooling** for database connections
- **Caching** for frequently accessed data

## 🔧 Configuration

### Application Properties
- Database connection settings
- JWT secret and expiration
- Logging levels
- Actuator endpoints

### Environment Variables
- `SPRING_PROFILES_ACTIVE` - Active profile
- `DATABASE_URL` - Database connection URL
- `JWT_SECRET` - JWT signing secret

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the API endpoints

## 🔮 Future Enhancements

- [ ] Mobile app support
- [ ] Advanced reporting features
- [ ] Data export/import
- [ ] Multi-currency support
- [ ] Investment tracking
- [ ] Bill reminders
- [ ] Goal setting and tracking
- [ ] Social features and sharing

---

**Built with ❤️ using Spring Boot 4.0, Java 25, and modern web technologies.**
