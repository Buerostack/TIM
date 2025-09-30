# Database Operations

## Overview

This document provides comprehensive guidance for common database operations, maintenance procedures, monitoring, and troubleshooting for TIM's PostgreSQL database. All operations are designed to work with the dual-schema architecture (`custom` and `tara`).

## Connection and Access

### Database Connection

**Direct Connection to Container:**
```bash
# Connect to PostgreSQL container
docker exec -it tim-postgres psql -U tim -d tim

# Or without interactive TTY
docker exec tim-postgres psql -U tim -d tim -c "SELECT 1;"
```

**Connection Parameters:**
- **Host**: `tim-postgres` (container name) or `localhost:9876` (external)
- **Database**: `tim`
- **Username**: `tim`
- **Password**: `123` (development default)
- **Port**: `5432` (internal) / `9876` (external)

### Basic Database Information

**Schema and Table Overview:**
```sql
-- List all schemas
SELECT schema_name FROM information_schema.schemata
WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast');

-- List tables in both schemas
SELECT table_schema, table_name, table_type
FROM information_schema.tables
WHERE table_schema IN ('custom', 'tara')
ORDER BY table_schema, table_name;

-- Get table row counts
SELECT
    schemaname,
    tablename,
    n_tup_ins - n_tup_del as estimated_rows
FROM pg_stat_user_tables
WHERE schemaname IN ('custom', 'tara')
ORDER BY schemaname, tablename;
```

## Common Query Operations

### Token Lookup and Validation

**Find Token by JWT ID:**
```sql
-- Search in custom schema
SELECT * FROM custom.jwt_metadata
WHERE jwt_uuid = '4b2bc14c-c234-4f63-a4dd-4522860d9b36';

-- Search in TARA schema
SELECT * FROM tara.jwt_metadata
WHERE jwt_uuid = '4b2bc14c-c234-4f63-a4dd-4522860d9b36';

-- Search across both schemas
SELECT 'custom' as schema, * FROM custom.jwt_metadata
WHERE jwt_uuid = '4b2bc14c-c234-4f63-a4dd-4522860d9b36'
UNION ALL
SELECT 'tara' as schema, * FROM tara.jwt_metadata
WHERE jwt_uuid = '4b2bc14c-c234-4f63-a4dd-4522860d9b36';
```

**Check Token Revocation Status:**
```sql
-- Check if token is revoked (custom)
SELECT
    CASE
        WHEN d.jwt_uuid IS NOT NULL THEN 'REVOKED'
        WHEN m.expires_at < NOW() THEN 'EXPIRED'
        WHEN m.jwt_uuid IS NOT NULL THEN 'ACTIVE'
        ELSE 'NOT_FOUND'
    END as status,
    m.issued_at,
    m.expires_at,
    d.denylisted_at
FROM custom.jwt_metadata m
FULL OUTER JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE COALESCE(m.jwt_uuid, d.jwt_uuid) = '4b2bc14c-c234-4f63-a4dd-4522860d9b36';
```

**Token Activity Summary:**
```sql
-- Get comprehensive token status
WITH token_status AS (
    SELECT
        'custom' as schema,
        m.jwt_uuid,
        m.claim_keys,
        m.issued_at,
        m.expires_at,
        d.denylisted_at,
        CASE
            WHEN d.jwt_uuid IS NOT NULL THEN 'REVOKED'
            WHEN m.expires_at < NOW() THEN 'EXPIRED'
            ELSE 'ACTIVE'
        END as status
    FROM custom.jwt_metadata m
    LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid

    UNION ALL

    SELECT
        'tara' as schema,
        m.jwt_uuid,
        m.claim_keys,
        m.issued_at,
        m.expires_at,
        d.denylisted_at,
        CASE
            WHEN d.jwt_uuid IS NOT NULL THEN 'REVOKED'
            WHEN m.expires_at < NOW() THEN 'EXPIRED'
            ELSE 'ACTIVE'
        END as status
    FROM tara.jwt_metadata m
    LEFT JOIN tara.denylist d ON m.jwt_uuid = d.jwt_uuid
)
SELECT schema, status, COUNT(*) as count
FROM token_status
GROUP BY schema, status
ORDER BY schema, status;
```

### Analytics and Reporting

**Token Generation Statistics:**
```sql
-- Daily token generation for last 30 days
SELECT
    'custom' as schema,
    DATE(issued_at) as date,
    COUNT(*) as tokens_generated
FROM custom.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(issued_at)

UNION ALL

SELECT
    'tara' as schema,
    DATE(issued_at) as date,
    COUNT(*) as tokens_generated
FROM tara.jwt_metadata
WHERE issued_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(issued_at)

ORDER BY schema, date;
```

**Claim Usage Patterns:**
```sql
-- Most popular claim combinations
SELECT
    schema_name,
    claim_keys,
    COUNT(*) as usage_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (PARTITION BY schema_name), 2) as percentage
FROM (
    SELECT 'custom' as schema_name, claim_keys FROM custom.jwt_metadata
    WHERE issued_at > NOW() - INTERVAL '30 days'
    UNION ALL
    SELECT 'tara' as schema_name, claim_keys FROM tara.jwt_metadata
    WHERE issued_at > NOW() - INTERVAL '30 days'
) combined
GROUP BY schema_name, claim_keys
ORDER BY schema_name, usage_count DESC;
```

**Revocation Analysis:**
```sql
-- Revocation patterns and timing
SELECT
    schema_name,
    DATE(denylisted_at) as revocation_date,
    COUNT(*) as revocations,
    AVG(EXTRACT(EPOCH FROM (denylisted_at - issued_at))/3600) as avg_hours_to_revocation
FROM (
    SELECT
        'custom' as schema_name,
        d.denylisted_at,
        m.issued_at
    FROM custom.denylist d
    JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
    WHERE d.denylisted_at > NOW() - INTERVAL '30 days'

    UNION ALL

    SELECT
        'tara' as schema_name,
        d.denylisted_at,
        m.issued_at
    FROM tara.denylist d
    JOIN tara.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
    WHERE d.denylisted_at > NOW() - INTERVAL '30 days'
) combined
GROUP BY schema_name, DATE(denylisted_at)
ORDER BY schema_name, revocation_date;
```

## Maintenance Operations

### Regular Cleanup Procedures

**Daily Cleanup Script:**
```sql
-- Clean expired denylist entries (both schemas)
DELETE FROM custom.denylist WHERE expires_at < NOW();
DELETE FROM tara.denylist WHERE expires_at < NOW();

-- Clean expired allowlist entries (both schemas)
DELETE FROM custom.allowlist WHERE expires_at < NOW();
DELETE FROM tara.allowlist WHERE expires_at < NOW();

-- Clean old OAuth states (TARA only, 15-minute timeout)
DELETE FROM tara.oauth_state WHERE created_at < NOW() - INTERVAL '15 minutes';

-- Get cleanup summary
SELECT
    'custom.denylist' as table_name,
    (SELECT COUNT(*) FROM custom.denylist) as remaining_rows
UNION ALL
SELECT 'tara.denylist', (SELECT COUNT(*) FROM tara.denylist)
UNION ALL
SELECT 'custom.allowlist', (SELECT COUNT(*) FROM custom.allowlist)
UNION ALL
SELECT 'tara.allowlist', (SELECT COUNT(*) FROM tara.allowlist)
UNION ALL
SELECT 'tara.oauth_state', (SELECT COUNT(*) FROM tara.oauth_state);
```

**Weekly Cleanup Script:**
```sql
-- Clean old metadata (keep for audit period)
-- Custom tokens: 30-day retention
DELETE FROM custom.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '30 days';

-- TARA tokens: 90-day retention (government compliance)
DELETE FROM tara.jwt_metadata
WHERE expires_at < NOW() - INTERVAL '90 days';

-- Vacuum and analyze for performance
VACUUM ANALYZE custom.jwt_metadata;
VACUUM ANALYZE custom.denylist;
VACUUM ANALYZE custom.allowlist;
VACUUM ANALYZE tara.jwt_metadata;
VACUUM ANALYZE tara.denylist;
VACUUM ANALYZE tara.allowlist;
VACUUM ANALYZE tara.oauth_state;

-- Update table statistics
ANALYZE;
```

**Monthly Maintenance:**
```sql
-- Full vacuum for space reclamation
VACUUM FULL custom.jwt_metadata;
VACUUM FULL custom.denylist;
VACUUM FULL tara.jwt_metadata;
VACUUM FULL tara.denylist;

-- Reindex for performance
REINDEX TABLE custom.jwt_metadata;
REINDEX TABLE custom.denylist;
REINDEX TABLE tara.jwt_metadata;
REINDEX TABLE tara.denylist;

-- Check for table bloat
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size
FROM pg_tables
WHERE schemaname IN ('custom', 'tara')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Data Integrity Checks

**Orphaned Records Detection:**
```sql
-- Find orphaned denylist entries (no corresponding metadata)
SELECT 'custom' as schema, COUNT(*) as orphaned_denylist
FROM custom.denylist d
LEFT JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE m.jwt_uuid IS NULL

UNION ALL

SELECT 'tara' as schema, COUNT(*) as orphaned_denylist
FROM tara.denylist d
LEFT JOIN tara.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE m.jwt_uuid IS NULL;

-- Find tokens with future issue dates (clock skew detection)
SELECT
    'custom' as schema,
    COUNT(*) as future_tokens,
    MIN(issued_at) as earliest_future_date
FROM custom.jwt_metadata
WHERE issued_at > NOW() + INTERVAL '5 minutes'

UNION ALL

SELECT
    'tara' as schema,
    COUNT(*) as future_tokens,
    MIN(issued_at) as earliest_future_date
FROM tara.jwt_metadata
WHERE issued_at > NOW() + INTERVAL '5 minutes';

-- Check for duplicate JTIs within each schema (shouldn't exist)
SELECT schema_name, jwt_uuid, count
FROM (
    SELECT 'custom' as schema_name, jwt_uuid, COUNT(*) as count
    FROM custom.jwt_metadata
    GROUP BY jwt_uuid
    HAVING COUNT(*) > 1

    UNION ALL

    SELECT 'tara' as schema_name, jwt_uuid, COUNT(*) as count
    FROM tara.jwt_metadata
    GROUP BY jwt_uuid
    HAVING COUNT(*) > 1
) duplicates;
```

**Cleanup Orphaned Records:**
```sql
-- Remove orphaned denylist entries
DELETE FROM custom.denylist d
WHERE NOT EXISTS (
    SELECT 1 FROM custom.jwt_metadata m
    WHERE m.jwt_uuid = d.jwt_uuid
);

DELETE FROM tara.denylist d
WHERE NOT EXISTS (
    SELECT 1 FROM tara.jwt_metadata m
    WHERE m.jwt_uuid = d.jwt_uuid
);

-- Report cleanup results
SELECT
    'Orphaned custom denylist entries cleaned' as operation,
    ROW_COUNT() as rows_affected;
```

## Emergency Operations

### Mass Token Revocation

**Revoke Tokens by Time Range:**
```sql
-- Emergency revocation of tokens issued in specific time window
-- Example: Security incident between 10:00-11:00 on specific date

BEGIN;

-- Revoke custom tokens
INSERT INTO custom.denylist (jwt_uuid, denylisted_at, expires_at)
SELECT jwt_uuid, NOW(), expires_at
FROM custom.jwt_metadata
WHERE issued_at BETWEEN '2024-01-15 10:00:00' AND '2024-01-15 11:00:00'
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom.denylist);

-- Revoke TARA tokens
INSERT INTO tara.denylist (jwt_uuid, denylisted_at, expires_at)
SELECT jwt_uuid, NOW(), expires_at
FROM tara.jwt_metadata
WHERE issued_at BETWEEN '2024-01-15 10:00:00' AND '2024-01-15 11:00:00'
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM tara.denylist);

-- Verify revocation counts
SELECT
    'custom' as schema,
    COUNT(*) as tokens_revoked
FROM custom.denylist
WHERE denylisted_at >= CURRENT_TIMESTAMP - INTERVAL '1 minute'

UNION ALL

SELECT
    'tara' as schema,
    COUNT(*) as tokens_revoked
FROM tara.denylist
WHERE denylisted_at >= CURRENT_TIMESTAMP - INTERVAL '1 minute';

COMMIT;
```

**Revoke Tokens by Claim Pattern:**
```sql
-- Revoke all tokens with specific claim patterns
-- Example: Compromise of tokens with admin claims

BEGIN;

-- Find and revoke custom admin tokens
INSERT INTO custom.denylist (jwt_uuid, denylisted_at, expires_at)
SELECT jwt_uuid, NOW(), expires_at
FROM custom.jwt_metadata
WHERE claim_keys LIKE '%admin%'
  AND expires_at > NOW()  -- Only active tokens
  AND jwt_uuid NOT IN (SELECT jwt_uuid FROM custom.denylist);

-- Report results
SELECT
    claim_keys,
    COUNT(*) as revoked_count
FROM custom.jwt_metadata m
JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE d.denylisted_at >= CURRENT_TIMESTAMP - INTERVAL '1 minute'
  AND claim_keys LIKE '%admin%'
GROUP BY claim_keys;

COMMIT;
```

### Database Recovery

**Backup Operations:**
```bash
# Full database backup
docker exec tim-postgres pg_dump -U tim tim > tim-full-backup-$(date +%Y%m%d).sql

# Schema-specific backups
docker exec tim-postgres pg_dump -U tim -n custom tim > custom-schema-backup-$(date +%Y%m%d).sql
docker exec tim-postgres pg_dump -U tim -n tara tim > tara-schema-backup-$(date +%Y%m%d).sql

# Data-only backup (no schema)
docker exec tim-postgres pg_dump -U tim --data-only tim > tim-data-backup-$(date +%Y%m%d).sql

# Compressed backup
docker exec tim-postgres pg_dump -U tim -Fc tim > tim-backup-$(date +%Y%m%d).dump
```

**Restore Operations:**
```bash
# Restore full database (WARNING: This will overwrite existing data)
docker exec -i tim-postgres psql -U tim tim < tim-full-backup-20240115.sql

# Restore specific schema
docker exec -i tim-postgres psql -U tim tim < custom-schema-backup-20240115.sql

# Restore from compressed backup
docker exec tim-postgres pg_restore -U tim -d tim tim-backup-20240115.dump

# Restore to different database for testing
docker exec tim-postgres createdb -U tim tim_test
docker exec -i tim-postgres psql -U tim tim_test < tim-full-backup-20240115.sql
```

**Point-in-Time Recovery Simulation:**
```sql
-- Simulate recovery to specific point in time
-- Step 1: Identify target time
SELECT MAX(issued_at) as last_good_time
FROM custom.jwt_metadata
WHERE issued_at < '2024-01-15 10:00:00';  -- Before incident

-- Step 2: Remove data after target time (BE VERY CAREFUL)
BEGIN;

-- Remove tokens issued after incident
DELETE FROM custom.denylist
WHERE jwt_uuid IN (
    SELECT jwt_uuid FROM custom.jwt_metadata
    WHERE issued_at >= '2024-01-15 10:00:00'
);

DELETE FROM custom.jwt_metadata
WHERE issued_at >= '2024-01-15 10:00:00';

-- Verify recovery point
SELECT COUNT(*) as remaining_tokens, MAX(issued_at) as latest_token
FROM custom.jwt_metadata;

-- ROLLBACK; -- Uncomment to cancel
-- COMMIT;   -- Uncomment to apply (IRREVERSIBLE)
```

## Performance Monitoring

### Database Performance Metrics

**Connection and Activity Monitoring:**
```sql
-- Active connections
SELECT
    datname,
    usename,
    application_name,
    client_addr,
    state,
    query_start,
    state_change
FROM pg_stat_activity
WHERE datname = 'tim'
ORDER BY query_start DESC;

-- Connection counts by state
SELECT
    state,
    COUNT(*) as connection_count
FROM pg_stat_activity
WHERE datname = 'tim'
GROUP BY state;

-- Long-running queries
SELECT
    query_start,
    now() - query_start as duration,
    state,
    query
FROM pg_stat_activity
WHERE datname = 'tim'
  AND now() - query_start > interval '30 seconds'
ORDER BY duration DESC;
```

**Table Performance Statistics:**
```sql
-- Table access patterns
SELECT
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    n_tup_ins,
    n_tup_upd,
    n_tup_del
FROM pg_stat_user_tables
WHERE schemaname IN ('custom', 'tara')
ORDER BY schemaname, tablename;

-- Index usage statistics
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname IN ('custom', 'tara')
ORDER BY schemaname, tablename, indexname;

-- Cache hit ratios
SELECT
    schemaname,
    tablename,
    heap_blks_read,
    heap_blks_hit,
    CASE
        WHEN heap_blks_read + heap_blks_hit = 0 THEN 0
        ELSE ROUND(heap_blks_hit::numeric / (heap_blks_read + heap_blks_hit) * 100, 2)
    END as cache_hit_ratio
FROM pg_statio_user_tables
WHERE schemaname IN ('custom', 'tara')
ORDER BY cache_hit_ratio;
```

### Query Performance Analysis

**Slow Query Detection:**
```sql
-- Enable query statistics (if not already enabled)
-- This requires postgresql.conf modification or runtime setting
-- SHOW log_min_duration_statement;

-- Analyze expensive operations
EXPLAIN (ANALYZE, BUFFERS)
SELECT m.*, d.denylisted_at
FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE m.issued_at > NOW() - INTERVAL '7 days';

-- Check index usage for common queries
EXPLAIN (ANALYZE, BUFFERS)
SELECT jwt_uuid FROM custom.denylist
WHERE jwt_uuid = '4b2bc14c-c234-4f63-a4dd-4522860d9b36';
```

**Performance Optimization Recommendations:**
```sql
-- Identify missing indexes
SELECT
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    seq_scan / NULLIF(idx_scan, 0) as seq_to_idx_ratio
FROM pg_stat_user_tables
WHERE schemaname IN ('custom', 'tara')
  AND seq_scan > 1000  -- Tables with many sequential scans
ORDER BY seq_to_idx_ratio DESC NULLS LAST;

-- Suggest additional indexes
-- Based on common query patterns:

-- Time-based queries (if frequently used)
-- CREATE INDEX idx_custom_jwt_metadata_issued ON custom.jwt_metadata (issued_at);
-- CREATE INDEX idx_tara_jwt_metadata_issued ON tara.jwt_metadata (issued_at);

-- Claim-based searches (if needed)
-- CREATE INDEX idx_custom_jwt_metadata_claims ON custom.jwt_metadata (claim_keys);
-- CREATE INDEX idx_tara_jwt_metadata_claims ON tara.jwt_metadata (claim_keys);
```

## Monitoring and Alerting

### Health Check Queries

**Basic Health Checks:**
```sql
-- Database connectivity
SELECT 'Database connected' as status, NOW() as timestamp;

-- Schema integrity
SELECT
    schema_name,
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = schema_name) as table_count
FROM information_schema.schemata
WHERE schema_name IN ('custom', 'tara');

-- Table row counts
SELECT
    'custom' as schema,
    'jwt_metadata' as table_name,
    COUNT(*) as row_count
FROM custom.jwt_metadata
UNION ALL
SELECT 'custom', 'denylist', COUNT(*) FROM custom.denylist
UNION ALL
SELECT 'tara', 'jwt_metadata', COUNT(*) FROM tara.jwt_metadata
UNION ALL
SELECT 'tara', 'denylist', COUNT(*) FROM tara.denylist
UNION ALL
SELECT 'tara', 'oauth_state', COUNT(*) FROM tara.oauth_state;
```

**Alerting Thresholds:**
```sql
-- High revocation rate (potential security incident)
SELECT
    schema_name,
    revocation_count,
    total_tokens,
    revocation_rate
FROM (
    SELECT
        'custom' as schema_name,
        (SELECT COUNT(*) FROM custom.denylist WHERE denylisted_at > NOW() - INTERVAL '1 hour') as revocation_count,
        (SELECT COUNT(*) FROM custom.jwt_metadata WHERE issued_at > NOW() - INTERVAL '1 hour') as total_tokens,
        CASE
            WHEN (SELECT COUNT(*) FROM custom.jwt_metadata WHERE issued_at > NOW() - INTERVAL '1 hour') = 0 THEN 0
            ELSE ROUND((SELECT COUNT(*) FROM custom.denylist WHERE denylisted_at > NOW() - INTERVAL '1 hour') * 100.0 / NULLIF((SELECT COUNT(*) FROM custom.jwt_metadata WHERE issued_at > NOW() - INTERVAL '1 hour'), 0), 2)
        END as revocation_rate

    UNION ALL

    SELECT
        'tara' as schema_name,
        (SELECT COUNT(*) FROM tara.denylist WHERE denylisted_at > NOW() - INTERVAL '1 hour'),
        (SELECT COUNT(*) FROM tara.jwt_metadata WHERE issued_at > NOW() - INTERVAL '1 hour'),
        CASE
            WHEN (SELECT COUNT(*) FROM tara.jwt_metadata WHERE issued_at > NOW() - INTERVAL '1 hour') = 0 THEN 0
            ELSE ROUND((SELECT COUNT(*) FROM tara.denylist WHERE denylisted_at > NOW() - INTERVAL '1 hour') * 100.0 / NULLIF((SELECT COUNT(*) FROM tara.jwt_metadata WHERE issued_at > NOW() - INTERVAL '1 hour'), 0), 2)
        END as revocation_rate
) rates
WHERE revocation_rate > 10;  -- Alert if > 10% revocation rate

-- Old OAuth states (potential issues)
SELECT COUNT(*) as stuck_oauth_flows
FROM tara.oauth_state
WHERE created_at < NOW() - INTERVAL '1 hour';  -- Alert if any exist

-- Database growth rate
SELECT
    'Database size growth check' as check_name,
    pg_size_pretty(pg_database_size('tim')) as current_size,
    'Monitor for unexpected growth' as recommendation;
```

### Automated Monitoring Script

**Shell Script for Health Monitoring:**
```bash
#!/bin/bash
# tim-db-health-check.sh

DB_CONTAINER="tim-postgres"
DB_USER="tim"
DB_NAME="tim"
ALERT_EMAIL="admin@example.com"

# Function to run SQL query
run_query() {
    docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c "$1"
}

# Check database connectivity
echo "Checking database connectivity..."
if ! run_query "SELECT 1;" > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to database"
    # Send alert email here
    exit 1
fi

# Check table counts
echo "Checking table integrity..."
CUSTOM_METADATA_COUNT=$(run_query "SELECT COUNT(*) FROM custom.jwt_metadata;" | tr -d ' ')
CUSTOM_DENYLIST_COUNT=$(run_query "SELECT COUNT(*) FROM custom.denylist;" | tr -d ' ')
TARA_OAUTH_COUNT=$(run_query "SELECT COUNT(*) FROM tara.oauth_state;" | tr -d ' ')

echo "Custom metadata: $CUSTOM_METADATA_COUNT"
echo "Custom denylist: $CUSTOM_DENYLIST_COUNT"
echo "TARA OAuth states: $TARA_OAUTH_COUNT"

# Check for stuck OAuth flows
if [ "$TARA_OAUTH_COUNT" -gt 100 ]; then
    echo "WARNING: High number of OAuth states ($TARA_OAUTH_COUNT)"
    # Send warning email here
fi

# Check for high revocation rate
REVOCATION_RATE=$(run_query "
    SELECT ROUND(
        CASE WHEN total.count = 0 THEN 0
        ELSE revoked.count * 100.0 / total.count END, 2
    )
    FROM
        (SELECT COUNT(*) as count FROM custom.jwt_metadata WHERE issued_at > NOW() - INTERVAL '1 hour') total,
        (SELECT COUNT(*) as count FROM custom.denylist WHERE denylisted_at > NOW() - INTERVAL '1 hour') revoked;
" | tr -d ' ')

if (( $(echo "$REVOCATION_RATE > 10" | bc -l) )); then
    echo "ALERT: High revocation rate: $REVOCATION_RATE%"
    # Send alert email here
fi

echo "Health check completed successfully"
```

## Troubleshooting

### Common Issues and Solutions

**Issue 1: High Database Connection Count**
```sql
-- Diagnose connection issues
SELECT
    state,
    COUNT(*) as connections,
    array_agg(DISTINCT application_name) as apps
FROM pg_stat_activity
WHERE datname = 'tim'
GROUP BY state;

-- Solution: Check for connection leaks in application
-- Increase connection pool if necessary
```

**Issue 2: Slow Query Performance**
```sql
-- Identify slow operations
SELECT
    query,
    mean_exec_time,
    calls,
    total_exec_time
FROM pg_stat_statements
WHERE query LIKE '%jwt_metadata%' OR query LIKE '%denylist%'
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Solution: Add missing indexes, optimize queries
```

**Issue 3: Database Storage Growth**
```sql
-- Analyze storage usage
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_total_relation_size(schemaname||'.'||tablename) as size_bytes
FROM pg_tables
WHERE schemaname IN ('custom', 'tara')
ORDER BY size_bytes DESC;

-- Solution: Implement regular cleanup, archival procedures
```

**Issue 4: Data Inconsistency**
```sql
-- Check for data inconsistencies
SELECT
    'Orphaned denylist entries' as issue,
    COUNT(*) as count
FROM custom.denylist d
LEFT JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE m.jwt_uuid IS NULL

UNION ALL

SELECT
    'Future-dated tokens' as issue,
    COUNT(*) as count
FROM custom.jwt_metadata
WHERE issued_at > NOW() + INTERVAL '5 minutes';

-- Solution: Run data integrity checks, cleanup procedures
```

### Recovery Procedures

**Procedure 1: Reset Specific Schema**
```bash
#!/bin/bash
# reset-custom-schema.sh

echo "WARNING: This will delete all custom schema data"
read -p "Are you sure? (yes/no): " confirm

if [ "$confirm" = "yes" ]; then
    docker exec tim-postgres psql -U tim -d tim -c "
        DROP SCHEMA IF EXISTS custom CASCADE;
        CREATE SCHEMA custom;
    "

    # Re-run initialization script for custom schema only
    docker exec tim-postgres psql -U tim -d tim -c "$(grep -A 20 'CREATE SCHEMA.*custom' /docker-entrypoint-initdb.d/00-init.sql)"

    echo "Custom schema reset completed"
fi
```

**Procedure 2: Emergency Cleanup**
```sql
-- Emergency cleanup for space issues
BEGIN;

-- Remove very old data first
DELETE FROM custom.jwt_metadata WHERE expires_at < NOW() - INTERVAL '7 days';
DELETE FROM tara.jwt_metadata WHERE expires_at < NOW() - INTERVAL '7 days';

-- Clean all expired entries
DELETE FROM custom.denylist WHERE expires_at < NOW();
DELETE FROM tara.denylist WHERE expires_at < NOW();
DELETE FROM custom.allowlist WHERE expires_at < NOW();
DELETE FROM tara.allowlist WHERE expires_at < NOW();
DELETE FROM tara.oauth_state WHERE created_at < NOW() - INTERVAL '15 minutes';

-- Vacuum immediately
VACUUM custom.jwt_metadata;
VACUUM tara.jwt_metadata;
VACUUM custom.denylist;
VACUUM tara.denylist;

COMMIT;

-- Report space reclaimed
SELECT pg_size_pretty(pg_database_size('tim')) as database_size;
```

---

## Related Documentation

- **[Database Setup](./01-database-setup.md)**: Initial setup and configuration
- **[Schema Architecture](./02-schema-architecture.md)**: Database design principles
- **[Custom JWT Tables](./03-custom-tables.md)**: Custom schema operations
- **[TARA Tables](./04-tara-tables.md)**: TARA schema operations
- **[JWT-Database Correlation](./05-jwt-correlation.md)**: Token-database relationships