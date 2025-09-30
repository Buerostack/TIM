#!/bin/bash
# Test Token Introspection Endpoint

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/test-utils.sh"

test_setup "introspect"

# Plan TAP output
tap_plan 12

# First, generate a token to introspect
log_info "Generating test token for introspection..."
request_data='{
  "JWTName": "introspect-test-token",
  "content": {
    "sub": "introspect-user",
    "role": "tester"
  },
  "expirationInMinutes": 60
}'

if ! http_request "POST" "$TIM_BASE_URL/jwt/custom/generate" "$request_data"; then
    log_error "Failed to generate test token"
    exit 1
fi

TEST_TOKEN=$(json_extract "$HTTP_BODY" ".token")
if [[ -z "$TEST_TOKEN" || "$TEST_TOKEN" == "null" ]]; then
    log_error "No token received from generate endpoint"
    exit 1
fi

log_info "Generated test token for introspection tests"

# Test 1: Introspect valid token (form-encoded)
log_info "Test 1: Introspect valid token (RFC 7662 form-encoded)"
if http_request "POST" "$TIM_BASE_URL/introspect" "token=$TEST_TOKEN" "application/x-www-form-urlencoded"; then
    assert_http_status "200" "Introspect valid token returns 200"

    # Check active status
    ACTIVE_STATUS=$(json_extract "$HTTP_BODY" ".active")
    assert_equals "true" "$ACTIVE_STATUS" "Token is marked as active"

    # Check standard claims
    assert_json_key "sub" "Response contains subject"
    assert_json_key "iss" "Response contains issuer"
    assert_json_key "exp" "Response contains expiration"
    assert_json_key "iat" "Response contains issued at"
    assert_json_key "jti" "Response contains JWT ID"

    # Check custom claims
    TOKEN_ROLE=$(json_extract "$HTTP_BODY" ".role")
    assert_equals "tester" "$TOKEN_ROLE" "Response contains custom role claim"

    # Check token type
    TOKEN_TYPE=$(json_extract "$HTTP_BODY" ".token_type")
    assert_equals "custom_jwt" "$TOKEN_TYPE" "Response contains correct token type"
else
    # Mark all planned tests as failed
    for i in {1..8}; do
        tap_not_ok "$i" "Introspection test $i failed due to request failure"
    done
fi

# Test 2: Introspect valid token (JSON)
log_info "Test 2: Introspect valid token (JSON endpoint)"
json_request_data="{\"token\": \"$TEST_TOKEN\"}"

if http_request "POST" "$TIM_BASE_URL/introspect" "$json_request_data" "application/json"; then
    assert_http_status "200" "JSON introspect valid token returns 200"

    ACTIVE_STATUS=$(json_extract "$HTTP_BODY" ".active")
    assert_equals "true" "$ACTIVE_STATUS" "Token is active via JSON endpoint"
else
    tap_not_ok "9" "JSON introspect failed"
    tap_not_ok "10" "Token active check via JSON failed"
fi

# Test 3: Introspect invalid token
log_info "Test 3: Introspect invalid token"
invalid_token="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature"

if http_request "POST" "$TIM_BASE_URL/introspect" "token=$invalid_token" "application/x-www-form-urlencoded"; then
    assert_http_status "200" "Introspect invalid token returns 200"

    ACTIVE_STATUS=$(json_extract "$HTTP_BODY" ".active")
    assert_equals "false" "$ACTIVE_STATUS" "Invalid token is marked as inactive"
else
    tap_not_ok "11" "Invalid token introspection failed"
    tap_not_ok "12" "Invalid token should be inactive"
fi

# Test 4: Get supported token types
log_info "Test 4: Get supported token types"
if http_request "GET" "$TIM_BASE_URL/introspect/types"; then
    assert_http_status "200" "Get supported types returns 200"

    # Check if custom_jwt is supported
    CUSTOM_JWT_SUPPORT=$(json_extract "$HTTP_BODY" ".custom_jwt")
    assert_not_empty "$CUSTOM_JWT_SUPPORT" "custom_jwt token type is supported"
else
    tap_not_ok "13" "Get supported types failed"
    tap_not_ok "14" "custom_jwt support check failed"
fi

test_teardown "introspect"