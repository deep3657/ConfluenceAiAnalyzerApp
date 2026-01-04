#!/bin/bash

# End-to-End Test Script for Confluence AI Analyzer
# Tests ingestion and search functionality

set -e

# Configuration
API_BASE_URL="http://localhost:8080"
DB_USER="saideepak.b"
DB_NAME="rca_engine"
TEST_LIMIT=10
EXPECTED_SUCCESS_RATE=80

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================================="
echo "  Confluence AI Analyzer - End-to-End Test Suite"
echo "=================================================="
echo ""

# Function to print colored output
print_status() {
    if [ "$2" == "success" ]; then
        echo -e "${GREEN}✓ $1${NC}"
    elif [ "$2" == "fail" ]; then
        echo -e "${RED}✗ $1${NC}"
    else
        echo -e "${YELLOW}→ $1${NC}"
    fi
}

# Function to check API health
check_health() {
    print_status "Checking API health..." "info"
    response=$(curl -s "${API_BASE_URL}/actuator/health" 2>/dev/null || echo "")
    if echo "$response" | grep -q '"status":"UP"'; then
        print_status "API is healthy" "success"
        return 0
    else
        print_status "API is not responding" "fail"
        return 1
    fi
}

# Function to clean up test data
cleanup_test_data() {
    print_status "Cleaning up existing test data..." "info"
    
    # Delete all embeddings and reset page statuses
    PGPASSWORD="" psql -h localhost -U $DB_USER -d $DB_NAME -q -c "
        DELETE FROM rca_embeddings;
        DELETE FROM parsed_rcas;
        UPDATE rca_pages SET status = 'PENDING', error_message = NULL, embedding_generated_at = NULL, parsed_at = NULL;
    " 2>/dev/null || true
    
    print_status "Test data cleaned" "success"
}

# Function to trigger sync with limit
trigger_sync() {
    local limit=$1
    print_status "Triggering sync with limit=$limit..." "info"
    
    response=$(curl -s -X POST "${API_BASE_URL}/api/v1/ingestion/sync" \
        -H "Content-Type: application/json" \
        -d "{\"spaceKeys\": [\"IMRCA\"], \"tags\": [], \"limit\": $limit}" 2>/dev/null || echo "")
    
    if [ -z "$response" ]; then
        print_status "Sync request failed - empty response" "fail"
        return 1
    fi
    
    # Check for error
    if echo "$response" | grep -q '"error"'; then
        print_status "Sync request failed: $response" "fail"
        return 1
    fi
    
    print_status "Sync triggered successfully" "success"
    return 0
}

# Function to wait for sync to complete
wait_for_sync() {
    local max_wait=120  # 2 minutes max
    local wait_time=0
    local interval=5
    
    print_status "Waiting for sync to complete (max ${max_wait}s)..." "info"
    
    while [ $wait_time -lt $max_wait ]; do
        # Check page statuses
        result=$(PGPASSWORD="" psql -h localhost -U $DB_USER -d $DB_NAME -t -c "
            SELECT 
                COUNT(*) FILTER (WHERE status = 'PENDING') as pending,
                COUNT(*) FILTER (WHERE status = 'EMBEDDED') as embedded,
                COUNT(*) FILTER (WHERE status = 'ERROR') as error
            FROM rca_pages
            WHERE status IN ('PENDING', 'EMBEDDED', 'ERROR', 'PARSED');
        " 2>/dev/null | tr -d ' ')
        
        pending=$(echo "$result" | cut -d'|' -f1)
        embedded=$(echo "$result" | cut -d'|' -f2)
        error=$(echo "$result" | cut -d'|' -f3)
        
        total=$((embedded + error))
        
        if [ "$total" -ge "$TEST_LIMIT" ]; then
            print_status "Sync completed: $embedded embedded, $error errors" "success"
            return 0
        fi
        
        echo "  ... Processed: $total/$TEST_LIMIT (Embedded: $embedded, Errors: $error)"
        sleep $interval
        wait_time=$((wait_time + interval))
    done
    
    print_status "Sync timed out after ${max_wait}s" "fail"
    return 1
}

# Function to verify ingestion results
verify_ingestion() {
    print_status "Verifying ingestion results..." "info"
    
    # Get counts
    result=$(PGPASSWORD="" psql -h localhost -U $DB_USER -d $DB_NAME -t -c "
        SELECT 
            COUNT(*) FILTER (WHERE status = 'EMBEDDED') as embedded,
            COUNT(*) FILTER (WHERE status = 'ERROR') as error,
            (SELECT COUNT(*) FROM rca_embeddings) as embeddings
        FROM rca_pages;
    " 2>/dev/null | tr -d ' ')
    
    embedded=$(echo "$result" | cut -d'|' -f1)
    error=$(echo "$result" | cut -d'|' -f2)
    embeddings=$(echo "$result" | cut -d'|' -f3)
    
    total=$((embedded + error))
    
    if [ "$total" -eq 0 ]; then
        print_status "No pages were processed" "fail"
        return 1
    fi
    
    success_rate=$((embedded * 100 / total))
    
    echo ""
    echo "  Ingestion Results:"
    echo "  ─────────────────────────────"
    echo "  Pages Embedded: $embedded"
    echo "  Pages Failed:   $error"
    echo "  Total Embeddings: $embeddings"
    echo "  Success Rate: ${success_rate}%"
    echo ""
    
    if [ "$success_rate" -ge "$EXPECTED_SUCCESS_RATE" ]; then
        print_status "Ingestion success rate: ${success_rate}% (≥${EXPECTED_SUCCESS_RATE}% required)" "success"
        return 0
    else
        print_status "Ingestion success rate: ${success_rate}% (<${EXPECTED_SUCCESS_RATE}% required)" "fail"
        return 1
    fi
}

# Function to test search
test_search() {
    local query="$1"
    local test_name="$2"
    
    response=$(curl -s -X POST "${API_BASE_URL}/api/v1/search" \
        -H "Content-Type: application/json" \
        -d "{\"query\": \"$query\", \"limit\": 5}" 2>/dev/null || echo "")
    
    if [ -z "$response" ]; then
        print_status "Search test '$test_name': Empty response" "fail"
        return 1
    fi
    
    # Check for error
    if echo "$response" | grep -q '"error"'; then
        print_status "Search test '$test_name': API error" "fail"
        return 1
    fi
    
    # Check if results array exists and has items
    result_count=$(echo "$response" | grep -o '"pageId"' | wc -l | tr -d ' ')
    
    if [ "$result_count" -gt 0 ]; then
        print_status "Search test '$test_name': Found $result_count results" "success"
        return 0
    else
        print_status "Search test '$test_name': No results found" "fail"
        return 1
    fi
}

# Function to run search tests
run_search_tests() {
    print_status "Running search tests..." "info"
    echo ""
    
    local passed=0
    local failed=0
    
    # Test 1: Generic incident search
    if test_search "incident issue problem" "Generic incident search"; then
        passed=$((passed + 1))
    else
        failed=$((failed + 1))
    fi
    
    # Test 2: Root cause search
    if test_search "root cause analysis" "Root cause search"; then
        passed=$((passed + 1))
    else
        failed=$((failed + 1))
    fi
    
    # Test 3: Resolution/fix search
    if test_search "fix resolution solution" "Resolution search"; then
        passed=$((passed + 1))
    else
        failed=$((failed + 1))
    fi
    
    # Test 4: Service/system search
    if test_search "service error failure" "Service error search"; then
        passed=$((passed + 1))
    else
        failed=$((failed + 1))
    fi
    
    # Test 5: Database related search
    if test_search "database connection timeout" "Database search"; then
        passed=$((passed + 1))
    else
        failed=$((failed + 1))
    fi
    
    total=$((passed + failed))
    search_success_rate=$((passed * 100 / total))
    
    echo ""
    echo "  Search Test Results:"
    echo "  ─────────────────────────────"
    echo "  Passed: $passed"
    echo "  Failed: $failed"
    echo "  Success Rate: ${search_success_rate}%"
    echo ""
    
    if [ "$search_success_rate" -ge "$EXPECTED_SUCCESS_RATE" ]; then
        print_status "Search tests: ${search_success_rate}% passed (≥${EXPECTED_SUCCESS_RATE}% required)" "success"
        return 0
    else
        print_status "Search tests: ${search_success_rate}% passed (<${EXPECTED_SUCCESS_RATE}% required)" "fail"
        return 1
    fi
}

# Function to get sample embedded page titles for verification
show_sample_pages() {
    print_status "Sample embedded pages:" "info"
    PGPASSWORD="" psql -h localhost -U $DB_USER -d $DB_NAME -c "
        SELECT title, status 
        FROM rca_pages 
        WHERE status = 'EMBEDDED' 
        LIMIT 5;
    " 2>/dev/null
}

# Main test execution
main() {
    local start_time=$(date +%s)
    local ingestion_passed=false
    local search_passed=false
    
    echo ""
    
    # Step 1: Check API health
    if ! check_health; then
        print_status "API is not running. Please start the backend first." "fail"
        exit 1
    fi
    echo ""
    
    # Step 2: Clean up test data
    cleanup_test_data
    echo ""
    
    # Step 3: Trigger sync
    if ! trigger_sync $TEST_LIMIT; then
        print_status "Failed to trigger sync" "fail"
        exit 1
    fi
    echo ""
    
    # Step 4: Wait for sync to complete
    if ! wait_for_sync; then
        print_status "Sync did not complete in time" "fail"
        exit 1
    fi
    echo ""
    
    # Step 5: Verify ingestion
    if verify_ingestion; then
        ingestion_passed=true
    fi
    echo ""
    
    # Step 6: Show sample pages
    show_sample_pages
    echo ""
    
    # Step 7: Run search tests
    if run_search_tests; then
        search_passed=true
    fi
    echo ""
    
    # Final summary
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo "=================================================="
    echo "                  TEST SUMMARY"
    echo "=================================================="
    echo ""
    echo "  Duration: ${duration}s"
    echo ""
    
    if $ingestion_passed; then
        print_status "Ingestion Tests: PASSED" "success"
    else
        print_status "Ingestion Tests: FAILED" "fail"
    fi
    
    if $search_passed; then
        print_status "Search Tests: PASSED" "success"
    else
        print_status "Search Tests: FAILED" "fail"
    fi
    
    echo ""
    
    if $ingestion_passed && $search_passed; then
        echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║           ALL TESTS PASSED! ✓                    ║${NC}"
        echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
        exit 0
    else
        echo -e "${RED}╔══════════════════════════════════════════════════╗${NC}"
        echo -e "${RED}║           SOME TESTS FAILED ✗                    ║${NC}"
        echo -e "${RED}╚══════════════════════════════════════════════════╝${NC}"
        exit 1
    fi
}

# Run main
main

