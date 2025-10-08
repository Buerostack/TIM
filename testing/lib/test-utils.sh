#!/bin/bash
# TIM Testing Utilities

# Source configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# Global test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Test result tracking
declare -a FAILED_TESTS=()

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" >&2
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1" >&2
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1" >&2
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1" >&2
}

log_debug() {
    if [[ "$TEST_VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1" >&2
    fi
}

# TAP (Test Anything Protocol) output functions
tap_plan() {
    echo "1..$1"
}

tap_ok() {
    local test_num=$1
    local description=$2
    echo "ok $test_num - $description"
}

tap_not_ok() {
    local test_num=$1
    local description=$2
    local details="${3:-}"
    echo "not ok $test_num - $description"
    if [[ -n "$details" ]]; then
        echo "  # $details"
    fi
}

# HTTP request wrapper with better error handling
http_request() {
    local method="$1"
    local url="$2"
    local data="${3:-}"
    local content_type="${4:-application/json}"
    local expect_status="${5:-200}"

    local curl_args=(
        -s
        -w "HTTPSTATUS:%{http_code};SIZE:%{size_download};TIME:%{time_total}"
        -H "Content-Type: $content_type"
        -X "$method"
        --max-time "$TIM_TIMEOUT"
    )

    if [[ -n "$data" ]]; then
        curl_args+=(-d "$data")
    fi

    local response
    response=$(curl "${curl_args[@]}" "$url" 2>&1)
    local curl_exit_code=$?

    if [[ $curl_exit_code -ne 0 ]]; then
        log_error "curl failed with exit code $curl_exit_code"
        return $curl_exit_code
    fi

    # Extract status and body
    local http_status=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    local response_time=$(echo "$response" | grep -o "TIME:[0-9.]*" | cut -d: -f2)
    local body=$(echo "$response" | sed -E 's/HTTPSTATUS:[0-9]*;SIZE:[0-9]*;TIME:[0-9.]*$//')

    log_debug "HTTP $method $url -> $http_status (${response_time}s)"

    # Store results in global variables for caller
    HTTP_STATUS="$http_status"
    HTTP_BODY="$body"
    HTTP_TIME="$response_time"

    # Check if status matches expectation
    if [[ "$http_status" != "$expect_status" ]]; then
        log_error "Expected HTTP $expect_status, got $http_status"
        log_debug "Response body: $body"
        return 1
    fi

    return 0
}

# JSON extraction utilities
json_extract() {
    local json="$1"
    local path="$2"
    echo "$json" | jq -r "$path" 2>/dev/null || echo "null"
}

json_has_key() {
    local json="$1"
    local key="$2"
    echo "$json" | jq -e "has(\"$key\")" >/dev/null 2>&1
}

# JWT utilities
jwt_decode_payload() {
    local token="$1"
    local payload=$(echo "$token" | cut -d. -f2)
    # Add padding if needed
    local padding=$((4 - ${#payload} % 4))
    if [[ $padding -ne 4 ]]; then
        payload="${payload}$(printf '=' $padding)"
    fi
    echo "$payload" | base64 -d 2>/dev/null | jq . 2>/dev/null || echo "{}"
}

jwt_extract_claim() {
    local token="$1"
    local claim="$2"
    local payload=$(jwt_decode_payload "$token")
    json_extract "$payload" ".$claim"
}

# Test assertion functions
assert_equals() {
    local expected="$1"
    local actual="$2"
    local description="${3:-assertion}"

    TESTS_RUN=$((TESTS_RUN + 1))

    if [[ "$expected" == "$actual" ]]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        tap_ok "$TESTS_RUN" "$description"
        log_success "$description"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        FAILED_TESTS+=("$description")
        tap_not_ok "$TESTS_RUN" "$description" "expected '$expected', got '$actual'"
        log_error "$description - expected '$expected', got '$actual'"
        return 1
    fi
}

assert_not_empty() {
    local value="$1"
    local description="${2:-value should not be empty}"

    TESTS_RUN=$((TESTS_RUN + 1))

    if [[ -n "$value" && "$value" != "null" ]]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        tap_ok "$TESTS_RUN" "$description"
        log_success "$description"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        FAILED_TESTS+=("$description")
        tap_not_ok "$TESTS_RUN" "$description" "value was empty or null"
        log_error "$description - value was empty or null"
        return 1
    fi
}

assert_http_status() {
    local expected="$1"
    local description="${2:-HTTP status check}"

    assert_equals "$expected" "$HTTP_STATUS" "$description"
}

assert_json_key() {
    local key="$1"
    local description="${2:-JSON should contain key '$key'}"

    TESTS_RUN=$((TESTS_RUN + 1))

    if json_has_key "$HTTP_BODY" "$key"; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        tap_ok "$TESTS_RUN" "$description"
        log_success "$description"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        FAILED_TESTS+=("$description")
        tap_not_ok "$TESTS_RUN" "$description" "key '$key' not found in JSON"
        log_error "$description - key '$key' not found in JSON"
        return 1
    fi
}

# Test lifecycle functions
test_setup() {
    local test_name="$1"
    log_info "Starting test: $test_name"

    # Create test-specific output directory
    TEST_DIR="$TEST_OUTPUT_DIR/$test_name"
    mkdir -p "$TEST_DIR"

    # Reset counters for this test
    TESTS_RUN=0
    TESTS_PASSED=0
    TESTS_FAILED=0
    FAILED_TESTS=()
}

test_teardown() {
    local test_name="$1"

    # Output test summary
    log_info "Test summary for $test_name:"
    log_info "  Total: $TESTS_RUN"
    log_info "  Passed: $TESTS_PASSED"
    log_info "  Failed: $TESTS_FAILED"

    if [[ $TESTS_FAILED -gt 0 ]]; then
        log_error "Failed tests:"
        for failed_test in "${FAILED_TESTS[@]}"; do
            log_error "  - $failed_test"
        done
        return 1
    else
        log_success "All tests passed!"
        return 0
    fi
}

# Server health check
check_server() {
    log_info "Checking TIM server at $TIM_BASE_URL..."

    if http_request "GET" "$TIM_BASE_URL/health" "" "" "200"; then
        log_success "Server is healthy"
        return 0
    else
        log_error "Server health check failed"
        return 1
    fi
}

# Cleanup function
cleanup_test_data() {
    log_info "Cleaning up test data..."
    # This will be implemented based on your cleanup needs
    # For now, just log the intent
    log_debug "Test data cleanup completed"
}