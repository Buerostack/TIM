# TIM Testing Framework

A comprehensive testing framework for TIM (Token Identity Manager) using bash scripts and curl.

## Overview

This testing framework follows industry best practices:
- **TAP (Test Anything Protocol)** for standardized test output
- **Modular design** with individual test files per endpoint
- **Integration testing** with data flow between endpoints
- **Parallel execution** support for faster test runs
- **Detailed reporting** with test results and timing

## Quick Start

```bash
# Run all tests
./run-tests.sh

# Run with verbose output
./run-tests.sh --verbose

# Run specific test pattern
./run-tests.sh jwt

# Run only integration tests
./run-tests.sh --integration-only

# Run tests in parallel
./run-tests.sh --parallel
```

## Directory Structure

```
testing/
├── run-tests.sh              # Master test runner
├── lib/
│   ├── test-utils.sh         # Common utilities and assertions
│   └── config.sh             # Test configuration
├── endpoints/
│   ├── jwt-generate.sh       # Test /jwt/custom/generate
│   ├── jwt-extend.sh         # Test /jwt/custom/extend
│   ├── jwt-revoke.sh         # Test /jwt/custom/revoke
│   ├── jwt-list.sh           # Test /jwt/custom/list
│   └── introspect.sh         # Test /introspect
├── integration/
│   └── full-lifecycle.sh     # End-to-end lifecycle test
└── reports/
    └── test-report.txt       # Generated test reports
```

## Available Tests

### Endpoint Tests (Unit)
- **jwt-generate.sh**: Tests JWT generation with various parameters
- **jwt-extend.sh**: Tests JWT lifetime extension
- **jwt-revoke.sh**: Tests JWT revocation
- **jwt-list.sh**: Tests JWT listing and filtering
- **introspect.sh**: Tests token introspection (RFC 7662)

### Integration Tests
- **full-lifecycle.sh**: Complete JWT lifecycle (Generate → Extend → Revoke → Introspect)

## Configuration

### Environment Variables

```bash
# TIM server configuration
export TIM_BASE_URL="http://localhost:8085"
export TIM_TIMEOUT="30"

# Test configuration
export TEST_VERBOSE="true"
export TEST_PARALLEL="false"
export TEST_OUTPUT_DIR="./reports"

# Test data
export TEST_USER="test-user"
export TEST_JWT_NAME="test-token"
```

### Command Line Options

```bash
./run-tests.sh [options] [test-pattern]

Options:
  --verbose, -v           Enable verbose output
  --parallel, -p          Run tests in parallel
  --unit-only            Run only unit tests
  --integration-only     Run only integration tests
  --timeout=SECONDS      Set HTTP timeout
  --base-url=URL         Set TIM base URL
  --help, -h             Show help
```

## Test Output

### TAP Format
Tests use [Test Anything Protocol](https://testanything.org/) for standardized output:

```
1..10
ok 1 - Generate basic JWT returns 200
ok 2 - Response contains token
not ok 3 - JWT has 3 parts
  # expected '3', got '2'
```

### Console Output
Color-coded console output with test progress:

```
[INFO] Starting test: jwt-generate
[PASS] Generate basic JWT returns 200
[PASS] Response contains token
[FAIL] JWT has 3 parts - expected '3', got '2'
```

### Test Reports
Detailed reports generated in `reports/test-report.txt`:

```
TIM Test Suite Report
=====================

Execution Summary:
  Total Tests: 25
  Passed: 23
  Failed: 2
  Duration: 15s
```

## Writing New Tests

### Endpoint Test Template

```bash
#!/bin/bash
# Test [Endpoint Name]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/test-utils.sh"

test_setup "test-name"
tap_plan 5  # Number of assertions

# Test 1: Basic functionality
if http_request "POST" "$TIM_BASE_URL/endpoint" "$request_data"; then
    assert_http_status "200" "Endpoint returns 200"
    assert_json_key "expected_field" "Response contains expected field"

    # Extract and validate data
    VALUE=$(json_extract "$HTTP_BODY" ".field")
    assert_equals "expected" "$VALUE" "Field has correct value"
fi

test_teardown "test-name"
```

### Available Assertions

```bash
# HTTP assertions
assert_http_status "200" "Request successful"

# JSON assertions
assert_json_key "token" "Response contains token"

# Value assertions
assert_equals "expected" "$actual" "Values match"
assert_not_empty "$value" "Value is not empty"
```

### JWT Utilities

```bash
# Extract JWT claims
JWT_SUB=$(jwt_extract_claim "$token" "sub")
JWT_EXP=$(jwt_extract_claim "$token" "exp")

# Decode JWT payload
PAYLOAD=$(jwt_decode_payload "$token")
```

## Current Test Coverage

### ✅ Currently Testable
- Custom JWT generation (`/jwt/custom/generate`)
- JWT extension (`/jwt/custom/extend`)
- JWT revocation (`/jwt/custom/revoke`)
- JWT listing (`/jwt/custom/list`)
- Token introspection (`/introspect`)

### ❌ Future Tests (when implemented)
- OAuth2/TARA authentication endpoints
- OAuth2 token introspection
- Multi-provider token validation

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run TIM Tests
  run: |
    cd testing
    ./run-tests.sh --parallel
  env:
    TIM_BASE_URL: http://localhost:8085
```

### Docker Integration
```bash
# Run tests against dockerized TIM
docker-compose up -d tim
./run-tests.sh --base-url=http://localhost:8085
```

## Troubleshooting

### Common Issues

1. **Server not running**: Ensure TIM is running at the configured URL
2. **Timeout errors**: Increase timeout with `--timeout=60`
3. **Permission errors**: Run `chmod +x *.sh` in test directories
4. **JSON parsing errors**: Ensure `jq` is installed

### Debug Mode
```bash
# Enable verbose output and debug logging
TEST_VERBOSE=true ./run-tests.sh --verbose
```

### Manual Test Execution
```bash
# Run individual test files
./endpoints/jwt-generate.sh
./integration/full-lifecycle.sh
```

## Best Practices

1. **Idempotent tests**: Tests should not depend on previous test state
2. **Cleanup**: Use test_teardown for cleanup operations
3. **Meaningful assertions**: Use descriptive assertion messages
4. **Error handling**: Handle HTTP errors gracefully
5. **Data isolation**: Use unique test data per test run