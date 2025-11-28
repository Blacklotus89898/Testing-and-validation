import random
import string
from typing import Dict, Any
from tester.BasePerformanceTester import BasePerformanceTester


# --- 3. TODO TESTER (Child Class) ---
class TodoTester(BasePerformanceTester):
    def __init__(self, base_url: str):
        super().__init__(base_url)
        self.endpoint = "todos"  # Targets /todos

    def _generate_payload(self) -> Dict[str, Any]:
        return {
            "title": "".join(random.choices(string.ascii_letters, k=10)),
            "description": "".join(random.choices(string.ascii_lowercase, k=20)),
            "doneStatus": random.choice([True, False]),
        }
