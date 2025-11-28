import random
import string
from typing import Dict, Any
from tester.BasePerformanceTester import BasePerformanceTester


# --- 4. PROJECT TESTER (Child Class) ---
class ProjectTester(BasePerformanceTester):
    def __init__(self, base_url: str):
        super().__init__(base_url)
        self.endpoint = "projects"  # Targets /projects

    def _generate_payload(self) -> Dict[str, Any]:
        # Customize this schema to match your Project API requirements
        return {
            "title": "Proj-" + "".join(random.choices(string.ascii_letters, k=8)),
            "completed": random.choice([True, False]),
            "active": True,
            "description": "Project test description",
        }
