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
parent_dir = os.path.dirname(current_dir)
sys.path.append(parent_dir)

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

    def _measure_operation(self, operation_name: str, func, *args) -> Dict[str, float]:
        """
        Runs the function while tracking time AND system resources specifically 
        for this duration.
        """
        # 1. Start the resource monitor for this specific phase
        self.monitor.start()
        
        # 2. Start Timer
        start_time = time.perf_counter()
        
        # 3. Run the operation
        func(*args)
        
        # 4. Stop Timer
        end_time = time.perf_counter()
        
        # 5. Stop Monitor and get stats for this phase
        sys_stats = self.monitor.stop()
        
        duration = end_time - start_time
        
        # Return combined stats
        return {
            "time_sec": duration,
            "cpu_avg": sys_stats.get('avg_cpu_percent', 0),
            "ram_avg": sys_stats.get('avg_memory_free_mb', 0)
        }

    def run_experiment(self, num_objects: int) -> Dict[str, Any]:
        target_url = f"{self.base_url}/{self.endpoint}"
        print(f"\n--- Starting Experiment ({self.endpoint}): Load = {num_objects} objects ---")
        
        created_ids = []
        
        # 1. CREATE PHASE
        def create_all():
            for _ in range(num_objects):
                try:
                    resp = requests.post(target_url, json=self._generate_payload())
                    if resp.status_code in [200, 201]:
                        created_ids.append(resp.json().get('id'))
                except requests.RequestException:
                    pass

        create_stats = self._measure_operation("Create", create_all)

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

        update_stats = self._measure_operation("Update", update_all)

        # 3. DELETE PHASE
        def delete_all():
            for obj_id in created_ids:
                try:
                    requests.delete(f"{target_url}/{obj_id}")
                except requests.RequestException:
                    pass

        delete_stats = self._measure_operation("Delete", delete_all)

        # Flatten the results for easier DataFrame conversion later
        results = {
            "endpoint": self.endpoint,
            "load_count": num_objects,
            
            # Create Stats
            "create_time_sec": round(create_stats["time_sec"], 4),
            "create_cpu_avg": create_stats["cpu_avg"],
            "create_ram_avg": create_stats["ram_avg"],
            
            # Update Stats
            "update_time_sec": round(update_stats["time_sec"], 4),
            "update_cpu_avg": update_stats["cpu_avg"],
            "update_ram_avg": update_stats["ram_avg"],
            
            # Delete Stats
            "delete_time_sec": round(delete_stats["time_sec"], 4),
            "delete_cpu_avg": delete_stats["cpu_avg"],
            "delete_ram_avg": delete_stats["ram_avg"],
        }

        self._print_results(results)
        return results

    def _print_results(self, r: Dict[str, Any]):
        print(f"Results for {r['endpoint']} ({r['load_count']} objects):")
        print(f"  [Create] Time: {r['create_time_sec']}s | CPU: {r['create_cpu_avg']}% | RAM: {r['create_ram_avg']}MB")
        print(f"  [Update] Time: {r['update_time_sec']}s | CPU: {r['update_cpu_avg']}% | RAM: {r['update_ram_avg']}MB")
        print(f"  [Delete] Time: {r['delete_time_sec']}s | CPU: {r['delete_cpu_avg']}% | RAM: {r['delete_ram_avg']}MB")