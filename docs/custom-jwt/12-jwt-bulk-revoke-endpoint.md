# TIM JWT Bulk Revoke Endpoint

## Overview

The JWT Bulk Revoke endpoint (`/jwt/custom/revoke/bulk`) allows administrators and applications to revoke multiple JWT tokens in a single API call. This is essential for security incident response, administrative actions, and emergency situations where multiple tokens need to be invalidated quickly and efficiently.

The bulk revoke operation processes each token individually, providing detailed feedback on successful and failed revocations while maintaining atomic operation integrity.

## Endpoint Reference

### POST `/jwt/custom/revoke/bulk`

Revokes multiple JWT tokens in a single request with comprehensive error handling and audit logging.

#### Request Format

```http
POST /jwt/custom/revoke/bulk
Content-Type: application/json

{
  "tokens": [
    "eyJraWQiOiJqd3RzaWduIi...",
    "eyJraWQiOiJqd3RzaWduIi...",
    "eyJraWQiOiJqd3RzaWduIi..."
  ],
  "reason": "Security incident - compromised user accounts"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `tokens` | array | **Yes** | Array of JWT token strings to revoke (max 100) |
| `reason` | string | No | Optional reason for audit logging and compliance |

#### Request Validation

- **Minimum tokens**: 1 (empty arrays are rejected)
- **Maximum tokens**: 100 (prevents abuse and timeout issues)
- **Token format**: Valid JWT structure (header.payload.signature)

#### Response Format

**All Newly Revoked (200 OK):**
```json
{
  "status": "completed",
  "total": 3,
  "newly_revoked": 3,
  "already_revoked": 0,
  "failed": 0,
  "message": "Bulk revocation completed: 3 newly revoked, 0 already revoked, 0 failed",
  "newly_revoked_tokens": [
    "eyJraWQiOiJqd3RzaWdu...",
    "eyJraWQiOiJqd3RzaWdu...",
    "eyJraWQiOiJqd3RzaWdu..."
  ],
  "already_revoked_tokens": [],
  "failed_tokens": [],
  "reason": "Security incident - compromised user accounts"
}
```

**All Already Revoked (409 Conflict):**
```json
{
  "status": "completed",
  "total": 3,
  "newly_revoked": 0,
  "already_revoked": 3,
  "failed": 0,
  "message": "Bulk revocation completed: 0 newly revoked, 3 already revoked, 0 failed",
  "newly_revoked_tokens": [],
  "already_revoked_tokens": [
    "eyJraWQiOiJqd3RzaWdu...",
    "eyJraWQiOiJqd3RzaWdu...",
    "eyJraWQiOiJqd3RzaWdu..."
  ],
  "failed_tokens": [],
  "reason": "Security incident - compromised user accounts"
}
```

**Mixed Results (207 Multi-Status):**
```json
{
  "status": "completed",
  "total": 4,
  "newly_revoked": 2,
  "already_revoked": 1,
  "failed": 1,
  "message": "Bulk revocation completed: 2 newly revoked, 1 already revoked, 1 failed",
  "newly_revoked_tokens": [
    "eyJraWQiOiJqd3RzaWdu...",
    "eyJraWQiOiJqd3RzaWdu..."
  ],
  "already_revoked_tokens": [
    "eyJraWQiOiJqd3RzaWdu..."
  ],
  "failed_tokens": [
    {
      "token": "invalid.token.here...",
      "reason": "Invalid token format"
    }
  ],
  "reason": "Security incident - compromised user accounts"
}
```

**Error Response (400 Bad Request - All Failed):**
```json
{
  "status": "completed",
  "total": 2,
  "successful": 0,
  "failed": 2,
  "message": "Bulk revocation completed: 0 successful, 2 failed",
  "successful_tokens": [],
  "failed_tokens": [
    {
      "token": "invalid.token.1...",
      "reason": "Invalid token format"
    },
    {
      "token": "expired.token.2...",
      "reason": "Token expired"
    }
  ]
}
```

## HTTP Status Codes

| Status Code | Scenario | Description |
|-------------|----------|-------------|
| **200 OK** | All Newly Revoked | All tokens were successfully revoked (newly revoked) |
| **409 Conflict** | All Already Revoked | All tokens were already revoked (idempotent) |
| **207 Multi-Status** | Mixed Results | Combination of newly revoked, already revoked, and/or failed tokens |
| **400 Bad Request** | Request Error | Invalid request format or all tokens failed processing |

### Response Logic

The endpoint uses intelligent status code selection based on revocation results:

```javascript
if (failed == 0) {
    if (alreadyRevoked == 0) {
        return 200; // All newly revoked
    } else if (newlyRevoked == 0) {
        return 409; // All already revoked
    } else {
        return 207; // Mixed newly/already revoked
    }
} else {
    if (newlyRevoked > 0 || alreadyRevoked > 0) {
        return 207; // Partial success with failures
    } else {
        return 400; // All failed
} else if (successful > 0) {
    return 207; // Partial success
} else {
    return 400; // All failed
}
```

## Use Cases and Examples

### 1. Security Incident Response

**Scenario**: User account compromised, revoke all active sessions

```bash
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": [
      "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIiwiaXNzIjoiVElNIiwiZXhwIjoxNzU5MTE4MjYxLCJpYXQiOjE3NTkxMTQ2NjEsImp0aSI6ImFkMDk0ODAzLWFhZWItNDg0ZS04NjZkLTEzMjdmMzg4OTdjNiJ9.lZurcA4n23TVhFAS4YX0oqACZ3azzcyWwNkIczmpighn0weFrj1CawNd-x0SGD7tBOCS8zFxXQR84IKQZnk3tjvB1TAQJa_xFic9p3fmfepxnxwNYQ04tPCaGGADDaSyBAcFTPqHRJQVvveauZ3xudy08jlg3tZnDXdIoDanVypIAyJBWrDsk-VNkNoyvRSe3RvAWTwSnswHPd1xI1ASXT7A-LQy-aEpqUZlyAOY5rLlBSEMuYHEMpwa_WbPApvBPkcead5M0JNCE3hqZHs-zoGk3NFMf6Nv7oqdEG73UOcWbOW1JJe6Mvitj4jp4wQ2Mugqt5VQznnu_xNiFQwRaw",
      "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6InVzZXIiLCJpc3MiOiJUSU0iLCJleHAiOjE3NTkxMTgyNjcsImlhdCI6MTc1OTExNDY2NywianRpIjoiYmRlMTFjMmMtMzRkZS00MWNlLTk1MzUtN2E1ZmVlNjczZmUyIn0.DweOVKLCNCxO7O8603iBm44rWZP4NAQt0yFYf68Fb2Q9NpF4E8L3plfzO7lJlVgIx5FnwFr4cjYPKLcpLji-x4NYMpCl9wLhAjomxP1ix7qmnRfuNAgTVc03LSPY8TDzd29VRwMB4IGY4qcZcV7mAMrktS9vT7vKg5URF0_O9DSKNjOY1OiDeDhLOCt0odcoi6gMC5r4lueyC12mHJ9XYjpKDfOBdNb3iFW7O0PDNY5kZaIj9V3EfbPVcjUhRSOOAA46PmuWrWzFPFryLBNNLKD7STED_86bWSIi43QD8mY-SwUkbSnTCmYjKg3YqyRzvrVd4yJ-2fwou82PySJKBw"
    ],
    "reason": "Security incident - account compromise detected"
  }'
```

**Response:**
```json
{
  "status": "completed",
  "total": 2,
  "successful": 2,
  "failed": 0,
  "message": "Bulk revocation completed: 2 successful, 0 failed",
  "successful_tokens": [
    "eyJraWQiOiJqd3RzaWdu...",
    "eyJraWQiOiJqd3RzaWdu..."
  ],
  "failed_tokens": [],
  "reason": "Security incident - account compromise detected"
}
```

### 2. Administrative User Deactivation

**Scenario**: Employee termination, revoke all access tokens

```bash
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["token1", "token2", "token3"],
    "reason": "Employee termination - user deactivated"
  }'
```

### 3. Emergency Response

**Scenario**: Security breach, revoke tokens for multiple users

```bash
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["...array of 50 tokens..."],
    "reason": "Emergency response - data breach mitigation"
  }'
```

## Error Scenarios

### Request Validation Errors

**Empty Token List:**
```bash
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{"tokens": []}'
```

**Response (400 Bad Request):**
```json
{
  "error": "invalid_request",
  "message": "Tokens list is required and cannot be empty"
}
```

**Too Many Tokens:**
```bash
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["...array of 150 tokens..."]
  }'
```

**Response (400 Bad Request):**
```json
{
  "error": "request_too_large",
  "message": "Cannot revoke more than 100 tokens at once",
  "provided": 150,
  "maximum": 100
}
```

### Token Processing Errors

**Invalid Token Format:**
```bash
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["invalid.token.here", "another.bad.token"],
    "reason": "Testing error handling"
  }'
```

**Response (400 Bad Request):**
```json
{
  "status": "completed",
  "total": 2,
  "successful": 0,
  "failed": 2,
  "message": "Bulk revocation completed: 0 successful, 2 failed",
  "successful_tokens": [],
  "failed_tokens": [
    {
      "token": "invalid.token.here...",
      "reason": "Invalid token format"
    },
    {
      "token": "another.bad.token...",
      "reason": "Invalid token format"
    }
  ],
  "reason": "Testing error handling"
}
```

## Security Features

### Rate Limiting and Abuse Prevention

- **Maximum tokens per request**: 100
- **Token validation**: Each token must be valid JWT format
- **Atomic operations**: Each token processed independently
- **Request size limits**: Prevents oversized payloads

### Privacy and Security

- **Token truncation**: Response only shows first 20 characters of tokens
- **Reason logging**: Optional audit trail for compliance
- **Immediate effectiveness**: Revoked tokens become invalid instantly
- **Database integrity**: All revocations stored with timestamp

### Audit and Compliance

```sql
-- Audit query: Find all tokens revoked in bulk
SELECT
  d.jwt_uuid,
  d.denylisted_at,
  m.claim_keys
FROM custom.denylist d
JOIN custom.jwt_metadata m ON d.jwt_uuid = m.jwt_uuid
WHERE d.denylisted_at BETWEEN '2025-01-01' AND '2025-01-02'
ORDER BY d.denylisted_at;
```

## Performance Considerations

### Optimization Features

- **Batch processing**: Tokens processed in single transaction
- **Error isolation**: One failed token doesn't affect others
- **Memory efficiency**: Tokens processed sequentially, not loaded all at once
- **Database optimization**: Bulk insert operations where possible

### Recommended Practices

1. **Batch size**: Stay under 100 tokens per request
2. **Error handling**: Always check response for partial failures
3. **Retry logic**: Implement exponential backoff for failed requests
4. **Monitoring**: Track bulk revocation patterns for security analysis

## Integration Examples

### Security Management System

```javascript
class SecurityManager {
  async revokeUserTokens(userTokens, reason) {
    const batchSize = 50; // Stay well under 100 limit
    const batches = this.chunkArray(userTokens, batchSize);
    const results = [];

    for (const batch of batches) {
      try {
        const response = await fetch('/jwt/custom/revoke/bulk', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            tokens: batch,
            reason: reason
          })
        });

        const result = await response.json();
        results.push(result);

        // Log partial failures
        if (result.failed > 0) {
          console.warn(`Bulk revoke partial failure: ${result.failed} tokens failed`);
          this.logFailedTokens(result.failed_tokens);
        }
      } catch (error) {
        console.error('Bulk revocation error:', error);
        throw error;
      }
    }

    return results;
  }

  chunkArray(array, size) {
    const chunks = [];
    for (let i = 0; i < array.length; i += size) {
      chunks.push(array.slice(i, i + size));
    }
    return chunks;
  }

  logFailedTokens(failedTokens) {
    failedTokens.forEach(failure => {
      console.error(`Token ${failure.token} failed: ${failure.reason}`);
    });
  }
}
```

### Incident Response Automation

```python
import requests
import json

class IncidentResponse:
    def __init__(self, tim_base_url):
        self.base_url = tim_base_url

    def emergency_revoke_all(self, token_list, incident_id):
        """Emergency bulk revocation for security incidents"""

        # Split into safe batch sizes
        batch_size = 75
        batches = [token_list[i:i+batch_size] for i in range(0, len(token_list), batch_size)]

        results = {
            'total_tokens': len(token_list),
            'total_successful': 0,
            'total_failed': 0,
            'batch_results': []
        }

        for i, batch in enumerate(batches):
            payload = {
                'tokens': batch,
                'reason': f'Emergency incident response - ID: {incident_id}'
            }

            response = requests.post(
                f'{self.base_url}/jwt/custom/revoke/bulk',
                headers={'Content-Type': 'application/json'},
                json=payload
            )

            batch_result = response.json()
            results['batch_results'].append(batch_result)
            results['total_successful'] += batch_result.get('successful', 0)
            results['total_failed'] += batch_result.get('failed', 0)

            print(f"Batch {i+1}/{len(batches)}: {batch_result['successful']} successful, {batch_result['failed']} failed")

        return results

# Usage
incident_response = IncidentResponse('http://localhost:8085')
compromised_tokens = ['token1', 'token2', 'token3']
results = incident_response.emergency_revoke_all(compromised_tokens, 'INC-2025-001')
```

## Monitoring and Observability

### Key Metrics

- **Bulk revocation requests per hour**
- **Average tokens per bulk request**
- **Success/failure ratios**
- **Response time distribution**
- **Error pattern analysis**

### Alerting Thresholds

- **High frequency**: > 10 bulk revocations per hour
- **Large batches**: > 50 tokens per request
- **High failure rate**: > 20% failed tokens
- **Emergency pattern**: Multiple rapid bulk revocations

### Logging Examples

**Successful Bulk Revocation:**
```
INFO [2025-09-29T02:57:00Z] Bulk revocation completed - total=2 successful=2 failed=0 reason="Security incident - bulk revocation test" duration=45ms
```

**Partial Failure:**
```
WARN [2025-09-29T02:57:00Z] Bulk revocation partial failure - total=3 successful=2 failed=1 reason="Mixed token validation" duration=67ms
```

**Rate Limit Hit:**
```
WARN [2025-09-29T02:57:00Z] Bulk revocation rejected - reason="request_too_large" provided=150 maximum=100
```

## Database Impact

### Denylist Growth

Bulk revocations will increase denylist size proportionally:

```sql
-- Monitor denylist growth from bulk operations
SELECT
  DATE(denylisted_at) as date,
  COUNT(*) as tokens_revoked,
  COUNT(*) / 24.0 as avg_per_hour
FROM custom.denylist
WHERE denylisted_at >= NOW() - INTERVAL '7 days'
GROUP BY DATE(denylisted_at)
ORDER BY date DESC;
```

### Cleanup Considerations

```sql
-- Cleanup expired denylist entries (maintenance script)
DELETE FROM custom.denylist
WHERE expires_at < NOW() - INTERVAL '30 days';

-- Optimize after bulk operations
VACUUM ANALYZE custom.denylist;
```

## Testing

### Manual Testing Script

```bash
#!/bin/bash

# Test 1: Valid bulk revocation
echo "Testing valid bulk revocation..."
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["valid_token_1", "valid_token_2"],
    "reason": "Test case 1"
  }'

# Test 2: Empty tokens array
echo "Testing empty tokens array..."
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{"tokens": []}'

# Test 3: Too many tokens
echo "Testing rate limit..."
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d "{\"tokens\": $(python3 -c "print(['token'] * 101)")}"

# Test 4: Invalid tokens
echo "Testing invalid tokens..."
curl -X POST http://localhost:8085/jwt/custom/revoke/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": ["invalid.token.1", "invalid.token.2"],
    "reason": "Error handling test"
  }'
```

### Automated Test Suite

```javascript
describe('Bulk Revoke Endpoint', () => {
  test('Successfully revokes multiple valid tokens', async () => {
    const tokens = await generateTestTokens(3);

    const response = await bulkRevoke({
      tokens: tokens,
      reason: 'Test revocation'
    });

    expect(response.status).toBe(200);
    expect(response.data.successful).toBe(3);
    expect(response.data.failed).toBe(0);
  });

  test('Handles partial failures gracefully', async () => {
    const validTokens = await generateTestTokens(2);
    const invalidTokens = ['invalid.token.1'];

    const response = await bulkRevoke({
      tokens: [...validTokens, ...invalidTokens]
    });

    expect(response.status).toBe(207); // Multi-Status
    expect(response.data.successful).toBe(2);
    expect(response.data.failed).toBe(1);
  });

  test('Rejects requests with too many tokens', async () => {
    const tokens = new Array(101).fill('dummy.token');

    const response = await bulkRevoke({ tokens });

    expect(response.status).toBe(400);
    expect(response.data.error).toBe('request_too_large');
  });

  test('Rejects empty token arrays', async () => {
    const response = await bulkRevoke({ tokens: [] });

    expect(response.status).toBe(400);
    expect(response.data.error).toBe('invalid_request');
  });
});
```

## Comparison with Industry Standards

### OAuth 2.0 Token Revocation (RFC 7009)

**TIM vs OAuth 2.0:**
- ✅ Similar bulk operation patterns
- ✅ Proper HTTP status code usage
- ✅ Detailed error reporting
- ⚠️ Extended with reason field for audit compliance

### Enterprise Security Standards

**Security Alignment:**
- ✅ Rate limiting for abuse prevention
- ✅ Atomic operation guarantees
- ✅ Comprehensive audit logging
- ✅ Privacy-conscious response formatting

---

## Related Documentation

- **[JWT Generation Endpoint](./06-jwt-generation-endpoint.md)**: Creating JWT tokens
- **[JWT Validation Endpoint](./07-jwt-validation-endpoint.md)**: Validating JWT tokens
- **[JWT Revocation Endpoint](./10-jwt-revocation-endpoint.md)**: Single token revocation
- **[JWT Extend Endpoint](./11-jwt-extend-endpoint.md)**: Token lifetime extension
- **[HTTP Response Codes](./09-http-response-codes.md)**: Status codes and error handling