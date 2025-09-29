# User Story: User Profile Service

## User Story
**As a** client application
**I want** to retrieve standardized user profile information from authenticated tokens
**So that** I can display user information and make authorization decisions consistently across different OAuth2 providers

## Background
Different OAuth2/OIDC providers return user information in various formats and claim structures. TIM should normalize this into a consistent user profile format while preserving provider-specific attributes when needed.

## Acceptance Criteria

### AC1: User Profile Retrieval
**Given** a validated OAuth2/OIDC token
**When** making a GET request to `/oauth2/profile`
**Then** TIM should extract user information from the token or fetch from provider's userinfo endpoint
**And** return a standardized user profile format
**And** include provider-specific custom claims when requested

### AC2: Token-Based Profile Extraction
**Given** a JWT token with user claims
**When** extracting user profile from token
**Then** TIM should prioritize information in this order:
1. ID token claims (most reliable)
2. Access token claims (if available)
3. UserInfo endpoint data (if needed)
**And** handle missing or empty claims gracefully
**And** validate claim formats and types

### AC3: UserInfo Endpoint Integration
**Given** insufficient user data in tokens
**When** additional user information is needed
**Then** TIM should call the provider's UserInfo endpoint using the access token
**And** merge UserInfo response with token claims
**And** handle UserInfo endpoint failures gracefully
**And** respect rate limits and caching

### AC4: Provider-Specific Profile Mapping
**Given** different providers use different claim names and formats
**When** normalizing user profiles
**Then** TIM should map provider-specific claims to standard format:

#### TARA Estonia Mapping:
- `personalcode` → `nationalId`
- `given_name` → `firstName`
- `family_name` → `lastName`
- `amr` → `authenticationMethod`
- `acr` → `levelOfAssurance`

#### Google OAuth2 Mapping:
- `given_name` → `firstName`
- `family_name` → `lastName`
- `picture` → `avatarUrl`
- `email_verified` → `emailVerified`

#### Azure AD Mapping:
- `given_name` → `firstName`
- `family_name` → `lastName`
- `upn` → `userPrincipalName`
- `oid` → `objectId`

### AC5: Standardized Profile Format
**Given** user profile data from any provider
**When** returning profile information
**Then** TIM should use this standardized format:
```json
{
  "sub": "unique_user_identifier",
  "provider": "provider_id",
  "profile": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "emailVerified": true,
    "phoneNumber": "+372123456789",
    "phoneVerified": true,
    "avatarUrl": "https://example.com/avatar.jpg",
    "locale": "et",
    "nationalId": "38001085718", // TARA specific
    "authenticationMethod": "idcard", // TARA specific
    "levelOfAssurance": "high" // TARA specific
  },
  "custom_claims": {
    // Provider-specific claims not in standard profile
  },
  "token_info": {
    "issued_at": "2025-09-29T15:00:00Z",
    "expires_at": "2025-09-29T16:00:00Z",
    "scopes": ["openid", "profile", "email"]
  }
}
```

### AC6: Profile Caching
**Given** user profile requests for the same token
**When** retrieving profile information
**Then** TIM should cache profile data with TTL based on token expiration
**And** invalidate cache when token expires
**And** allow cache bypass with `force_refresh` parameter
**And** handle UserInfo endpoint rate limits through caching

### AC7: Privacy and Data Protection
**Given** sensitive user information
**When** handling profile data
**Then** TIM should:
- Only return explicitly consented user attributes
- Respect scope limitations (profile, email, phone scopes)
- Not log sensitive personal information
- Support data minimization principles
- Comply with GDPR requirements for Estonian users

## Technical Requirements

### Profile Request Format
```http
GET /oauth2/profile HTTP/1.1
Authorization: Bearer ACCESS_TOKEN_OR_ID_TOKEN

# Optional query parameters:
# ?include_custom_claims=true
# ?force_refresh=true
```

### Provider Configuration for Profile Mapping
```yaml
providers:
  tara:
    profile_mapping:
      firstName: "given_name"
      lastName: "family_name"
      nationalId: "personalcode"
      authenticationMethod: "amr"
      levelOfAssurance: "acr"
    custom_claims:
      - "personalcode"
      - "amr"
      - "acr"
```

### UserInfo Endpoint Integration
- Support OAuth2 Bearer token authentication
- Handle various UserInfo response formats (JSON, JWT)
- Implement proper error handling for UserInfo failures
- Support UserInfo endpoint discovery from OIDC configuration

## TARA-Specific Requirements

### Estonian Personal Code Handling
- Validate Estonian personal code format (11 digits)
- Extract birth date and gender from personal code
- Handle personal code privacy considerations

### Authentication Method Mapping
```
TARA amr values → TIM authenticationMethod:
- "idcard" → "estonian-id-card"
- "mID" → "mobile-id"
- "smartid" → "smart-id"
- "eIDAS" → "eu-eid"
```

### Level of Assurance Mapping
```
TARA acr values → TIM levelOfAssurance:
- "low" → "low"
- "substantial" → "substantial"
- "high" → "high"
```

## Security and Privacy Considerations
- Never log personal identification numbers or sensitive claims
- Implement proper access controls for profile data
- Support scope-based data filtering
- Audit profile access for compliance
- Handle cross-border data transfer requirements

## Error Handling
- Handle missing or malformed claims gracefully
- Provide meaningful error messages for profile failures
- Support partial profile data when some claims are unavailable
- Log errors without exposing sensitive information

## Definition of Done
- [ ] User profile extraction from tokens
- [ ] UserInfo endpoint integration
- [ ] Provider-specific claim mapping
- [ ] Standardized profile response format
- [ ] Profile data caching with proper TTL
- [ ] TARA-specific profile handling
- [ ] Privacy-compliant data handling
- [ ] Scope-based data filtering
- [ ] Unit tests for all profile scenarios
- [ ] Integration tests with real providers
- [ ] Privacy and security audit
- [ ] GDPR compliance verification