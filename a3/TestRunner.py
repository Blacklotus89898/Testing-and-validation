import time
import requests
import sys
from PerformanceTester import RestPerformanceTester

# Configuration
API_URL = "http://localhost:4567"
LOAD_LEVELS = [10, 100, 500, 1000]

def main():
    print("==========================================================")
    print("      Part C: Non-Functional Performance Testing          ")
    print("==========================================================")
    
    # 1. Verify API is accessible
    try:
        requests.get(f"{API_URL}/todos", timeout=2)
        print(f"✅ Target API is online at {API_URL}")
    except requests.RequestException:
        print(f"❌ Error: Could not connect to {API_URL}")
        print("   Please ensure the Part A server is running.")
        sys.exit(1)

    tester = RestPerformanceTester(API_URL)
    all_results = []

    # 2. Run experiments for each load level
    for load in LOAD_LEVELS:
        result = tester.run_experiment(load)
        all_results.append(result)
        # Optional: Cool-down pause between tests
        time.sleep(1)

    # 3. Print Final Summary Table
    print("\n" + "="*85)
    print(f"{'Objects':<10} | {'Create(s)':<12} | {'Update(s)':<12} | {'Delete(s)':<12} | {'CPU%':<8} | {'Free RAM'}")
    print("-" * 85)
    for r in all_results:
        print(f"{r['load_count']:<10} | {r['create_time_sec']:<12} | {r['update_time_sec']:<12} | {r['delete_time_sec']:<12} | {r['avg_cpu_percent']:<8} | {r['avg_memory_free_mb']} MB")
    print("="*85)

if __name__ == "__main__":
    main()