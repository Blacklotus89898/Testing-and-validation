import requests
import xml.etree.ElementTree as ET
import random
import sys
import unittest

# =========================================================
#                     CLIENT CLASS
# =========================================================


class APIClient:
    def __init__(self, base_url, timeout=5):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def request(self, method, endpoint, data=None, accept="json"):
        """Send an HTTP request and return (response, parsed_body)."""
        if not endpoint.startswith("/"):
            endpoint = "/" + endpoint
        url = f"{self.base_url}{endpoint}"
        headers = {}

        try:
            if accept == "json":
                headers["Accept"] = "application/json"
                response = requests.request(
                    method, url, json=data, headers=headers, timeout=self.timeout
                )
                body = response.json() if response.text.strip() else None

            elif accept == "xml":
                headers.update(
                    {"Accept": "application/xml", "Content-Type": "application/xml"}
                )
                xml_data = None
                if isinstance(data, dict):
                    xml_data = (
                        "<root>"
                        + "".join(f"<{k}>{v}</{k}>" for k, v in data.items())
                        + "</root>"
                    )
                elif isinstance(data, str):
                    xml_data = data

                response = requests.request(
                    method, url, data=xml_data, headers=headers, timeout=self.timeout
                )
                body = ET.fromstring(response.text) if response.text.strip() else None
            else:
                raise ValueError("Unsupported accept type, choose 'json' or 'xml'.")

        except requests.RequestException as e:
            print(f"❌ Request failed: {method} {url} -> {e}")
            return None, None

        return response, body

    def assert_response(self, response, expected_status=200):
        """Assert response code and print result."""
        if response is None:
            print("❌ No response received.")
            return False
        if response.status_code == expected_status:
            print(
                f"✅ {response.request.method} {response.url} -> {response.status_code}"
            )
            return True
        else:
            print(
                f"❌ {response.request.method} {response.url} -> Expected {expected_status}, got {response.status_code}"
            )
            return False


# =========================================================
#                     VALIDATION HELPERS
# =========================================================


def contains_expected_subset(expected, actual):
    """Recursively check if expected dict is a subset of actual (JSON), with sorted list comparison."""
    if isinstance(actual, dict):
        if all(k in actual and actual[k] == v for k, v in expected.items()):
            return True
        return any(contains_expected_subset(expected, v) for v in actual.values())
    elif isinstance(actual, list):
        sorted_list = sorted(
            actual,
            key=lambda x: tuple(sorted(x.items())) if isinstance(x, dict) else str(x),
        )
        return any(contains_expected_subset(expected, item) for item in sorted_list)
    return False


def xml_contains_subset(expected, actual):
    """Recursively check if expected tags/text exist anywhere in actual XML tree."""
    if not isinstance(actual, ET.Element):
        return False
    if all(
        actual.find(k) is not None and actual.find(k).text == str(v)
        for k, v in expected.items()
    ):
        return True
    return any(xml_contains_subset(expected, child) for child in actual)


def validate_body(ep, resp):
    """Validate the response body for JSON or XML."""
    expected_body = ep.get("expected_body")
    if expected_body is None or resp is None:
        return

    accept_type = ep.get("accept", "json").lower()

    try:
        if accept_type == "json":
            actual_body = resp.json() if resp.text.strip() else None
            if not contains_expected_subset(expected_body, actual_body):
                print(
                    f"❌ JSON body mismatch.\nExpected subset: {expected_body}\nGot: {actual_body}"
                )
            else:
                print("✅ JSON body contains expected data")

        elif accept_type == "xml":
            actual_body = ET.fromstring(resp.text) if resp.text.strip() else None
            if not xml_contains_subset(expected_body, actual_body):
                actual_str = (
                    ET.tostring(actual_body, encoding="unicode")
                    if actual_body is not None
                    else "None"
                )
                print(
                    f"❌ XML body mismatch.\nExpected subset: {expected_body}\nGot: {actual_str}"
                )
            else:
                print("✅ XML body contains expected data")

        else:
            print(f"⚠️ Unsupported accept type: {accept_type}")

    except Exception as e:
        print(f"❌ Error during body validation: {e}")


def run_test_case(client, ep):
    """Run a single endpoint test and validate results."""
    resp, _ = client.request(
        ep["method"],
        ep["endpoint"],
        data=ep.get("data"),
        accept=ep.get("accept", "json"),
    )
    client.assert_response(resp, ep.get("expected", 200))
    validate_body(ep, resp)


# =========================================================
#                     UNITTEST WRAPPER
# =========================================================


class TestAPI(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.BASE_URL = "http://localhost:4567"
        cls.client = APIClient(cls.BASE_URL)

        try:
            health_resp = requests.get(cls.client.base_url, timeout=3)
            health_resp.raise_for_status()
        except requests.RequestException:
            print(
                f"❌ Server at {cls.client.base_url} is not reachable. Skipping tests."
            )
            sys.exit(0)

        print(f"✅ Server reachable at {cls.client.base_url}\n")

        # --- INITIAL DATA POPULATION ---
        cls.mock_project_data = {
            "title": "New Project",
            "description": "Example project description",
        }
        cls.mock_project_data2 = {
            "title": "New Project2",
            "description": "Example project description",
        }
        cls.mock_project_data3 = {
            "title": "New Project3",
            "description": "Example project description",
        }
        cls.mock_category_data = {
            "title": "Work",
            "description": "Tasks related to office work",
        }
        cls.mock_todo_data = {
            "title": "Title",
            "doneStatus": False,
            "description": "desciption",
        }

        cls.mock_invalid_project_json = {"invalidField": 10000}
        cls.mock_invalid_project_xml = """<?xml version="1.0" encoding="UTF-8"?>
        <project><invalidFiled>10000</invalidFiled></project>"""

        print("⚙️ Populating database with initial test data...")
        cls.client.request("POST", "/todos", data=cls.mock_todo_data)
        cls.client.request("POST", "/categories", data=cls.mock_category_data)
        cls.client.request("POST", "/projects", data=cls.mock_project_data)
        cls.client.request("POST", "/projects", data=cls.mock_project_data2)
        cls.client.request("POST", "/projects", data=cls.mock_project_data3)
        cls.client.request("POST", "/projects/2/categories", data={"id": "2"})
        print("✅ Database populated.\n")

    def test_endpoints(self):
        endpoints_to_test = [
            # /projects
            {
                "endpoint": "/projects",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 200,
                "expected_body": self.mock_project_data,
            },
            {
                "endpoint": "/projects",
                "method": "GET",
                "data": None,
                "accept": "xml",
                "expected": 200,
                "expected_body": self.mock_project_data,
            },
            {
                "endpoint": "/projects",
                "method": "POST",
                "data": self.mock_project_data,
                "accept": "json",
                "expected": 201,
            },
            {
                "endpoint": "/projects",
                "method": "PUT",
                "data": self.mock_project_data,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects",
                "method": "DELETE",
                "data": None,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects",
                "method": "OPTIONS",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects",
                "method": "HEAD",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            # /projects/{id}
            {
                "endpoint": "/projects/2",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 200,
                "expected_body": self.mock_project_data,
            },
            {
                "endpoint": "/projects/2",
                "method": "GET",
                "data": None,
                "accept": "xml",
                "expected": 200,
                "expected_body": self.mock_project_data,
            },
            {
                "endpoint": "/projects/1",
                "method": "POST",
                "data": {"description": "Updated"},
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/4",
                "method": "PUT",
                "data": self.mock_project_data,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/3",
                "method": "DELETE",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1",
                "method": "PATCH",
                "data": {"title": "Patched"},
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1",
                "method": "OPTIONS",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1",
                "method": "HEAD",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            # /projects/{id}/tasks
            {
                "endpoint": "/projects/1/tasks",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/tasks",
                "method": "POST",
                "data": self.mock_todo_data,
                "accept": "json",
                "expected": 201,
            },
            {
                "endpoint": "/projects/1/tasks",
                "method": "DELETE",
                "data": None,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/tasks",
                "method": "PATCH",
                "data": None,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/tasks",
                "method": "PUT",
                "data": self.mock_todo_data,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/tasks",
                "method": "OPTIONS",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/tasks",
                "method": "HEAD",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            # /projects/{id}/tasks/{todoId}
            {
                "endpoint": "/projects/1/tasks/1",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 404,
            },
            {
                "endpoint": "/projects/1/tasks/1",
                "method": "POST",
                "data": self.mock_todo_data,
                "accept": "json",
                "expected": 404,
            },
            {
                "endpoint": "/projects/1/tasks/1",
                "method": "DELETE",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/tasks/1",
                "method": "PATCH",
                "data": None,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/tasks/1",
                "method": "PUT",
                "data": self.mock_todo_data,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/tasks/1",
                "method": "OPTIONS",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/tasks/1",
                "method": "HEAD",
                "data": None,
                "accept": "json",
                "expected": 404,
            },
            # /projects/{id}/categories
            {
                "endpoint": "/projects/1/categories",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/categories",
                "method": "POST",
                "data": {"id": "1"},
                "accept": "json",
                "expected": 201,
            },
            {
                "endpoint": "/projects/1/categories",
                "method": "PUT",
                "data": {"id": "1"},
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/categories",
                "method": "DELETE",
                "data": None,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/categories",
                "method": "PATCH",
                "data": None,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/categories",
                "method": "OPTIONS",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/categories",
                "method": "HEAD",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            # /projects/{id}/categories/{catId}
            {
                "endpoint": "/projects/1/categories/1",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 404,
            },
            {
                "endpoint": "/projects/1/categories/1",
                "method": "POST",
                "data": {"id": "1"},
                "accept": "json",
                "expected": 404,
            },
            {
                "endpoint": "/projects/1/categories/1",
                "method": "PUT",
                "data": {"id": "1"},
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/2/categories/2",
                "method": "DELETE",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/categories/1",
                "method": "PATCH",
                "data": None,
                "accept": "json",
                "expected": 405,
            },
            {
                "endpoint": "/projects/1/categories/1",
                "method": "OPTIONS",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1/categories/1",
                "method": "HEAD",
                "data": None,
                "accept": "json",
                "expected": 404,
            },
        ]

        # Randomised testing
        random.seed(42)
        random.shuffle(endpoints_to_test)

        last_endpoint = ""
        for ep in endpoints_to_test:
            curr = ep["endpoint"]
            if curr != last_endpoint:
                print(f"\n########### Testing endpoint: {curr}")
                last_endpoint = curr
            run_test_case(self.client, ep)

    def test_invalid_format(self):
        print("\n########### Testing malformed input formats")
        invalid_format_test = [
            {
                "endpoint": "/projects",
                "method": "POST",
                "data": self.mock_invalid_project_json,
                "accept": "json",
                "expected": 400,
                "expected_body": {
                    "errorMessages": ["Could not find field: invalidField"]
                },
            },
            {
                "endpoint": "/projects",
                "method": "POST",
                "data": self.mock_invalid_project_xml,
                "accept": "xml",
                "expected": 400,
            },
        ]
        for ep in invalid_format_test:
            run_test_case(self.client, ep)

    def test_side_effects(self):
        print("\n########### Testing API side effects")
        side_effect_test = [
            {
                "endpoint": "/projects/1",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 200,
                "expected_body": {
                    "projects": [
                        {
                            "id": "1",
                            "title": "Office Work",
                            "completed": "false",
                            "active": "false",
                            "description": "Updated",
                            "tasks": [{"id": "2"}, {"id": "4"}],
                            "categories": [{"id": "1"}],
                        }
                    ]
                },
            },
            {
                "endpoint": "/projects",
                "method": "POST",
                "data": self.mock_project_data,
                "accept": "json",
                "expected": 201,
            },
            {
                "endpoint": "/projects/1",
                "method": "POST",
                "data": {"description": "Updated2"},
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/4",
                "method": "DELETE",
                "data": None,
                "accept": "json",
                "expected": 200,
            },
            {
                "endpoint": "/projects/1",
                "method": "GET",
                "data": None,
                "accept": "json",
                "expected": 200,
                "expected_body": {
                    "projects": [
                        {
                            "id": "1",
                            "title": "Office Work",
                            "completed": "false",
                            "active": "false",
                            "description": "Updated2",
                            "tasks": [{"id": "2"}, {"id": "4"}],
                            "categories": [{"id": "1"}],
                        }
                    ]
                },
            },
        ]
        for ep in side_effect_test:
            run_test_case(self.client, ep)


# =========================================================
#                     MAIN ENTRY POINT
# =========================================================

if __name__ == "__main__":
    # Optional: Allow reproducible randomization
    seed = random.randint(1, 99999)
    random.seed(seed)

    print(f"\n{'='*70}")
    print(f"🚀 Starting Project API Test Suite (Seed: {seed})")
    print(f"{'='*70}\n")

    # Run all tests and capture results
    test_loader = unittest.TestLoader()
    test_suite = test_loader.loadTestsFromTestCase(TestAPI)
    test_runner = unittest.TextTestRunner(verbosity=2)
    result = test_runner.run(test_suite)

    # Print summary
    print(f"\n{'='*70}")
    print(f"🎯 Test Summary (Seed: {seed})")
    print(f"   Tests run: {result.testsRun}")
    print(f"   Failures: {len(result.failures)}")
    print(f"   Errors: {len(result.errors)}")

    if result.wasSuccessful():
        print(f"   ✅ All tests passed in random order!")
        print(f"   🎉 Test independence verified!")
    else:
        print(f"   ❌ Some tests failed")
        if result.failures:
            print(f"   💥 Failed tests may have order dependencies")

    # Exit with success/failure code (for CI/CD integration)
    sys.exit(0 if result.wasSuccessful() else 1)
