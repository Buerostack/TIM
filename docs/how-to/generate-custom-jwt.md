# How to Generate Custom JWT Tokens

This guide shows you how to generate custom JWT tokens using TIM 2.0's JWT management API.

## Overview

TIM 2.0 allows you to generate custom JWT tokens with:
- Custom claims (key-value pairs)
- Configurable expiration times
- RSA256 signatures
- Token metadata and lifecycle management

## Prerequisites

- TIM 2.0 running (see [setup-development-environment.md](setup-development-environment.md))
- API accessible at `http://localhost:8085`

## Basic Token Generation

### Using cURL

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "my-first-token",
    "content": {
      "sub": "user123",
      "role": "user"
    },
    "expirationInMinutes": 60
  }'
```

### Expected Response

```json
{
  "token": "eyJraWQiOiJqd3RzaWduIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJ0aW0tYXVkaWVuY2UiLCJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6InVzZXIiLCJpc3MiOiJUSU0iLCJleHAiOjE3MDkxMjM0NTYsImlhdCI6MTcwOTExOTg1Nn0...",
  "expiresAt": "2024-02-28T15:30:56Z",
  "tokenId": "d4c5b6a7-8e9f-4a5b-9c8d-7e6f5a4b3c2d"
}
```

## Request Parameters

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `JWTName` | string | Human-readable token name/label |
| `content` | object | Custom claims to include in the token |
| `expirationInMinutes` | integer | Token validity duration in minutes |

### Optional Fields

| Field | Type | Description | Default |
|-------|------|-------------|---------|
| `setCookie` | boolean | Set token as HttpOnly cookie | `false` |

## Custom Claims

The `content` object can contain any JSON-serializable data:

```json
{
  "JWTName": "user-session",
  "content": {
    "sub": "user123",
    "email": "user@example.com",
    "role": "admin",
    "permissions": ["read", "write", "delete"],
    "organizationId": "org-456",
    "metadata": {
      "loginMethod": "password",
      "ipAddress": "192.168.1.100"
    }
  },
  "expirationInMinutes": 120
}
```

## Common Use Cases

### 1. User Authentication Token

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "user-auth-token",
    "content": {
      "sub": "user@example.com",
      "name": "John Doe",
      "role": "user"
    },
    "expirationInMinutes": 60
  }'
```

### 2. API Service Token

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "api-service-token",
    "content": {
      "sub": "service-api",
      "scope": "read:users write:users",
      "client_id": "service-123"
    },
    "expirationInMinutes": 1440
  }'
```

### 3. Temporary Access Token

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "temp-access",
    "content": {
      "sub": "guest-user",
      "access_level": "limited",
      "resource_id": "doc-789"
    },
    "expirationInMinutes": 15
  }'
```

## Using the Generated Token

### 1. Extract the Token

Save the token from the response:

```bash
TOKEN=$(curl -s -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "test-token",
    "content": {"sub": "testuser"},
    "expirationInMinutes": 60
  }' | jq -r '.token')

echo "Token: $TOKEN"
```

### 2. Use Token in Requests

Include the token in the `Authorization` header:

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

## Generating Token with Cookie

To set the token as an HttpOnly cookie:

```bash
curl -X POST http://localhost:8085/jwt/custom/generate \
  -H "Content-Type: application/json" \
  -d '{
    "JWTName": "session-token",
    "content": {
      "sub": "user123",
      "role": "user"
    },
    "expirationInMinutes": 60,
    "setCookie": true
  }' \
  -c cookies.txt \
  -v
```

The response will include a `Set-Cookie` header, and subsequent requests can use the cookie:

```bash
curl -X POST http://localhost:8085/jwt/custom/list/me \
  -b cookies.txt \
  -H "Content-Type: application/json"
```

## Programming Language Examples

### JavaScript (Node.js)

```javascript
const axios = require('axios');

async function generateToken() {
  try {
    const response = await axios.post('http://localhost:8085/jwt/custom/generate', {
      JWTName: 'my-token',
      content: {
        sub: 'user123',
        role: 'admin'
      },
      expirationInMinutes: 60
    });

    const { token, expiresAt, tokenId } = response.data;
    console.log('Token:', token);
    console.log('Expires:', expiresAt);
    console.log('Token ID:', tokenId);

    return token;
  } catch (error) {
    console.error('Error generating token:', error.response?.data);
  }
}

generateToken();
```

### Python

```python
import requests
import json

def generate_token():
    url = 'http://localhost:8085/jwt/custom/generate'
    payload = {
        'JWTName': 'my-token',
        'content': {
            'sub': 'user123',
            'role': 'admin'
        },
        'expirationInMinutes': 60
    }

    response = requests.post(url, json=payload)

    if response.status_code == 200:
        data = response.json()
        print(f"Token: {data['token']}")
        print(f"Expires: {data['expiresAt']}")
        print(f"Token ID: {data['tokenId']}")
        return data['token']
    else:
        print(f"Error: {response.text}")
        return None

token = generate_token()
```

### Java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.net.URI;
import java.util.Map;

public class TokenGenerator {
    public static void main(String[] args) throws Exception {
        String url = "http://localhost:8085/jwt/custom/generate";

        Map<String, Object> requestBody = Map.of(
            "JWTName", "my-token",
            "content", Map.of(
                "sub", "user123",
                "role", "admin"
            ),
            "expirationInMinutes", 60
        );

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        System.out.println("Response: " + response.body());
    }
}
```

## Token Expiration Guidelines

| Use Case | Recommended Expiration |
|----------|----------------------|
| Web session | 60-120 minutes |
| API token | 24 hours - 7 days |
| Service-to-service | 7-30 days |
| Temporary access | 5-15 minutes |
| Long-lived API key | Use token extension instead |

## Security Best Practices

1. **Short Expiration Times**: Use the shortest practical expiration time
2. **Specific Claims**: Include only necessary claims in the token
3. **Secure Storage**: Never store tokens in plain text or local storage
4. **HTTPS Only**: Always use HTTPS in production
5. **Token Rotation**: Implement token refresh/extension for long sessions
6. **Revocation**: Revoke tokens when sessions end or on security events

## Next Steps

- **List Your Tokens**: See [list-and-manage-tokens.md](list-and-manage-tokens.md)
- **Validate Tokens**: See [validate-tokens.md](validate-tokens.md)
- **Extend Token Expiration**: See token extension API
- **Revoke Tokens**: See token revocation API
