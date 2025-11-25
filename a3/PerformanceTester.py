import requests
import time
import random
import string
import psutil
import statistics
import threading
from typing import Dict, List, Any, Optional

class ResourceMonitor:
    """
    Monitors system resources (CPU and Memory) in a background thread.
    Uses psutil as a cross-platform alternative to OS-specific tools like perfmon/vmstat.
    """
    def __init__(self, interval: float = 0.1):
        self.interval = interval
        self.cpu_log: List[float] = []
        self.mem_log: List[float] = []
        self._running = False
        self._thread: Optional[threading.Thread] = None

    def _monitor_loop(self):
        while self._running:
            # Capture CPU percentage (non-blocking)
            self.cpu_log.append(psutil.cpu_percent(interval=None))
            # Capture Available Memory in MB
            mem_info = psutil.virtual_memory()
            self.mem_log.append(mem_info.available / (1024 * 1024))
            time.sleep(self.interval)

    def start(self):
        """Start monitoring resources in the background."""
        self.cpu_log.clear()
        self.mem_log.clear()
        self._running = True
        self._thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._thread.start()

    def stop(self) -> Dict[str, float]:
        """Stop monitoring and return average statistics."""
        self._running = False
        if self._thread:
            self._thread.join()
        
        avg_cpu = statistics.mean(self.cpu_log) if self.cpu_log else 0.0
        avg_mem = statistics.mean(self.mem_log) if self.mem_log else 0.0
        
        return {
            "avg_cpu_percent": round(avg_cpu, 2),
            "avg_memory_free_mb": round(avg_mem, 2)
        }

class RestPerformanceTester:
    """
    Conducts performance tests (Create, Update, Delete) on a target REST API endpoint.
    """
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.monitor = ResourceMonitor()

    def _generate_payload(self) -> Dict[str, Any]:
        """
        Generates a random payload for the API.
        ADAPT THIS METHOD to match the schema of your Part A API.
        """
        return {
            "title": ''.join(random.choices(string.ascii_letters, k=10)),
            "description": ''.join(random.choices(string.ascii_lowercase, k=20)),
            "doneStatus": random.choice([True, False])
        }

    def _measure_operation(self, operation_name: str, func, *args) -> float:
        """Helper to measure execution time of a function."""
        start = time.perf_counter()
        func(*args)
        end = time.perf_counter()
        return end - start

    def run_experiment(self, num_objects: int) -> Dict[str, Any]:
        """
        Runs a full Create-Update-Delete cycle for `num_objects` items
        while monitoring system resources.
        """
        print(f"\n--- Starting Experiment: Load = {num_objects} objects ---")
        
        self.monitor.start()
        
        # 1. CREATE PHASE
        created_ids = []
        
        def create_all():
            for _ in range(num_objects):
                try:
                    resp = requests.post(f"{self.base_url}/todos", json=self._generate_payload())
                    if resp.status_code in [200, 201]:
                        created_ids.append(resp.json().get('id'))
                except requests.RequestException:
                    pass

        create_time = self._measure_operation("Create", create_all)

        # 2. UPDATE PHASE
        def update_all():
            for obj_id in created_ids:
                try:
                    requests.put(
                        f"{self.base_url}/todos/{obj_id}", 
                        json=self._generate_payload()
                    )
                except requests.RequestException:
                    pass

        update_time = self._measure_operation("Update", update_all)

        # 3. DELETE PHASE
        def delete_all():
            for obj_id in created_ids:
                try:
                    requests.delete(f"{self.base_url}/todos/{obj_id}")
                except requests.RequestException:
                    pass

        delete_time = self._measure_operation("Delete", delete_all)

        # Stop monitoring and get system stats
        sys_stats = self.monitor.stop()

        results = {
            "load_count": num_objects,
            "create_time_sec": round(create_time, 4),
            "update_time_sec": round(update_time, 4),
            "delete_time_sec": round(delete_time, 4),
            **sys_stats
        }

        self._print_results(results)
        return results

    def _print_results(self, r: Dict[str, Any]):
        print(f"Results for {r['load_count']} objects:")
        print(f"  > Create Time: {r['create_time_sec']}s")
        print(f"  > Update Time: {r['update_time_sec']}s")
        print(f"  > Delete Time: {r['delete_time_sec']}s")
        print(f"  > Avg CPU Use: {r['avg_cpu_percent']}%")
        print(f"  > Avg Free RAM: {r['avg_memory_free_mb']} MB")