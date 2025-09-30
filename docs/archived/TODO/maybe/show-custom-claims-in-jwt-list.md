# Show Custom Claims in JWT List Response

## Overview
Currently, when listing JWTs via `/jwt/custom/list/me`, the response includes a `claims` field that is always `null`. Custom claims (like `"role": "client"`) that users include when creating JWTs are not displayed in the list response.

## Current Behavior
- JWT creation accepts custom claims in `content` field (e.g., `{"sub": "user", "role": "admin"}`)
- Custom claims are stored in `CustomJwtMetadata.claimKeys` field
- JWT list endpoint returns `"claims": null` for all tokens
- The `JwtTokenSummary.claims` field exists but is never populated

## Potential Implementation
Add optional custom claims display with these considerations:

### Request Parameter
- Add `includeClaims: boolean` parameter (default: false)
- Only show claims when explicitly requested

### Security & Privacy
- Filter out standard JWT claims (`sub`, `iss`, `aud`, `exp`, `iat`, `jti`) - already shown separately
- Consider truncating very large claim values
- Document that custom claims may be visible to token holders

### Code Changes Required
- Modify `CustomJwtService.convertToTokenSummary()` to populate claims field
- Update `JwtListRequest` to include `includeClaims` parameter
- Add filtering logic for standard claims

## Arguments For
- **Transparency**: Users can see what permissions/roles their tokens have
- **Debugging**: Helps troubleshoot authorization issues
- **User Experience**: Complete token information in one place
- **API Completeness**: The field already exists but is unused

## Arguments Against
- **Security**: Custom claims may contain sensitive information
- **Variable Structure**: Custom claims can be arbitrary JSON, making API response unpredictable
- **Privacy**: Some claims might be internal-only metadata
- **Response Size**: Large custom claims could bloat the response

## Decision
**Currently not implemented** to maintain security and keep API responses predictable. Custom claims remain stored but hidden from list responses.

## Related Files
- `CustomJwtService.java:convertToTokenSummary()` - where claims field would be populated
- `JwtTokenSummary.java` - contains unused `claims` field
- `CustomJwtMetadata.java` - stores `claimKeys` field with custom claim data