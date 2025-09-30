#!/bin/bash
# Integration Test: Complete JWT Lifecycle
# Tests: Generate → Introspect → Extend → Introspect → Revoke → Introspect

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/test-utils.sh"

test_setup "full-lifecycle"

# Plan TAP output
tap_plan 15

# Step 1: Generate a JWT
log_info "Step 1: Generate JWT"
request_data='{
  "JWTName": "lifecycle-test-token",
  "content": {
    "sub": "lifecycle-user",
    "role": "integration-tester",
    "department": "qa"
  },
  "expirationInMinutes": 30
}'

if http_request "POST" "$TIM_BASE_URL/jwt/custom/generate" "$request_data"; then
    assert_http_status "200" "JWT generation successful"

    TOKEN=$(json_extract "$HTTP_BODY" ".token")
    assert_not_empty "$TOKEN" "Generated token is not empty"

    # Extract JTI for later operations
    JTI=$(jwt_extract_claim "$TOKEN" "jti")
    assert_not_empty "$JTI" "JWT contains JTI claim"

    log_info "Generated JWT with JTI: $JTI"
else
    log_error "Failed to generate JWT"
    exit 1
fi

# Step 2: Introspect the fresh token
log_info "Step 2: Introspect fresh token"
if http_request "POST" "$TIM_BASE_URL/introspect" "token=$TOKEN" "application/x-www-form-urlencoded"; then
    assert_http_status "200" "Fresh token introspection successful"

    ACTIVE_STATUS=$(json_extract "$HTTP_BODY" ".active")
    assert_equals "true" "$ACTIVE_STATUS" "Fresh token is active"

    INTROSPECT_SUB=$(json_extract "$HTTP_BODY" ".sub")
    assert_equals "lifecycle-user" "$INTROSPECT_SUB" "Introspection shows correct subject"

    INTROSPECT_ROLE=$(json_extract "$HTTP_BODY" ".role")
    assert_equals "integration-tester" "$INTROSPECT_ROLE" "Introspection shows correct role"
else
    log_error "Failed to introspect fresh token"
    exit 1
fi

# Step 3: Extend the token
log_info "Step 3: Extend token lifetime"
extend_data="{\"jti\": \"$JTI\", \"additionalMinutes\": 60}"

if http_request "POST" "$TIM_BASE_URL/jwt/custom/extend" "$extend_data"; then
    assert_http_status "200" "Token extension successful"

    EXTENDED_TOKEN=$(json_extract "$HTTP_BODY" ".token")
    assert_not_empty "$EXTENDED_TOKEN" "Extended token is not empty"

    # Verify extended token has same JTI but different expiration
    EXTENDED_JTI=$(jwt_extract_claim "$EXTENDED_TOKEN" "jti")
    assert_equals "$JTI" "$EXTENDED_JTI" "Extended token has same JTI"

    TOKEN="$EXTENDED_TOKEN"  # Use extended token for remaining tests
else
    log_error "Failed to extend token"
    # Continue with original token
fi

# Step 4: Introspect the extended token
log_info "Step 4: Introspect extended token"
if http_request "POST" "$TIM_BASE_URL/introspect" "token=$TOKEN" "application/x-www-form-urlencoded"; then
    assert_http_status "200" "Extended token introspection successful"

    ACTIVE_STATUS=$(json_extract "$HTTP_BODY" ".active")
    assert_equals "true" "$ACTIVE_STATUS" "Extended token is still active"
else
    log_error "Failed to introspect extended token"
fi

# Step 5: Revoke the token
log_info "Step 5: Revoke token"
revoke_data="{\"jti\": \"$JTI\", \"reason\": \"Integration test cleanup\"}"

if http_request "POST" "$TIM_BASE_URL/jwt/custom/revoke" "$revoke_data"; then
    assert_http_status "200" "Token revocation successful"

    REVOKE_STATUS=$(json_extract "$HTTP_BODY" ".revoked")
    assert_equals "true" "$REVOKE_STATUS" "Revocation confirmed"
else
    log_error "Failed to revoke token"
fi

# Step 6: Introspect the revoked token
log_info "Step 6: Introspect revoked token"
if http_request "POST" "$TIM_BASE_URL/introspect" "token=$TOKEN" "application/x-www-form-urlencoded"; then
    assert_http_status "200" "Revoked token introspection successful"

    ACTIVE_STATUS=$(json_extract "$HTTP_BODY" ".active")
    assert_equals "false" "$ACTIVE_STATUS" "Revoked token is inactive"
else
    log_error "Failed to introspect revoked token"
fi

# Step 7: List tokens to verify state
log_info "Step 7: List user tokens"
list_data="{\"subject\": \"lifecycle-user\"}"

if http_request "POST" "$TIM_BASE_URL/jwt/custom/list" "$list_data"; then
    assert_http_status "200" "Token list retrieval successful"

    # Verify the token appears in the list but might be marked as revoked
    TOKENS_ARRAY=$(json_extract "$HTTP_BODY" ".tokens")
    assert_not_empty "$TOKENS_ARRAY" "User has tokens in the list"
else
    log_error "Failed to list user tokens"
fi

log_info "Integration test completed: Generate → Introspect → Extend → Introspect → Revoke → Introspect → List"

test_teardown "full-lifecycle"