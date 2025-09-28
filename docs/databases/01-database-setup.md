# Database Setup and Initialization

## Overview

TIM uses PostgreSQL as its primary database, automatically initialized through Docker Compose with predefined schemas and tables. The setup is designed for development and production environments with minimal configuration required.

## Docker Infrastructure

### Container Configuration

**Docker Compose Setup:**
```yaml
services:
  postgres:
    image: postgres:16
    container_name: tim-postgres
    environment:
      POSTGRES_DB: tim
      POSTGRES_USER: tim
      POSTGRES_PASSWORD: 123
    ports:
      - "9876:5432"
    volumes:
      - tim-pgdata:/var/lib/postgresql/data
      - ./db/init.sql:/docker-entrypoint-initdb.d/00-init.sql:ro
```

**Key Configuration Details:**
- **PostgreSQL Version**: 16 (latest stable)
- **Database Name**: `tim`
- **Default User**: `tim` / `123` (development credentials)
- **External Port**: `9876` (mapped from container port `5432`)
- **Persistent Storage**: `tim-pgdata` Docker volume
- **Initialization**: Automatic via `/docker-entrypoint-initdb.d/00-init.sql`

### Application Database Connection

**Spring Boot Configuration:**
```properties
# Database Connection
spring.datasource.url=jdbc:postgresql://postgres:5432/tim
spring.datasource.username=tim
spring.datasource.password=123

# JPA/Hibernate Settings
spring.jpa.hibernate.ddl-auto=none       # Manual schema management
spring.liquibase.enabled=false           # No migration framework
spring.datasource.hikari.maximum-pool-size=5
```

**Environment Variable Overrides:**
```yaml
# Docker Compose environment section
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tim
  SPRING_DATASOURCE_USERNAME: tim
  SPRING_DATASOURCE_PASSWORD: 123
  SPRING_JPA_HIBERNATE_DDL_AUTO: none
  SPRING_LIQUIBASE_ENABLED: "false"
```

## Automatic Initialization Process

### Initialization Script: `db/init.sql`

The database is automatically initialized when the PostgreSQL container starts for the first time. The initialization script creates:

**1. Schema Creation:**
```sql
CREATE SCHEMA IF NOT EXISTS custom;
CREATE SCHEMA IF NOT EXISTS tara;
```

**2. Table Creation Order:**
```sql
-- Custom JWT tables
CREATE TABLE IF NOT EXISTS custom.denylist (...);
CREATE TABLE IF NOT EXISTS custom.allowlist (...);
CREATE TABLE IF NOT EXISTS custom.jwt_metadata (...);

-- TARA OAuth tables
CREATE TABLE IF NOT EXISTS tara.denylist (...);
CREATE TABLE IF NOT EXISTS tara.allowlist (...);
CREATE TABLE IF NOT EXISTS tara.jwt_metadata (...);
CREATE TABLE IF NOT EXISTS tara.oauth_state (...);
```

**3. Index Creation:**
```sql
-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_custom_denylist_exp ON custom.denylist (expires_at);
CREATE INDEX IF NOT EXISTS idx_custom_allowlist_exp ON custom.allowlist (expires_at);
CREATE INDEX IF NOT EXISTS idx_tara_denylist_exp ON tara.denylist (expires_at);
CREATE INDEX IF NOT EXISTS idx_tara_allowlist_exp ON tara.allowlist (expires_at);
```

### First-Time Setup Flow

**1. Container Startup:**
```bash
docker-compose up -d postgres
```

**2. Automatic Process:**
- PostgreSQL container starts
- Database `tim` is created
- User `tim` is created with full permissions
- `00-init.sql` script executes automatically
- All schemas, tables, and indexes are created
- Container is ready for connections

**3. Verification:**
```bash
# Connect to database
docker exec -it tim-postgres psql -U tim -d tim

# Verify schemas
\dn

# Verify tables
\dt custom.*
\dt tara.*
```

## Schema Design Rationale

### Two-Schema Architecture

**Why Separate Schemas?**

**`custom` Schema:**
- **Purpose**: Custom JWT functionality developed by TIM
- **Independence**: Self-contained JWT management
- **Control**: Full control over token lifecycle
- **Use Case**: API tokens, service-to-service authentication

**`tara` Schema:**
- **Purpose**: TARA (Estonian e-ID) OAuth integration
- **Compliance**: Government identity standards
- **Isolation**: Separate from custom token logic
- **Use Case**: Citizen authentication, government services

**Benefits of Separation:**
1. **Security Isolation**: TARA tokens cannot interfere with custom tokens
2. **Compliance**: TARA requirements don't affect custom functionality
3. **Maintainability**: Clear separation of concerns
4. **Scalability**: Independent scaling and optimization
5. **Auditing**: Separate audit trails for different token types

### Identical Table Structures

**Why Are Tables Nearly Identical?**

Both schemas have identical table structures but serve different purposes:

**Shared Patterns:**
```sql
-- Both schemas have:
jwt_metadata (jwt_uuid, claim_keys, issued_at, expires_at)
denylist (jwt_uuid, denylisted_at, expires_at)
allowlist (jwt_hash, expires_at)
```

**Rationale for Duplication:**
1. **Functional Separation**: Different token ecosystems
2. **Data Isolation**: No cross-contamination of token data
3. **Independent Operations**: Separate maintenance and cleanup
4. **Security**: TARA tokens have different validation rules
5. **Compliance**: Government vs. commercial token requirements

**TARA-Specific Addition:**
```sql
-- Only in TARA schema
tara.oauth_state (state, created_at, pkce_verifier)
```

## Database Schema Management

### No Migration Framework

**Current Approach:**
- **Manual Schema Control**: `spring.jpa.hibernate.ddl-auto=none`
- **Disabled Liquibase**: `spring.liquibase.enabled=false`
- **Init Script**: Single `db/init.sql` for setup

**Benefits:**
- **Predictable**: No automatic schema changes
- **Simple**: Single initialization script
- **Controlled**: Manual approval for schema changes
- **Docker-Native**: Leverages PostgreSQL init mechanisms

**Limitations:**
- **Manual Updates**: Schema changes require script updates
- **No Versioning**: No automatic migration history
- **Rollback**: Manual rollback procedures required

### Schema Change Process

**For Development:**
1. Modify `db/init.sql`
2. Recreate database: `docker-compose down -v && docker-compose up -d`
3. Test application startup

**For Production:**
1. Create migration script
2. Backup database
3. Apply changes manually
4. Update `db/init.sql` for future deployments
5. Test rollback procedures

## Performance Considerations

### Connection Pooling

**HikariCP Configuration:**
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
```

**Pool Sizing Rationale:**
- **Max 5 Connections**: Suitable for moderate load
- **Min 2 Idle**: Maintains ready connections
- **Fast Timeout**: Quick failure detection
- **Container Resource**: Optimized for Docker deployment

### Index Strategy

**Performance Indexes:**
- **Expiration Indexes**: Enable efficient cleanup operations
- **Primary Keys**: UUID-based for distributed systems
- **No Full-Text**: Simple text searches only

**Missing Indexes (Potential Additions):**
```sql
-- Time-based queries
CREATE INDEX idx_custom_jwt_metadata_issued ON custom.jwt_metadata (issued_at);
CREATE INDEX idx_tara_jwt_metadata_issued ON tara.jwt_metadata (issued_at);

-- Claim-based queries
CREATE INDEX idx_custom_jwt_metadata_claims ON custom.jwt_metadata (claim_keys);
```

## Backup and Recovery

### Data Persistence

**Docker Volume:**
- **Volume Name**: `tim-pgdata`
- **Location**: Docker manages location
- **Persistence**: Survives container recreation
- **Backup**: Manual export required

**Backup Strategy:**
```bash
# Export entire database
docker exec tim-postgres pg_dump -U tim tim > tim-backup.sql

# Export specific schema
docker exec tim-postgres pg_dump -U tim -n custom tim > custom-backup.sql

# Restore from backup
docker exec -i tim-postgres psql -U tim tim < tim-backup.sql
```

### Disaster Recovery

**Recovery Scenarios:**
1. **Container Loss**: Data persists in Docker volume
2. **Volume Loss**: Restore from database backup
3. **Corruption**: Restore from backup, replay recent changes
4. **Schema Issues**: Recreate from `init.sql`

## Security Configuration

### Access Control

**Database User Permissions:**
```sql
-- TIM application user has:
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA custom TO tim;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA tara TO tim;

-- No administrative permissions:
-- No CREATE, DROP, ALTER permissions
-- No superuser privileges
```

### Network Security

**Container Network:**
- **Internal Communication**: TIM app → PostgreSQL
- **External Access**: Port `9876` for development/debugging
- **Production**: Remove external port mapping

**Connection Security:**
```properties
# Development credentials (change for production)
POSTGRES_USER=tim
POSTGRES_PASSWORD=123  # Use strong passwords in production
```

## Monitoring and Health Checks

### Database Health

**Basic Connectivity:**
```bash
# Connection test
docker exec tim-postgres psql -U tim -d tim -c "SELECT 1;"

# Schema verification
docker exec tim-postgres psql -U tim -d tim -c "\dn"

# Table count verification
docker exec tim-postgres psql -U tim -d tim -c "
SELECT schemaname, count(*)
FROM pg_tables
WHERE schemaname IN ('custom', 'tara')
GROUP BY schemaname;"
```

### Performance Monitoring

**Connection Pool Status:**
```sql
SELECT count(*), state
FROM pg_stat_activity
WHERE datname = 'tim'
GROUP BY state;
```

**Table Growth:**
```sql
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_total_relation_size(schemaname||'.'||tablename) as size_bytes
FROM pg_tables
WHERE schemaname IN ('custom', 'tara')
ORDER BY size_bytes DESC;
```

## Troubleshooting

### Common Setup Issues

**1. Container Won't Start:**
```bash
# Check logs
docker logs tim-postgres

# Common causes:
# - Port 9876 already in use
# - Volume permission issues
# - Invalid init.sql syntax
```

**2. Connection Refused:**
```bash
# Verify container is running
docker ps | grep tim-postgres

# Check port mapping
docker port tim-postgres

# Test connection
docker exec tim-postgres psql -U tim -d tim -c "SELECT 1;"
```

**3. Schema Not Created:**
```bash
# Check init script execution
docker logs tim-postgres | grep init

# Verify schemas exist
docker exec tim-postgres psql -U tim -d tim -c "\dn"

# Manual schema creation
docker exec tim-postgres psql -U tim -d tim -f /docker-entrypoint-initdb.d/00-init.sql
```

### Recovery Procedures

**Complete Reset:**
```bash
# Stop and remove everything
docker-compose down -v

# Remove Docker volume (WARNING: DATA LOSS)
docker volume rm tim_tim-pgdata

# Restart fresh
docker-compose up -d postgres
```

**Schema-Only Reset:**
```bash
# Drop and recreate schemas
docker exec tim-postgres psql -U tim -d tim -c "
DROP SCHEMA IF EXISTS custom CASCADE;
DROP SCHEMA IF EXISTS tara CASCADE;
"

# Re-run init script
docker exec tim-postgres psql -U tim -d tim -f /docker-entrypoint-initdb.d/00-init.sql
```

## Development vs Production

### Development Setup

**Current Configuration (Development):**
- External port mapping for debugging
- Simple credentials
- Volume for data persistence
- No SSL/TLS encryption

### Production Recommendations

**Security Hardening:**
```yaml
services:
  postgres:
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/postgres_password
    secrets:
      - postgres_password
    # Remove external port mapping
    # ports: - "9876:5432"  # Remove this line
```

**Additional Security:**
- Use Docker secrets for passwords
- Enable SSL/TLS connections
- Implement network policies
- Regular security updates
- Monitoring and alerting

---

## Related Documentation

- **[Schema Architecture](./02-schema-architecture.md)**: Detailed schema design
- **[Custom JWT Tables](./03-custom-tables.md)**: Custom schema tables
- **[TARA Tables](./04-tara-tables.md)**: TARA schema tables
- **[JWT-Database Correlation](./05-jwt-correlation.md)**: How JWTs link to database