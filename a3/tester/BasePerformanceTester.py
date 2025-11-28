import requests
import time
import random
import string
import psutil
import statistics
import threading
from typing import Dict, List, Any, Optional
import sys
import os

# Get the absolute path of the current script
current_dir = os.path.dirname(os.path.abspath(__file__))

# Get the parent directory (project/)
parent_dir = os.path.dirname(current_dir)

# Add the parent directory to sys.path
sys.path.append(parent_dir)

# Now you can import from the sibling folder
from utils.ResourceMonitor import ResourceMonitor


class BasePerformanceTester:
    """
    Base class containing the logic to run Create/Update/Delete cycles.
    Subclasses must define the 'endpoint' and '_generate_payload'.
    """
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.monitor = ResourceMonitor()
        self.endpoint = "" 

    def _generate_payload(self) -> Dict[str, Any]:
        """Child classes must override this to return correct JSON data."""
        raise NotImplementedError("Subclasses must implement _generate_payload")

    def _measure_operation(self, operation_name: str, func, *args) -> float:
        start = time.perf_counter()
        func(*args)
        end = time.perf_counter()
        return end - start

    def run_experiment(self, num_objects: int) -> Dict[str, Any]:
        target_url = f"{self.base_url}/{self.endpoint}"
        print(f"\n--- Starting Experiment ({self.endpoint}): Load = {num_objects} objects ---")
        
        self.monitor.start()
        
        created_ids = []
        
        # 1. CREATE PHASE
        def create_all():
            for _ in range(num_objects):
                try:
                    resp = requests.post(target_url, json=self._generate_payload())
                    if resp.status_code in [200, 201]:
                        # Assumes the API returns an 'id' field
                        created_ids.append(resp.json().get('id'))
                except requests.RequestException:
                    pass

        create_time = self._measure_operation("Create", create_all)

        # 2. UPDATE PHASE
        def update_all():
            for obj_id in created_ids:
                try:
                    requests.put(
                        f"{target_url}/{obj_id}", 
                        json=self._generate_payload()
                    )
                except requests.RequestException:
                    pass

        update_time = self._measure_operation("Update", update_all)

        # 3. DELETE PHASE
        def delete_all():
            for obj_id in created_ids:
                try:
                    requests.delete(f"{target_url}/{obj_id}")
                except requests.RequestException:
                    pass

        delete_time = self._measure_operation("Delete", delete_all)

        # 4. GET PHASE
        def get_all():
            try:
                requests.get(target_url)
            except requests.RequestException:
                pass

        get_time = self._measure_operation("Get", get_all)

        sys_stats = self.monitor.stop()

        results = {
            "endpoint": self.endpoint,
            "load_count": num_objects,
            "create_time_sec": round(create_time, 4),
            "update_time_sec": round(update_time, 4),
            "delete_time_sec": round(delete_time, 4),
            "get_time_sec": round(get_time, 4),
            **sys_stats
        }

        self._print_results(results)
        return results

    def _print_results(self, r: Dict[str, Any]):
        print(f"Results for {r['endpoint']} ({r['load_count']} objects):")
        print(f"  > Create Time: {r['create_time_sec']}s")
        print(f"  > Update Time: {r['update_time_sec']}s")
        print(f"  > Delete Time: {r['delete_time_sec']}s")
        print(f"  > Get Time: {r['get_time_sec']}s")
        print(f"  > Avg CPU Use: {r['avg_cpu_percent']}%")
        print(f"  > Avg Free RAM: {r['avg_memory_free_mb']} MB")