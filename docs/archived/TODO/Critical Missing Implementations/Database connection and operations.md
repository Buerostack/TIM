# Database Connection and Operations

## User Story
**AS A** TIM application instance
**I WANT TO** properly connect to PostgreSQL and perform database operations
**SO THAT** I can store and retrieve JWT metadata, allowlists, denylists, and OAuth state

## Acceptance Criteria

### AC1: Database Configuration
- [ ] Spring Boot DataSource configuration for PostgreSQL
- [ ] Connection pooling with HikariCP
- [ ] Environment-based configuration (dev, test, prod)
- [ ] SSL connection support for production
- [ ] Connection timeout and retry logic

### AC2: Schema Management
- [ ] Automatic schema creation on startup if missing
- [ ] Support for multiple schemas (custom, tara)
- [ ] Database migration strategy (Flyway or Liquibase)
- [ ] Version tracking for schema changes
- [ ] Rollback capabilities for failed migrations

### AC3: Transaction Management
- [ ] Proper @Transactional annotations on service methods
- [ ] Rollback on runtime exceptions
- [ ] Isolation levels appropriate for each operation
- [ ] Handle deadlocks and retry logic
- [ ] Bulk operations with appropriate batch sizes

### AC4: Connection Health and Monitoring
- [ ] Health check endpoint for database connectivity
- [ ] Connection pool metrics and monitoring
- [ ] Slow query logging and analysis
- [ ] Database connection timeout handling
- [ ] Graceful degradation when database is unavailable

### AC5: Data Persistence Operations
- [ ] Save JWT metadata on token generation
- [ ] Query operations for allowlist/denylist checking
- [ ] OAuth state persistence and cleanup
- [ ] Efficient queries using proper indexes
- [ ] Handle concurrent access and data consistency

### AC6: Error Handling
- [ ] Graceful handling of database connection failures
- [ ] Retry logic for transient database errors
- [ ] Proper exception mapping to HTTP responses
- [ ] Logging of database errors without exposing sensitive data
- [ ] Circuit breaker pattern for database operations