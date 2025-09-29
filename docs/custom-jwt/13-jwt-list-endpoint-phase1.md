# JWT List Endpoint - Phase 1: User Token Listing

## Overview

The JWT List endpoint Phase 1 provides users with the ability to view and manage their own JWT tokens. This endpoint enables users to see their active sessions, expired tokens, and revoked tokens with comprehensive filtering and pagination capabilities.

**Endpoint**: `POST /jwt/custom/list/me`
**Security Level**: User-scoped (users can only see their own tokens)
**Implementation Status**: ✅ Complete

## 🚀 Quick Start Guide

### Step 1: Generate a JWT Token

First, create a JWT token that you'll use for authentication:

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "jwt_name": "MY_SESSION",
    "content": {
      "sub": "user123",
      "role": "user"
    },
    "expiration_in_minutes": 60
  }'
```

**Response:**
```json
{
  "status": "created",
  "jwt_name": "MY_SESSION",
  "token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ...",
  "expires_at": "2024-01-01T13:00:00Z"
}
```

### Step 2: Copy the Token

Copy the `token` value from the response above.

### Step 3: List Your Tokens

Use the token from step 1 in the Authorization header:

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ..." \
  -d '{}'
```

**Response:**
```json
{
  "tokens": [
    {
      "jti": "12345678-1234-1234-1234-123456789abc",
      "subject": "user123",
      "jwt_name": "MY_SESSION",
      "status": "active",
      "issued_at": "2024-01-01T12:00:00Z",
      "expires_at": "2024-01-01T13:00:00Z",
      "issuer": "TIM",
      "audience": "tim-audience"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 0,
    "size": 20,
    "total_pages": 1
  }
}
```

### Step 4: Filter Your Results (Optional)

Add filters to the request body:

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "status": "active",
    "jwt_name": "API_TOKEN",
    "limit": 10
  }'
```

## 📬 Postman Setup

### Step-by-Step Postman Guide

**Step 1: Generate a Token**

1. Create a new POST request: `http://localhost:8085/jwt/custom/generate`
2. Set Headers: `Content-Type: application/json`
3. Set Body (raw JSON):
   ```json
   {
     "jwt_name": "POSTMAN_TEST",
     "content": {
       "sub": "postman-user",
       "role": "user"
     },
     "expiration_in_minutes": 60
   }
   ```
4. Send the request and copy the `token` value from the response

**Step 2: Set Up Environment Variable**

1. In Postman, create an Environment called "TIM JWT Testing"
2. Add a variable: `jwt_token` = `<paste-your-token-here>`
3. Select this environment for your requests

**Step 3: Create List Tokens Request**

1. Create a new POST request: `http://localhost:8085/jwt/custom/list/me`
2. Set Headers:
   - `Content-Type: application/json`
   - `Authorization: Bearer {{jwt_token}}`
3. Set Body (raw JSON):
   ```json
   {
     "limit": 10,
     "status": "active"
   }
   ```
4. Send the request - you should see your tokens!

**Step 4: Test Different Filters**

Try these different body configurations:

- **All tokens**: `{}`
- **Active only**: `{"status": "active"}`
- **With pagination**: `{"limit": 5, "offset": 0}`
- **Date filter**: `{"issued_after": "2024-01-01T00:00:00Z"}`
- **Token name**: `{"jwt_name": "POSTMAN_TEST"}`

### Expected Postman Response

```json
{
  "tokens": [
    {
      "jti": "12345678-1234-1234-1234-123456789abc",
      "subject": "postman-user",
      "jwt_name": "POSTMAN_TEST",
      "status": "active",
      "issued_at": "2024-01-01T12:00:00Z",
      "expires_at": "2024-01-01T13:00:00Z",
      "issuer": "TIM",
      "audience": "tim-audience"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 0,
    "size": 20,
    "total_pages": 1
  }
}
```

## API Reference

### Endpoint Details

```http
POST /jwt/custom/list/me
Content-Type: application/json
Authorization: Bearer <your-jwt-token>
```

**Authentication**: Required - You must provide a valid JWT token in the Authorization header. The endpoint extracts the subject from your token and returns only your tokens.

### Request Format

```json
{
  "status": "active",
  "issued_after": "2024-01-01T00:00:00Z",
  "issued_before": "2024-01-31T23:59:59Z",
  "expires_after": "2024-01-01T00:00:00Z",
  "expires_before": "2024-12-31T23:59:59Z",
  "jwt_name": "SESSION_TOKEN",
  "limit": 50,
  "offset": 0
}
```

### Request Parameters

| Parameter | Type | Required | Description | Default | Max |
|-----------|------|----------|-------------|---------|-----|
| `status` | string | No | Filter by token status: `active`, `expired`, `revoked`, `all` | `all` | - |
| `issued_after` | string | No | ISO 8601 datetime - tokens issued after this time | - | - |
| `issued_before` | string | No | ISO 8601 datetime - tokens issued before this time | - | - |
| `expires_after` | string | No | ISO 8601 datetime - tokens expiring after this time | - | - |
| `expires_before` | string | No | ISO 8601 datetime - tokens expiring before this time | - | - |
| `jwt_name` | string | No | Filter by JWT name (exact match) | - | - |
| `limit` | integer | No | Number of tokens per page | 50 | 100 |
| `offset` | integer | No | Number of tokens to skip (for pagination) | 0 | - |

**Note**: All parameters are optional. An empty request body `{}` will return all user tokens with default pagination.

### Response Format

#### Success Response (HTTP 200)

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
      "claims": "sub,role,permissions"
    },
    {
      "jti": "7b0c397c-2b86-4ffe-8f29-8fe313af4699",
      "subject": "user123",
      "status": "revoked",
      "issued_at": "2024-01-14T09:15:00Z",
      "expires_at": "2024-01-15T09:15:00Z",
      "revoked_at": "2024-01-14T15:30:00Z",
      "revocation_reason": "user_logout",
      "jwt_name": "API_TOKEN",
      "issuer": "TIM",
      "audience": "api",
      "claims": "sub,scope"
    }
  ],
  "pagination": {
    "total": 127,
    "limit": 50,
    "offset": 0,
    "has_more": true
  }
}
```

#### Token Status Values

| Status | Description | Criteria |
|--------|-------------|----------|
| `active` | Token is valid and usable | Not expired AND not revoked |
| `expired` | Token has passed expiration time | `expires_at` < current time |
| `revoked` | Token has been manually revoked | Present in denylist table |

#### Error Responses

**Authentication Required (HTTP 401)**
```json
{
  "error": "invalid_authorization",
  "message": "Authorization header must be in format 'Bearer <token>'"
}
```

**Invalid Token (HTTP 401)**
```json
{
  "error": "invalid_token",
  "message": "Token is invalid, expired, or revoked"
}
```

**Missing Subject (HTTP 401)**
```json
{
  "error": "missing_subject",
  "message": "Token does not contain a valid subject claim"
}
```

**Request Error (HTTP 400)**
```json
{
  "error": "listing_failed",
  "message": "Failed to list tokens: Invalid datetime format"
}
```

## Use Cases

### 1. View All Active Sessions

**User Story**: "As a user, I want to see all my currently active tokens to understand my active sessions."

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"status": "active"}'
```

**Expected Response**: List of all non-expired, non-revoked tokens for the user.

### 2. Security Audit - Recent Token Activity

**User Story**: "As a user, I want to see all tokens created in the last 7 days to audit my recent activity."

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "issued_after": "2024-01-08T00:00:00Z",
    "limit": 100
  }'
```

### 3. Session Management - Find Specific Token Type

**User Story**: "As a user, I want to find all my API tokens to manage my programmatic access."

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"jwt_name": "API_TOKEN"}'
```

### 4. Cleanup - View Expired Tokens

**User Story**: "As a user, I want to see my expired tokens to understand my usage patterns."

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -d '{"status": "expired"}'
```

### 5. Pagination - Browse Large Token Lists

**User Story**: "As a user with many tokens, I want to paginate through my token list."

```bash
# Get first page
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -d '{"limit": 20, "offset": 0}'

# Get second page
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -d '{"limit": 20, "offset": 20}'
```

## Frontend Integration Examples

### React Hook for Token Listing

```javascript
import { useState, useEffect } from 'react';

function useUserTokens(filters = {}) {
  const [tokens, setTokens] = useState([]);
  const [pagination, setPagination] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchTokens = async (requestFilters = filters) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/jwt/custom/list/me', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestFilters)
      });

      if (response.ok) {
        const data = await response.json();
        setTokens(data.tokens);
        setPagination(data.pagination);
      } else {
        const errorData = await response.json();
        setError(errorData.message);
      }
    } catch (err) {
      setError('Failed to fetch tokens');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTokens();
  }, []);

  return {
    tokens,
    pagination,
    loading,
    error,
    refetch: fetchTokens
  };
}

// Usage in component
function TokenListPage() {
  const { tokens, pagination, loading, error, refetch } = useUserTokens();
  const [filters, setFilters] = useState({});

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
    refetch(newFilters);
  };

  if (loading) return <div>Loading tokens...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h1>My JWT Tokens</h1>
      <TokenFilters onFilterChange={handleFilterChange} />
      <TokenTable tokens={tokens} />
      <Pagination
        pagination={pagination}
        onPageChange={(offset) => refetch({...filters, offset})}
      />
    </div>
  );
}
```

### Vue.js Integration

```vue
<template>
  <div class="token-list">
    <h2>My JWT Tokens</h2>

    <div class="filters">
      <select v-model="filters.status" @change="fetchTokens">
        <option value="">All Statuses</option>
        <option value="active">Active</option>
        <option value="expired">Expired</option>
        <option value="revoked">Revoked</option>
      </select>

      <input
        type="text"
        v-model="filters.jwt_name"
        placeholder="JWT Name"
        @input="debounceSearch"
      />
    </div>

    <div v-if="loading">Loading...</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <table v-else class="token-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Status</th>
          <th>Issued</th>
          <th>Expires</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="token in tokens" :key="token.jti">
          <td>{{ token.jwt_name }}</td>
          <td>
            <span :class="`status-${token.status}`">
              {{ token.status }}
            </span>
          </td>
          <td>{{ formatDate(token.issued_at) }}</td>
          <td>{{ formatDate(token.expires_at) }}</td>
          <td>
            <button
              v-if="token.status === 'active'"
              @click="revokeToken(token.jti)"
            >
              Revoke
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pagination" v-if="pagination">
      <button
        :disabled="pagination.offset === 0"
        @click="previousPage"
      >
        Previous
      </button>

      <span>
        {{ pagination.offset + 1 }} -
        {{ Math.min(pagination.offset + pagination.limit, pagination.total) }}
        of {{ pagination.total }}
      </span>

      <button
        :disabled="!pagination.has_more"
        @click="nextPage"
      >
        Next
      </button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      tokens: [],
      pagination: null,
      loading: false,
      error: null,
      filters: {
        status: '',
        jwt_name: '',
        limit: 20,
        offset: 0
      }
    };
  },

  mounted() {
    this.fetchTokens();
  },

  methods: {
    async fetchTokens() {
      this.loading = true;
      this.error = null;

      try {
        const response = await fetch('/jwt/custom/list/me', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(this.filters)
        });

        if (response.ok) {
          const data = await response.json();
          this.tokens = data.tokens;
          this.pagination = data.pagination;
        } else {
          const errorData = await response.json();
          this.error = errorData.message;
        }
      } catch (err) {
        this.error = 'Failed to fetch tokens';
      } finally {
        this.loading = false;
      }
    },

    debounceSearch: debounce(function() {
      this.filters.offset = 0;
      this.fetchTokens();
    }, 500),

    nextPage() {
      this.filters.offset += this.filters.limit;
      this.fetchTokens();
    },

    previousPage() {
      this.filters.offset = Math.max(0, this.filters.offset - this.filters.limit);
      this.fetchTokens();
    },

    async revokeToken(jti) {
      // Implementation for token revocation
      // This would call the revoke endpoint
    },

    formatDate(dateString) {
      return new Date(dateString).toLocaleString();
    }
  }
};
</script>
```

## Testing

### Manual Testing with curl

**Important**: First generate a token, then use it in all subsequent requests.

```bash
# Step 1: Generate a test token
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "jwt_name": "TEST_SESSION",
    "content": {"sub": "testuser", "role": "user"},
    "expiration_in_minutes": 60
  }'

# Copy the returned token, then use it in the Authorization header for all tests below:
export TEST_TOKEN="<paste-token-here>"

# Test 1: List all tokens (default)
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -d '{}'

# Test 2: Filter by status
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -d '{"status": "active"}'

# Test 3: Date range filtering
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -d '{
    "issued_after": "2024-01-01T00:00:00Z",
    "issued_before": "2024-01-31T23:59:59Z"
  }'

# Test 4: Pagination
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -d '{"limit": 10, "offset": 0}'

# Test 5: Combined filters
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TEST_TOKEN" \
  -d '{
    "status": "active",
    "jwt_name": "API_TOKEN",
    "limit": 25
  }'
```

### Automated Testing

```javascript
describe('JWT List Endpoint - Phase 1', () => {
  beforeEach(async () => {
    // Generate test tokens for the user
    await generateTestTokens('test-user', 5);
  });

  test('should list all user tokens by default', async () => {
    const response = await fetch('/jwt/custom/list/me', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
    });

    expect(response.status).toBe(200);
    const data = await response.json();

    expect(data.tokens).toBeInstanceOf(Array);
    expect(data.pagination).toBeDefined();
    expect(data.pagination.total).toBeGreaterThan(0);
  });

  test('should filter tokens by status', async () => {
    const response = await fetch('/jwt/custom/list/me', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'active' })
    });

    const data = await response.json();
    data.tokens.forEach(token => {
      expect(token.status).toBe('active');
    });
  });

  test('should respect pagination limits', async () => {
    const response = await fetch('/jwt/custom/list/me', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ limit: 3 })
    });

    const data = await response.json();
    expect(data.tokens.length).toBeLessThanOrEqual(3);
    expect(data.pagination.limit).toBe(3);
  });

  test('should handle date range filtering', async () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);

    const response = await fetch('/jwt/custom/list/me', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        issued_after: yesterday.toISOString()
      })
    });

    const data = await response.json();
    data.tokens.forEach(token => {
      expect(new Date(token.issued_at)).toBeAfter(yesterday);
    });
  });

  test('should enforce maximum limit', async () => {
    const response = await fetch('/jwt/custom/list/me', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ limit: 200 })
    });

    const data = await response.json();
    expect(data.pagination.limit).toBeLessThanOrEqual(100);
  });
});
```

## Security Considerations

### Access Control
- **User Isolation**: Users can only see tokens where `subject` matches their authenticated identity
- **No Token Values**: Actual JWT token values are never returned (only metadata)
- **Rate Limiting**: Consider implementing rate limiting for this endpoint

### Data Privacy
- **Minimal Exposure**: Only necessary metadata is exposed
- **Audit Logging**: All listing requests should be logged for security monitoring
- **Session Context**: Extract user identity from authenticated session (not from request body)

### Performance Considerations
- **Database Indexes**: Ensure proper indexing on `subject`, `issued_at`, `expires_at`
- **Query Optimization**: Use efficient pagination with database-level LIMIT/OFFSET
- **Caching**: Consider caching frequently accessed token lists

## Error Handling

### Common Error Scenarios

```json
// Invalid date format
{
  "error": "listing_failed",
  "message": "Failed to list tokens: Invalid datetime format"
}

// Invalid pagination parameters
{
  "error": "listing_failed",
  "message": "Failed to list tokens: Limit must be between 1 and 100"
}

// Authentication required
{
  "error": "authentication_required",
  "message": "User authentication required for token listing"
}
```

## Database Schema Reference

### Tables Used

**`custom.jwt_metadata`**:
- Primary source for token information
- Indexed on `subject` for efficient user filtering
- Contains all token metadata including issuer, audience, claims

**`custom.denylist`**:
- Checked to determine revocation status
- Contains revocation timestamp and reason
- Joined with metadata for complete token status

### Key Queries

```sql
-- Main listing query with filters
SELECT m.*, d.denylisted_at, d.reason
FROM custom.jwt_metadata m
LEFT JOIN custom.denylist d ON m.jwt_uuid = d.jwt_uuid
WHERE m.subject = ?
  AND (? IS NULL OR m.issued_at >= ?)
  AND (? IS NULL OR m.issued_at <= ?)
  AND (? IS NULL OR m.expires_at >= ?)
  AND (? IS NULL OR m.expires_at <= ?)
  AND (? IS NULL OR m.jwt_name = ?)
ORDER BY m.issued_at DESC
LIMIT ? OFFSET ?
```

## Integration with Existing Endpoints

This endpoint complements the existing JWT management endpoints:

- **Generation** (`/jwt/custom/generate`) → Creates tokens that appear in listings
- **Validation** (`/jwt/custom/validate`) → Validates tokens shown in listings
- **Revocation** (`/jwt/custom/revoke`) → Changes token status in listings
- **Extension** (`/jwt/custom/extend`) → Creates new tokens that replace old ones

## Future Enhancements (Phase 2+)

- **Admin Access**: Cross-user token listing for administrators
- **Advanced Filtering**: Complex query capabilities
- **Export Features**: CSV/JSON export of token lists
- **Real-time Updates**: WebSocket notifications for token changes

---

**Implementation Status**: ✅ **Complete and Ready for Production**

This Phase 1 implementation provides a solid foundation for user token management with comprehensive filtering, pagination, and status tracking capabilities.