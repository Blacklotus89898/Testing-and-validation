import time
import psutil
import statistics
import threading
from typing import Dict, List, Optional

class ResourceMonitor:
    """
    Monitors system resources.
    Calculates RELATIVE usage (Change from Start vs Average During Run).
    """
    def __init__(self, interval: float = 0.1):
        self.interval = interval
        self.cpu_log: List[float] = []
        self.mem_log: List[float] = []
        self._running = False
        self._thread: Optional[threading.Thread] = None
        
        # Baselines
        self.start_cpu_idle: float = 0.0
        self.start_mem_free: float = 0.0

    def _monitor_loop(self):
        while self._running:
            # Capture CPU percentage (non-blocking)
            self.cpu_log.append(psutil.cpu_percent(interval=None))
            
            # Capture Available Memory in MB
            mem_info = psutil.virtual_memory()
            self.mem_log.append(mem_info.available / (1024 * 1024))
            
            time.sleep(self.interval)

    def start(self):
        """Capture baseline and start monitoring."""
        self.cpu_log.clear()
        self.mem_log.clear()

        # 1. GET BASELINES (State before load)
        # Prime the CPU counter (first call is often 0.0)
        psutil.cpu_percent(interval=None)
        time.sleep(0.5) # Wait a moment to get a real reading
        self.start_cpu_idle = psutil.cpu_percent(interval=None)
        
        mem_info = psutil.virtual_memory()
        self.start_mem_free = mem_info.available / (1024 * 1024)

        # 2. Start Thread
        self._running = True
        self._thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._thread.start()

    def stop(self) -> Dict[str, float]:
        """Stop monitoring and return RELATIVE usage statistics."""
        self._running = False
        if self._thread:
            self._thread.join()
        
        # Calculate Averages during the run
        avg_run_cpu = statistics.mean(self.cpu_log) if self.cpu_log else 0.0
        avg_run_mem_free = statistics.mean(self.mem_log) if self.mem_log else 0.0
        
        # Calculate RELATIVE Usage (Delta)
        # CPU: How much higher was the CPU during the test compared to idle?
        cpu_increase = avg_run_cpu - self.start_cpu_idle
        
        # Memory: How much LESS memory is free now compared to the start? 
        # (Start Free - Avg Free during run = Memory Consumed)
        mem_consumed = self.start_mem_free - avg_run_mem_free

        return {
            # Absolute values (optional, keeping for reference)
            "avg_cpu_percent": round(avg_run_cpu, 2),
            "avg_memory_free_mb": round(avg_run_mem_free, 2),
            
            # RELATIVE values (What you asked for)
            "rel_cpu_increase": round(max(0, cpu_increase), 2), # avoid negative if system gets quieter
            "rel_mem_consumed_mb": round(mem_consumed, 2)
        }