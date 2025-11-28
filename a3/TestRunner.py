import time
import requests
import sys
from tester.TodoTester import TodoTester
from tester.ProjectTester import ProjectTester

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

    # Instantiate our specific testers
    testers = [
        ("Todo", TodoTester(API_URL)),
        ("Project", ProjectTester(API_URL))
    ]

    all_results = []

    # 2. Run experiments for each tester and each load level
    for name, tester in testers:
        print(f"\n--- Testing Object Type: {name} ---")
        
        for load in LOAD_LEVELS:
            # Run the experiment
            result = tester.run_experiment(load)
            
            # Inject the 'type' name into the result dictionary
            result['type'] = name 
            all_results.append(result)
            
            # Optional: Cool-down pause between tests
            time.sleep(1)

    # 3. Print Final Summary Table
    # I widened the columns slightly to fit the headers nicely
    print("\n" + "="*130)
    print(f"{'Type':<8} | {'Objects':<8} | {'Create(s)':<10} | {'Update(s)':<10} | {'Delete(s)':<10} | {'Abs CPU%':<10} | {'Rel CPU%':<10} | {'Abs Free MB':<12} | {'Rel Used MB'}")
    print("-" * 130)
    
    for r in all_results:
        print(f"{r['type']:<8} | {r['load_count']:<8} | {r['create_time_sec']:<10} | {r['update_time_sec']:<10} | {r['delete_time_sec']:<10} | {r['avg_cpu_percent']:<10} | {r['rel_cpu_increase']:<10} | {r['avg_memory_free_mb']:<12} | {r['rel_mem_consumed_mb']} MB")
    print("="*130)

if __name__ == "__main__":
    main()