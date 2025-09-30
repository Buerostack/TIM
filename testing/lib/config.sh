#!/bin/bash
# TIM Testing Configuration

# Server Configuration
TIM_BASE_URL="${TIM_BASE_URL:-http://localhost:8085}"
TIM_TIMEOUT="${TIM_TIMEOUT:-30}"

# Test Configuration
TEST_OUTPUT_DIR="${TEST_OUTPUT_DIR:-./reports}"
TEST_VERBOSE="${TEST_VERBOSE:-false}"
TEST_PARALLEL="${TEST_PARALLEL:-false}"

# Test Data
TEST_USER="${TEST_USER:-test-user}"
TEST_JWT_NAME="${TEST_JWT_NAME:-test-token}"
TEST_AUDIENCE="${TEST_AUDIENCE:-tim-audience}"

# Colors for output
if [[ -t 1 ]]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    NC='\033[0m' # No Color
else
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    NC=''
fi

# Ensure output directory exists
mkdir -p "$TEST_OUTPUT_DIR"