# Cleanup Jobs for Expired Tokens

## User Story
**AS A** system administrator
**I WANT TO** automatically clean up expired tokens and OAuth states
**SO THAT** the database doesn't grow indefinitely and performance remains optimal

## Acceptance Criteria

### AC1: OAuth State Cleanup
- [ ] Scheduled job to delete expired entries from `tara.oauth_state`
- [ ] Configurable cleanup interval (e.g., every hour)
- [ ] Remove states older than configured TTL (e.g., 10 minutes)
- [ ] Log cleanup statistics for monitoring
- [ ] Handle cleanup failures gracefully

### AC2: JWT Metadata Cleanup
- [ ] Clean up expired entries from both `custom.jwt_metadata` and `tara.jwt_metadata`
- [ ] Configurable retention period beyond token expiration
- [ ] Preserve metadata for tokens in allowlist/denylist
- [ ] Batch processing to avoid long-running transactions
- [ ] Option to archive before deletion

### AC3: Allowlist/Denylist Cleanup
- [ ] Remove expired entries from allowlist tables
- [ ] Clean up denylist entries with expiration dates
- [ ] Preserve permanent denylist entries (no expiration)
- [ ] Cleanup only after referenced token metadata is removed
- [ ] Maintain referential integrity during cleanup

### AC4: Scheduling and Configuration
- [ ] Spring @Scheduled annotation for automated execution
- [ ] Configurable cron expressions for different cleanup jobs
- [ ] Environment-specific cleanup intervals
- [ ] Enable/disable cleanup jobs via configuration
- [ ] Manual trigger endpoints for immediate cleanup

### AC5: Monitoring and Alerting
- [ ] Metrics on cleanup job execution and records processed
- [ ] Alerts for cleanup job failures
- [ ] Logging of cleanup statistics and performance
- [ ] Health check integration for cleanup job status
- [ ] Dashboard metrics for database growth trends

### AC6: Performance and Safety
- [ ] Batch processing with configurable batch sizes
- [ ] Transaction boundaries to prevent long locks
- [ ] Backup verification before large cleanups
- [ ] Ability to pause/resume cleanup operations
- [ ] Recovery mechanisms for interrupted cleanup jobs