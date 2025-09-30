# Token Revocation Functionality

## User Story
**AS AN** administrator or security officer
**I WANT TO** revoke specific JWT tokens or ban users
**SO THAT** I can immediately block access for compromised accounts or security incidents

## Acceptance Criteria

### AC1: Token Revocation Endpoint
- [ ] `POST /jwt/revoke` accepts token to be revoked
- [ ] Adds token hash to appropriate denylist (custom or tara schema)
- [ ] Returns confirmation of revocation
- [ ] Supports revocation by token value or token ID
- [ ] Validates caller has appropriate permissions

### AC2: Subject-Based Revocation
- [ ] `POST /jwt/revoke/subject` revokes all tokens for a user
- [ ] Adds subject to denylist to prevent new token generation
- [ ] Affects both existing and future tokens for the subject
- [ ] Supports optional expiration time for temporary bans
- [ ] Includes reason field for audit purposes

### AC3: Batch Revocation
- [ ] Support revoking multiple tokens in single request
- [ ] Handle partial failures gracefully
- [ ] Return status for each token in batch
- [ ] Optimize database operations for large batches
- [ ] Validate request size limits

### AC4: Denylist Management
- [ ] Store revocation reason and timestamp
- [ ] Set expiration for temporary revocations
- [ ] Clean up expired denylist entries automatically
- [ ] Support querying revocation status
- [ ] Maintain separate denylists for custom vs TARA tokens

### AC5: Integration with Validation
- [ ] Validation endpoints check denylist before accepting tokens
- [ ] Fast denylist lookup using indexed token hashes
- [ ] Subject-based checks for new token generation
- [ ] Proper error responses for revoked tokens
- [ ] Audit logging for revocation events