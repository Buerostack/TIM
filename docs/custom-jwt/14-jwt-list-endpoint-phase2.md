# JWT List Endpoint - Phase 2: Administrative Token Management

## Overview

Phase 2 extends the JWT listing functionality to provide administrators with comprehensive token management capabilities across all users. This includes advanced filtering, bulk operations, security monitoring, and compliance reporting features.

**Endpoint**: `POST /jwt/custom/list/admin`
**Security Level**: Administrator-only (cross-user access)
**Implementation Status**: ✅ Complete

## API Reference

### Administrative Listing Endpoint

```http
POST /jwt/custom/list/admin
Content-Type: application/json
Authorization: Bearer <admin-token>
```

### Request Format

```json
{
  "subject": "user123",
  "status": "active",
  "issued_after": "2024-01-01T00:00:00Z",
  "issued_before": "2024-01-31T23:59:59Z",
  "expires_after": "2024-01-01T00:00:00Z",
  "expires_before": "2024-12-31T23:59:59Z",
  "jwt_name": "SESSION_TOKEN",
  "issuer": "TIM",
  "audience": "api",
  "revocation_reason": "security_incident",
  "limit": 100,
  "offset": 0,
  "include_claims": true,
  "sort_by": "issued_at",
  "sort_order": "desc"
}
```

### Request Parameters

| Parameter | Type | Required | Description | Default | Max |
|-----------|------|----------|-------------|---------|-----|
| `subject` | string | No | Filter by specific user (null = all users) | null | - |
| `status` | string | No | Token status: `active`, `expired`, `revoked`, `all` | `all` | - |
| `issued_after` | string | No | ISO 8601 datetime filter | - | - |
| `issued_before` | string | No | ISO 8601 datetime filter | - | - |
| `expires_after` | string | No | ISO 8601 datetime filter | - | - |
| `expires_before` | string | No | ISO 8601 datetime filter | - | - |
| `jwt_name` | string | No | Filter by JWT name (exact match) | - | - |
| `issuer` | string | No | Filter by token issuer | - | - |
| `audience` | string | No | Filter by audience (contains match) | - | - |
| `revocation_reason` | string | No | Filter by revocation reason | - | - |
| `limit` | integer | No | Tokens per page | 50 | 500 |
| `offset` | integer | No | Pagination offset | 0 | - |
| `include_claims` | boolean | No | Include detailed claim information | false | - |
| `sort_by` | string | No | Sort field: `issued_at`, `expires_at`, `subject` | `issued_at` | - |
| `sort_order` | string | No | Sort direction: `asc`, `desc` | `desc` | - |

### Response Format

#### Administrative Response (HTTP 200)

```json
{
  "tokens": [
    {
      "jti": "41ea92a2-af8d-44cd-97eb-afba7eaff632",
      "subject": "user123",
      "status": "active",
      "issued_at": "2024-01-15T10:30:00Z",
      "expires_at": "2024-01-16T10:30:00Z",
      "revoked_at": null,
      "revocation_reason": null,
      "jwt_name": "SESSION_TOKEN",
      "issuer": "TIM",
      "audience": "api,web",
      "claims": "sub,role,permissions,department",
      "claim_details": {
        "sub": "user123",
        "role": "manager",
        "permissions": ["read", "write"],
        "department": "engineering"
      }
    },
    {
      "jti": "7b0c397c-2b86-4ffe-8f29-8fe313af4699",
      "subject": "admin456",
      "status": "revoked",
      "issued_at": "2024-01-14T09:15:00Z",
      "expires_at": "2024-01-15T09:15:00Z",
      "revoked_at": "2024-01-14T15:30:00Z",
      "revocation_reason": "security_incident",
      "jwt_name": "ADMIN_TOKEN",
      "issuer": "TIM",
      "audience": "admin",
      "claims": "sub,role,scope",
      "claim_details": {
        "sub": "admin456",
        "role": "admin",
        "scope": "full_access"
      }
    }
  ],
  "pagination": {
    "total": 15432,
    "limit": 100,
    "offset": 0,
    "has_more": true
  },
  "summary": {
    "total_active": 8234,
    "total_expired": 4521,
    "total_revoked": 2677,
    "users_with_tokens": 1205,
    "most_common_reasons": [
      {"reason": "user_logout", "count": 1245},
      {"reason": "security_incident", "count": 89}
    ]
  }
}
```

## Administrative Use Cases

### 1. Security Incident Response

**Scenario**: "Security team needs to identify all admin tokens created in the last 24 hours"

```bash
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "issued_after": "2024-01-14T00:00:00Z",
    "audience": "admin",
    "status": "active",
    "limit": 500
  }'
```

### 2. User Token Audit

**Scenario**: "Investigate specific user's token activity"

```bash
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "subject": "suspicious_user",
    "include_claims": true
  }'
```

### 3. Compliance Reporting

**Scenario**: "Generate monthly compliance report of all revoked tokens"

```bash
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "status": "revoked",
    "issued_after": "2024-01-01T00:00:00Z",
    "issued_before": "2024-01-31T23:59:59Z",
    "limit": 1000
  }'
```

### 4. Token Expiration Management

**Scenario**: "Find tokens expiring in the next hour for proactive renewal"

```bash
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "status": "active",
    "expires_before": "2024-01-15T11:30:00Z",
    "sort_by": "expires_at",
    "sort_order": "asc"
  }'
```

### 5. Bulk Operations Support

**Scenario**: "Identify tokens for bulk revocation by pattern"

```bash
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "jwt_name": "OLD_API_TOKEN",
    "status": "active",
    "limit": 100
  }'

# Extract JTIs for bulk revocation
# Then use bulk revoke endpoint with collected JTIs
```

## Administrative Dashboard Integration

### React Admin Dashboard

```javascript
import { useState, useEffect } from 'react';
import { AdminTokenFilters, TokenTable, BulkActions } from './components';

function AdminTokenManagement() {
  const [tokens, setTokens] = useState([]);
  const [pagination, setPagination] = useState(null);
  const [summary, setSummary] = useState(null);
  const [selectedTokens, setSelectedTokens] = useState([]);
  const [filters, setFilters] = useState({
    limit: 100,
    offset: 0
  });

  const fetchTokens = async () => {
    try {
      const response = await fetch('/jwt/custom/list/admin', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${adminToken}`
        },
        body: JSON.stringify(filters)
      });

      const data = await response.json();
      setTokens(data.tokens);
      setPagination(data.pagination);
      setSummary(data.summary);
    } catch (error) {
      console.error('Failed to fetch tokens:', error);
    }
  };

  useEffect(() => {
    fetchTokens();
  }, [filters]);

  const handleBulkRevoke = async (reason) => {
    const tokenJtis = selectedTokens.map(token => token.jti);

    try {
      const response = await fetch('/jwt/custom/revoke/bulk', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tokens: tokenJtis,
          reason: reason
        })
      });

      if (response.ok) {
        // Refresh token list
        fetchTokens();
        setSelectedTokens([]);
      }
    } catch (error) {
      console.error('Bulk revocation failed:', error);
    }
  };

  return (
    <div className="admin-token-management">
      <h1>JWT Token Administration</h1>

      <div className="summary-cards">
        <SummaryCard title="Active Tokens" value={summary?.total_active} />
        <SummaryCard title="Revoked Today" value={summary?.total_revoked} />
        <SummaryCard title="Users with Tokens" value={summary?.users_with_tokens} />
      </div>

      <AdminTokenFilters
        filters={filters}
        onFilterChange={setFilters}
      />

      <BulkActions
        selectedCount={selectedTokens.length}
        onBulkRevoke={handleBulkRevoke}
        onClearSelection={() => setSelectedTokens([])}
      />

      <TokenTable
        tokens={tokens}
        selectedTokens={selectedTokens}
        onSelectionChange={setSelectedTokens}
        onRefresh={fetchTokens}
      />

      <Pagination
        pagination={pagination}
        onPageChange={(newOffset) =>
          setFilters(prev => ({...prev, offset: newOffset}))
        }
      />
    </div>
  );
}
```

### Dashboard Components

```javascript
// Summary Card Component
function SummaryCard({ title, value, trend }) {
  return (
    <div className="summary-card">
      <h3>{title}</h3>
      <div className="value">{value?.toLocaleString()}</div>
      {trend && <div className={`trend ${trend.direction}`}>{trend.text}</div>}
    </div>
  );
}

// Advanced Filters Component
function AdminTokenFilters({ filters, onFilterChange }) {
  const [localFilters, setLocalFilters] = useState(filters);

  const handleSubmit = (e) => {
    e.preventDefault();
    onFilterChange(localFilters);
  };

  return (
    <form onSubmit={handleSubmit} className="admin-filters">
      <div className="filter-row">
        <input
          type="text"
          placeholder="User ID"
          value={localFilters.subject || ''}
          onChange={(e) => setLocalFilters(prev => ({
            ...prev, subject: e.target.value || null
          }))}
        />

        <select
          value={localFilters.status || ''}
          onChange={(e) => setLocalFilters(prev => ({
            ...prev, status: e.target.value || null
          }))}
        >
          <option value="">All Statuses</option>
          <option value="active">Active</option>
          <option value="expired">Expired</option>
          <option value="revoked">Revoked</option>
        </select>

        <input
          type="text"
          placeholder="JWT Name"
          value={localFilters.jwt_name || ''}
          onChange={(e) => setLocalFilters(prev => ({
            ...prev, jwt_name: e.target.value || null
          }))}
        />
      </div>

      <div className="filter-row">
        <input
          type="datetime-local"
          placeholder="Issued After"
          onChange={(e) => setLocalFilters(prev => ({
            ...prev, issued_after: e.target.value ? new Date(e.target.value).toISOString() : null
          }))}
        />

        <input
          type="datetime-local"
          placeholder="Issued Before"
          onChange={(e) => setLocalFilters(prev => ({
            ...prev, issued_before: e.target.value ? new Date(e.target.value).toISOString() : null
          }))}
        />

        <button type="submit">Apply Filters</button>
        <button type="button" onClick={() => {
          setLocalFilters({ limit: 100, offset: 0 });
          onFilterChange({ limit: 100, offset: 0 });
        }}>
          Clear Filters
        </button>
      </div>
    </form>
  );
}

// Bulk Actions Component
function BulkActions({ selectedCount, onBulkRevoke, onClearSelection }) {
  const [revokeReason, setRevokeReason] = useState('');
  const [showBulkMenu, setShowBulkMenu] = useState(false);

  if (selectedCount === 0) return null;

  return (
    <div className="bulk-actions">
      <div className="selection-info">
        {selectedCount} tokens selected
        <button onClick={onClearSelection}>Clear Selection</button>
      </div>

      <div className="bulk-menu">
        <button onClick={() => setShowBulkMenu(!showBulkMenu)}>
          Bulk Actions ▼
        </button>

        {showBulkMenu && (
          <div className="bulk-dropdown">
            <div className="bulk-revoke">
              <input
                type="text"
                placeholder="Revocation reason"
                value={revokeReason}
                onChange={(e) => setRevokeReason(e.target.value)}
              />
              <button
                onClick={() => onBulkRevoke(revokeReason)}
                disabled={!revokeReason.trim()}
              >
                Revoke Selected ({selectedCount})
              </button>
            </div>

            <div className="bulk-export">
              <button onClick={() => exportSelected('csv')}>
                Export as CSV
              </button>
              <button onClick={() => exportSelected('json')}>
                Export as JSON
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
```

## Advanced Security Features

### Authorization Levels

```yaml
Admin Roles:
  super_admin:
    - View all tokens across all users
    - Bulk revocation capabilities
    - System-wide analytics access
    - Compliance report generation

  security_admin:
    - View tokens for security investigations
    - Revoke tokens with security reasons
    - Access to security metrics
    - Limited to security-related filters

  user_admin:
    - View tokens for specific users
    - User management operations
    - Limited bulk operations
    - User-scoped analytics
```

### Audit Trail Enhancement

```json
{
  "admin_action": {
    "admin_user": "admin@company.com",
    "action": "bulk_token_listing",
    "timestamp": "2024-01-15T14:30:00Z",
    "filters_used": {
      "subject": "user123",
      "status": "active"
    },
    "tokens_accessed": 45,
    "ip_address": "10.0.1.100",
    "user_agent": "TIM-Admin-Dashboard/1.0"
  }
}
```

## Performance Optimization

### Database Query Optimization

```sql
-- Efficient admin queries with proper indexing
CREATE INDEX idx_custom_jwt_metadata_admin_composite
ON custom.jwt_metadata (subject, issued_at, expires_at);

CREATE INDEX idx_custom_jwt_metadata_jwt_name
ON custom.jwt_metadata (jwt_name);

CREATE INDEX idx_custom_jwt_metadata_issuer
ON custom.jwt_metadata (issuer);

CREATE INDEX idx_custom_denylist_reason
ON custom.denylist (reason);
```

### Caching Strategy

```yaml
caching_layers:
  frequent_queries:
    technology: "Redis"
    ttl: "5 minutes"
    keys: ["admin:tokens:{filters_hash}", "summary:stats"]

  heavy_analytics:
    technology: "Materialized Views"
    refresh: "hourly"
    queries: ["user_token_counts", "revocation_statistics"]

  export_cache:
    technology: "File System"
    ttl: "1 hour"
    format: ["csv", "json"]
```

## Monitoring & Analytics

### Security Monitoring Queries

```bash
# 1. Suspicious token creation patterns
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "issued_after": "2024-01-15T12:00:00Z",
    "limit": 500,
    "sort_by": "subject"
  }'

# 2. Mass revocation investigation
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -d '{
    "status": "revoked",
    "revocation_reason": "security_incident",
    "issued_after": "2024-01-14T00:00:00Z"
  }'

# 3. Long-lived token audit
curl -X POST http://localhost:8085/jwt/custom/list/admin \
  -d '{
    "status": "active",
    "issued_before": "2024-01-01T00:00:00Z",
    "sort_by": "issued_at",
    "sort_order": "asc"
  }'
```

### Operational Monitoring

```javascript
// Admin dashboard real-time monitoring
class TokenMonitoringService {
  async getSecurityMetrics() {
    const response = await fetch('/jwt/custom/list/admin', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`
      },
      body: JSON.stringify({
        status: 'revoked',
        issued_after: new Date(Date.now() - 24*60*60*1000).toISOString()
      })
    });

    return response.json();
  }

  async getExpiringTokens() {
    const oneHourFromNow = new Date(Date.now() + 60*60*1000);

    return fetch('/jwt/custom/list/admin', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`
      },
      body: JSON.stringify({
        status: 'active',
        expires_before: oneHourFromNow.toISOString(),
        sort_by: 'expires_at'
      })
    });
  }

  async getUserTokenDistribution() {
    // This would require aggregation queries
    // Could be implemented with database views or analytics endpoints
  }
}
```

## Compliance & Reporting

### GDPR Compliance

```javascript
// User data export for GDPR requests
async function exportUserData(userId) {
  const response = await fetch('/jwt/custom/list/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${adminToken}`
    },
    body: JSON.stringify({
      subject: userId,
      include_claims: true,
      limit: 1000
    })
  });

  const userData = await response.json();

  return {
    user_id: userId,
    total_tokens: userData.pagination.total,
    active_tokens: userData.tokens.filter(t => t.status === 'active').length,
    data_retention_info: {
      oldest_token: userData.tokens[userData.tokens.length - 1]?.issued_at,
      newest_token: userData.tokens[0]?.issued_at
    },
    tokens: userData.tokens
  };
}

// Right to erasure implementation
async function deleteUserTokens(userId, reason = "user_request_erasure") {
  // 1. List all user tokens
  const tokens = await listUserTokens(userId);

  // 2. Revoke all active tokens
  const activeTokens = tokens.filter(t => t.status === 'active');
  if (activeTokens.length > 0) {
    await bulkRevokeTokens(activeTokens.map(t => t.jti), reason);
  }

  // 3. Schedule metadata cleanup (after retention period)
  scheduleMetadataCleanup(userId, reason);
}
```

### SOX Compliance

```sql
-- Audit trail queries for SOX compliance
-- 1. Token access patterns
SELECT
  subject,
  COUNT(*) as total_tokens,
  COUNT(*) FILTER (WHERE d.jwt_uuid IS NOT NULL) as revoked_count,
  MIN(issued_at) as first_token,
  MAX(issued_at) as latest_token
FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE issued_at > NOW() - INTERVAL '90 days'
GROUP BY subject
HAVING COUNT(*) > 100;

-- 2. Administrative actions audit
SELECT
  admin_user,
  action_type,
  tokens_affected,
  timestamp,
  justification
FROM admin_audit_log
WHERE action_type IN ('bulk_revoke', 'bulk_list', 'user_token_access')
  AND timestamp > NOW() - INTERVAL '1 year';
```

## Error Handling

### Administrative Error Responses

```json
// Insufficient privileges
{
  "error": "insufficient_privileges",
  "message": "Admin access required for cross-user token listing",
  "required_role": "admin"
}

// Invalid filter combination
{
  "error": "invalid_filters",
  "message": "Cannot combine subject filter with user-scoped endpoint",
  "suggestion": "Use /list/admin endpoint for cross-user queries"
}

// Rate limit exceeded
{
  "error": "rate_limit_exceeded",
  "message": "Too many admin requests. Try again in 60 seconds",
  "retry_after": 60
}
```

## Integration with Existing Endpoints

### Workflow Integration

```javascript
// Complete token management workflow
class TokenManagementWorkflow {
  async investigateUser(userId) {
    // 1. List user's tokens
    const tokens = await this.listUserTokens(userId);

    // 2. Identify suspicious patterns
    const suspiciousTokens = tokens.filter(token =>
      this.isSuspicious(token)
    );

    // 3. Revoke if necessary
    if (suspiciousTokens.length > 0) {
      await this.bulkRevoke(
        suspiciousTokens.map(t => t.jti),
        'security_investigation'
      );
    }

    // 4. Generate investigation report
    return this.generateReport(userId, tokens, suspiciousTokens);
  }

  async cleanupExpiredTokens() {
    // 1. Find tokens expiring soon
    const expiringTokens = await this.listTokensByExpiration();

    // 2. Notify users about expiring tokens
    await this.notifyExpiringTokens(expiringTokens);

    // 3. Clean up already expired metadata
    await this.cleanupExpiredMetadata();
  }
}
```

## Performance Benchmarks

### Expected Performance

| Operation | Target | Acceptable | Poor |
|-----------|--------|------------|------|
| Simple listing (< 100 tokens) | < 100ms | < 500ms | > 1s |
| Complex filtering | < 300ms | < 1s | > 3s |
| Large result set (1000+ tokens) | < 1s | < 3s | > 10s |
| Cross-user queries | < 500ms | < 2s | > 5s |

### Optimization Strategies

```yaml
query_optimization:
  database_level:
    - Proper indexing strategy
    - Query plan analysis
    - Connection pooling

  application_level:
    - Result caching
    - Lazy loading for large datasets
    - Async processing for heavy operations

  frontend_level:
    - Virtual scrolling for large lists
    - Debounced search inputs
    - Progressive loading
```

## Related Documentation

- **[Phase 1 Documentation](./13-jwt-list-endpoint-phase1.md)**: Basic user token listing
- **[Phase 3 Advanced Features](./TODO/API%20Features/JWT-List-Phase3-Advanced-Features.md)**: Future enterprise features
- **[JWT Revocation Endpoint](./10-jwt-revocation-endpoint.md)**: Token revocation integration
- **[Bulk Revocation](./12-jwt-bulk-revoke-endpoint.md)**: Bulk operations integration
- **[Database Schema](./databases/03-custom-tables.md)**: Database structure reference

---

**Implementation Status**: ✅ **Complete - Ready for Production Use**

This Phase 2 implementation provides comprehensive administrative capabilities for JWT token management with proper security controls, performance optimization, and integration capabilities.