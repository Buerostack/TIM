#!/bin/bash
# TIM Testing Framework - Master Test Runner
# Usage: ./run-tests.sh [options] [test-pattern]
# Examples:
#   ./run-tests.sh                    # Run all tests
#   ./run-tests.sh jwt                # Run JWT-related tests
#   ./run-tests.sh --verbose          # Run with verbose output
#   ./run-tests.sh --integration      # Run integration tests only

set -euo pipefail

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Source utilities
source "lib/test-utils.sh"

# Test execution modes
RUN_UNIT_TESTS=true
RUN_INTEGRATION_TESTS=true
TEST_PATTERN=""
PARALLEL_TESTS=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose|-v)
            TEST_VERBOSE=true
            shift
            ;;
        --parallel|-p)
            PARALLEL_TESTS=true
            shift
            ;;
        --unit-only)
            RUN_INTEGRATION_TESTS=false
            shift
            ;;
        --integration-only)
            RUN_UNIT_TESTS=false
            shift
            ;;
        --timeout=*)
            TIM_TIMEOUT="${1#*=}"
            shift
            ;;
        --base-url=*)
            TIM_BASE_URL="${1#*=}"
            shift
            ;;
        --help|-h)
            cat << EOF
TIM Testing Framework

Usage: $0 [options] [test-pattern]

Options:
  --verbose, -v           Enable verbose output
  --parallel, -p          Run tests in parallel
  --unit-only            Run only unit tests (individual endpoints)
  --integration-only     Run only integration tests
  --timeout=SECONDS      Set HTTP timeout (default: 30)
  --base-url=URL         Set TIM base URL (default: http://localhost:8085)
  --help, -h             Show this help

Test Pattern:
  Optional pattern to filter test files (e.g., "jwt", "introspect")

Environment Variables:
  TIM_BASE_URL           TIM server base URL
  TEST_VERBOSE           Enable verbose output (true/false)
  TEST_PARALLEL          Enable parallel execution (true/false)
  TEST_OUTPUT_DIR        Test output directory (default: ./reports)

Examples:
  $0                           # Run all tests
  $0 jwt                       # Run JWT-related tests
  $0 --verbose introspect      # Run introspection tests with verbose output
  $0 --integration-only        # Run only integration tests
EOF
            exit 0
            ;;
        -*)
            log_error "Unknown option: $1"
            exit 1
            ;;
        *)
            TEST_PATTERN="$1"
            shift
            ;;
    esac
done

# Global test results
TOTAL_TESTS=0
TOTAL_PASSED=0
TOTAL_FAILED=0
FAILED_TEST_FILES=()

# Test execution function
run_test_file() {
    local test_file="$1"
    local test_name=$(basename "$test_file" .sh)

    log_info "Running test: $test_name"

    if [[ "$TEST_VERBOSE" == "true" ]]; then
        bash "$test_file"
    else
        bash "$test_file" 2>/dev/null
    fi

    local exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
        log_success "Test passed: $test_name"
        return 0
    else
        log_error "Test failed: $test_name (exit code: $exit_code)"
        FAILED_TEST_FILES+=("$test_name")
        return $exit_code
    fi
}

# Main test execution
main() {
    log_info "TIM Testing Framework"
    log_info "Base URL: $TIM_BASE_URL"
    log_info "Timeout: ${TIM_TIMEOUT}s"
    log_info "Output Directory: $TEST_OUTPUT_DIR"

    # Check server health before running tests
    if ! check_server; then
        log_error "Server health check failed. Ensure TIM is running at $TIM_BASE_URL"
        exit 1
    fi

    # Clean up previous test results
    rm -rf "$TEST_OUTPUT_DIR"
    mkdir -p "$TEST_OUTPUT_DIR"

    local test_files=()

    # Collect unit tests
    if [[ "$RUN_UNIT_TESTS" == "true" ]]; then
        while IFS= read -r -d '' file; do
            if [[ -z "$TEST_PATTERN" ]] || [[ "$file" == *"$TEST_PATTERN"* ]]; then
                test_files+=("$file")
            fi
        done < <(find endpoints -name "*.sh" -type f -print0 2>/dev/null || true)
    fi

    # Collect integration tests
    if [[ "$RUN_INTEGRATION_TESTS" == "true" ]]; then
        while IFS= read -r -d '' file; do
            if [[ -z "$TEST_PATTERN" ]] || [[ "$file" == *"$TEST_PATTERN"* ]]; then
                test_files+=("$file")
            fi
        done < <(find integration -name "*.sh" -type f -print0 2>/dev/null || true)
    fi

    if [[ ${#test_files[@]} -eq 0 ]]; then
        log_warning "No test files found matching pattern: ${TEST_PATTERN:-*}"
        exit 0
    fi

    log_info "Found ${#test_files[@]} test files to execute"

    # Execute tests
    local start_time=$(date +%s)

    if [[ "$PARALLEL_TESTS" == "true" ]]; then
        log_info "Running tests in parallel..."

        local pids=()
        for test_file in "${test_files[@]}"; do
            run_test_file "$test_file" &
            pids+=($!)
        done

        # Wait for all tests to complete
        local failed_count=0
        for pid in "${pids[@]}"; do
            if ! wait "$pid"; then
                failed_count=$((failed_count + 1))
            fi
        done

        TOTAL_FAILED=$failed_count
        TOTAL_PASSED=$((${#test_files[@]} - failed_count))
        TOTAL_TESTS=${#test_files[@]}
    else
        log_info "Running tests sequentially..."

        for test_file in "${test_files[@]}"; do
            TOTAL_TESTS=$((TOTAL_TESTS + 1))

            if run_test_file "$test_file"; then
                TOTAL_PASSED=$((TOTAL_PASSED + 1))
            else
                TOTAL_FAILED=$((TOTAL_FAILED + 1))
            fi
        done
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # Generate test report
    generate_report "$duration"

    # Exit with appropriate code
    if [[ $TOTAL_FAILED -gt 0 ]]; then
        log_error "Test suite failed with $TOTAL_FAILED failures"
        exit 1
    else
        log_success "All tests passed!"
        exit 0
    fi
}

# Generate test report
generate_report() {
    local duration="$1"
    local report_file="$TEST_OUTPUT_DIR/test-report.txt"

    cat > "$report_file" << EOF
TIM Test Suite Report
=====================

Execution Summary:
  Total Tests: $TOTAL_TESTS
  Passed: $TOTAL_PASSED
  Failed: $TOTAL_FAILED
  Duration: ${duration}s
  Timestamp: $(date)

Configuration:
  Base URL: $TIM_BASE_URL
  Timeout: ${TIM_TIMEOUT}s
  Parallel: $PARALLEL_TESTS
  Verbose: $TEST_VERBOSE

EOF

    if [[ $TOTAL_FAILED -gt 0 ]]; then
        cat >> "$report_file" << EOF
Failed Tests:
EOF
        for failed_test in "${FAILED_TEST_FILES[@]}"; do
            echo "  - $failed_test" >> "$report_file"
        done
        echo "" >> "$report_file"
    fi

    cat >> "$report_file" << EOF
Test Details:
  See individual test logs in $TEST_OUTPUT_DIR/
EOF

    log_info "Test report generated: $report_file"

    # Also output summary to console
    echo ""
    echo "========================================="
    echo "Test Summary:"
    echo "  Total: $TOTAL_TESTS"
    echo "  Passed: $TOTAL_PASSED"
    echo "  Failed: $TOTAL_FAILED"
    echo "  Duration: ${duration}s"
    echo "========================================="
}

# Handle script interruption
trap 'log_error "Test execution interrupted"; exit 130' INT TERM

# Run main function
main "$@"