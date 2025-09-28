# JPA Entity Implementations

## User Story
**AS A** developer working on TIM
**I WANT TO** have complete JPA entity implementations for all database tables
**SO THAT** the application can persist and retrieve data from PostgreSQL properly

## Acceptance Criteria

### AC1: Custom Schema Entities
- [ ] `CustomJwtMetadata` entity mapping to `custom.jwt_metadata` table
- [ ] `CustomAllowlist` entity mapping to `custom.allowlist` table
- [ ] `CustomDenylist` entity mapping to `custom.denylist` table
- [ ] Proper field mappings with correct data types
- [ ] JPA annotations for primary keys, indexes, and constraints

### AC2: TARA Schema Entities
- [ ] `TaraJwtMetadata` entity mapping to `tara.jwt_metadata` table
- [ ] `TaraAllowlist` entity mapping to `tara.allowlist` table
- [ ] `TaraDenylist` entity mapping to `tara.denylist` table
- [ ] `TaraOauthState` entity mapping to `tara.oauth_state` table
- [ ] Schema-specific entity placement in correct packages

### AC3: Entity Relationships
- [ ] Define appropriate relationships between entities
- [ ] Use lazy loading where appropriate for performance
- [ ] Implement proper cascading rules
- [ ] Add validation annotations for data integrity
- [ ] Handle JSON fields properly (claims_json)

### AC4: Repository Implementations
- [ ] Extend JpaRepository for basic CRUD operations
- [ ] Custom query methods for business logic
- [ ] Native queries for complex operations
- [ ] Proper transaction management
- [ ] Error handling for database operations

### AC5: Database Compatibility
- [ ] Ensure compatibility with PostgreSQL-specific features
- [ ] Handle timestamp with timezone properly
- [ ] Support JSONB data type for claims storage
- [ ] Use appropriate column definitions
- [ ] Test with actual database schema

### AC6: Testing
- [ ] Unit tests for entity mappings
- [ ] Integration tests with test database
- [ ] Validate all CRUD operations work correctly
- [ ] Test constraint violations and error handling
- [ ] Performance testing for indexed queries