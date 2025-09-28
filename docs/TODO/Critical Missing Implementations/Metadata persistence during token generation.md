# Metadata Persistence During Token Generation

## User Story
**AS AN** auditor or system administrator
**I WANT TO** have complete metadata recorded for every JWT token generated
**SO THAT** I can track token usage, debug issues, and maintain security compliance

## Acceptance Criteria

### AC1: Custom JWT Metadata Persistence
- [ ] Save metadata to `custom.jwt_metadata` table on token generation
- [ ] Record JWT name, subject, issuer, audience, and expiration
- [ ] Store complete claims as JSON in `claims_json` field
- [ ] Generate and store token hash for lookup purposes
- [ ] Record accurate issuance timestamp

### AC2: TARA JWT Metadata Persistence
- [ ] Save metadata to `tara.jwt_metadata` table after TARA authentication
- [ ] Include TARA-specific claims and authentication details
- [ ] Link to original OAuth state for audit trail
- [ ] Record authentication method and level of assurance
- [ ] Store session identifier if provided by TARA

### AC3: Atomic Operations
- [ ] Token generation and metadata persistence in same transaction
- [ ] Rollback token generation if metadata save fails
- [ ] Handle database errors gracefully
- [ ] Ensure data consistency between token and metadata
- [ ] Prevent orphaned tokens without metadata

### AC4: Performance Optimization
- [ ] Efficient database inserts with prepared statements
- [ ] Batch processing for multiple token generation
- [ ] Appropriate indexes on frequently queried fields
- [ ] Connection pooling for database operations
- [ ] Async metadata persistence for high-throughput scenarios

### AC5: Data Retention and Cleanup
- [ ] Automatic cleanup of expired token metadata
- [ ] Configurable retention periods for different token types
- [ ] Archive old metadata before deletion
- [ ] Prevent cleanup of tokens still in allowlist
- [ ] Background job for periodic maintenance

### AC6: Query and Reporting
- [ ] Endpoints to query token metadata by various criteria
- [ ] Support pagination for large result sets
- [ ] Filtering by subject, date range, token type
- [ ] Export capabilities for audit reports
- [ ] Performance monitoring for metadata queries