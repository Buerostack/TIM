#!/bin/bash
# Test JWT Generate Endpoint

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/test-utils.sh"

test_setup "jwt-generate"

# Plan TAP output
tap_plan 10

# Test 1: Generate basic JWT
log_info "Test 1: Generate basic JWT"
request_data='{
  "JWTName": "test-basic-token",
  "content": {
    "sub": "test-user",
    "role": "user"
  },
  "expirationInMinutes": 60
}'

if http_request "POST" "$TIM_BASE_URL/jwt/custom/generate" "$request_data"; then
    assert_http_status "200" "Generate basic JWT returns 200"
    assert_json_key "token" "Response contains token"

    # Extract and validate token
    GENERATED_TOKEN=$(json_extract "$HTTP_BODY" ".token")
    assert_not_empty "$GENERATED_TOKEN" "Generated token is not empty"

    # Validate JWT structure (3 parts separated by dots)
    TOKEN_PARTS=$(echo "$GENERATED_TOKEN" | tr '.' '\n' | wc -l)
    assert_equals "3" "$TOKEN_PARTS" "JWT has 3 parts (header.payload.signature)"

    # Validate token claims
    JWT_SUB=$(jwt_extract_claim "$GENERATED_TOKEN" "sub")
    assert_equals "test-user" "$JWT_SUB" "JWT contains correct subject"

    JWT_TOKEN_TYPE=$(jwt_extract_claim "$GENERATED_TOKEN" "token_type")
    assert_equals "custom_jwt" "$JWT_TOKEN_TYPE" "JWT contains token_type claim"

    # Store token for other tests
    echo "$GENERATED_TOKEN" > "$TEST_DIR/generated_token.txt"
else
    tap_not_ok "1" "Generate basic JWT failed"
    tap_not_ok "2" "Response contains token"
    tap_not_ok "3" "Generated token is not empty"
    tap_not_ok "4" "JWT has 3 parts"
    tap_not_ok "5" "JWT contains correct subject"
    tap_not_ok "6" "JWT contains token_type claim"
fi

# Test 2: Generate JWT with custom claims
log_info "Test 2: Generate JWT with custom claims"
request_data='{
  "JWTName": "test-custom-claims",
  "content": {
    "sub": "test-user",
    "role": "admin",
    "department": "engineering",
    "permissions": ["read", "write", "delete"]
  },
  "expirationInMinutes": 120
}'

if http_request "POST" "$TIM_BASE_URL/jwt/custom/generate" "$request_data"; then
    assert_http_status "200" "Generate JWT with custom claims returns 200"

    CUSTOM_TOKEN=$(json_extract "$HTTP_BODY" ".token")
    JWT_ROLE=$(jwt_extract_claim "$CUSTOM_TOKEN" "role")
    assert_equals "admin" "$JWT_ROLE" "JWT contains custom role claim"

    JWT_DEPT=$(jwt_extract_claim "$CUSTOM_TOKEN" "department")
    assert_equals "engineering" "$JWT_DEPT" "JWT contains custom department claim"
else
    tap_not_ok "7" "Generate JWT with custom claims failed"
    tap_not_ok "8" "JWT contains custom role claim"
    tap_not_ok "9" "JWT contains custom department claim"
fi

# Test 3: Invalid request handling
log_info "Test 3: Invalid request handling"
invalid_data='{"invalid": "json"}'

if http_request "POST" "$TIM_BASE_URL/jwt/custom/generate" "$invalid_data" "application/json" "400"; then
    assert_http_status "400" "Invalid request returns 400"
else
    # If we get here, it means the request succeeded when it shouldn't have
    tap_not_ok "10" "Invalid request should return 400"
fi

test_teardown "jwt-generate"