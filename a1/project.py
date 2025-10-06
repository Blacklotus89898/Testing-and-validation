import requests
import xml.etree.ElementTree as ET
import random

class APIClient:
    def __init__(self, base_url, timeout=5):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def request(self, method, endpoint, data=None, accept="json"):
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
                headers["Accept"] = "application/xml"
                headers["Content-Type"] = "application/xml"
                xml_data = None
                if data:
                    xml_data = (
                        "<root>"
                        + "".join([f"<{k}>{v}</{k}>" for k, v in data.items()])
                        + "</root>"
                    )
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
        if response is None:
            print("❌ No response received")
            return False
        if response.status_code == expected_status:
            print(f"✅ {response.request.method} {response.url} -> {response.status_code}")
            return True
        else:
            print(f"❌ {response.request.method} {response.url} -> Expected {expected_status}, got {response.status_code}")
            return False
        


def contains_expected_subset(expected, actual):
    """
    Recursively check if expected dict is present anywhere inside actual dict/list.
    """
    if isinstance(actual, dict):
        # If actual is a dict, first check direct match
        if all(k in actual and actual[k] == v for k, v in expected.items()):
            return True
        # Otherwise, check all values recursively
        return any(contains_expected_subset(expected, v) for v in actual.values())
    elif isinstance(actual, list):
        # Check each item in the list
        return any(contains_expected_subset(expected, item) for item in actual)
    return False

    
def xml_contains_subset(expected, actual):
    """
    Check if expected dict (tag->text) exists anywhere in the XML tree `actual` (an Element).
    """
    if not isinstance(actual, ET.Element):
        return False

    # Direct match: all keys in expected match the child tags/text
    if all(actual.find(k) is not None and actual.find(k).text == str(v) for k, v in expected.items()):
        return True

    # Otherwise, check recursively in children
    return any(xml_contains_subset(expected, child) for child in actual)

# ---------------- Example usage ----------------
if __name__ == "__main__":
    client = APIClient("http://localhost:4567")

    try:
        health_resp = requests.get(client.base_url, timeout=3)
        health_resp.raise_for_status()
    except requests.RequestException:
        print(f"❌ Server at {client.base_url} is not reachable. Skipping tests.")
        exit(0)  # stop the script

    # Example data
    example_project_data = {"title": "New Project", "description": "Example project description"}
    example_project_data2 = {"title": "New Project2", "description": "Example project description"}
    example_project_data3 = {"title": "New Project3", "description": "Example project description"}
    example_category_data = {"title": "Work", "description": "Tasks related to office work"}
    example_todo_data = {"title": "Title",	"doneStatus": False, "description": "desciption"}

    # ---- Populate db for test: Setup function

    # Add task to project (assuming project ID = 1)
    resp, task_body = client.request("POST", "/todos", data=example_todo_data)
    # Create category
    resp, category_body = client.request("POST", "/categories", data=example_category_data)
    
    # Create project
    resp, project_body = client.request("POST", "/projects", data=example_project_data)
    resp, project_body = client.request("POST", "/projects", data=example_project_data2)
    resp, project_body = client.request("POST", "/projects", data=example_project_data3) # Third project in db
    resp, project_body = client.request("POST", "/projects/2/categories", data={"id": "2"}) # Cat 2 with proj 2
    
    
    # ----  Define the endpoints and methods to test
    endpoints_to_test = [
        # /projects
        {"endpoint": "/projects", "method": "GET", "data": None, "accept": "json", "expected": 200, "expected_body": example_project_data},
        {"endpoint": "/projects", "method": "GET", "data": None, "accept": "xml", "expected": 200, "expected_body": example_project_data},
        {"endpoint": "/projects", "method": "POST", "data": example_project_data, "accept": "json", "expected": 201},
        {"endpoint": "/projects", "method": "PUT", "data": example_project_data, "accept": "json", "expected": 405},
        {"endpoint": "/projects", "method": "DELETE", "data": None, "accept": "json", "expected": 405},
        {"endpoint": "/projects", "method": "OPTIONS", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects", "method": "HEAD", "data": None, "accept": "json", "expected": 200},

        # /projects/{id}
        {"endpoint": "/projects/2", "method": "GET", "data": None, "accept": "json", "expected": 200, "expected_body": example_project_data},
        {"endpoint": "/projects/2", "method": "GET", "data": None, "accept": "xml", "expected": 200, "expected_body": example_project_data},
        {"endpoint": "/projects/1", "method": "POST", "data": {"description": "Updated"}, "accept": "json", "expected": 200},
        {"endpoint": "/projects/4", "method": "PUT", "data": example_project_data, "accept": "json", "expected": 200}, # prevent destroying default project 
        {"endpoint": "/projects/3", "method": "DELETE", "data": None, "accept": "json", "expected": 200}, # must create an id 2 project in setup
        {"endpoint": "/projects/1", "method": "PATCH", "data": {"title": "Patched"}, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1", "method": "OPTIONS", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1", "method": "HEAD", "data": None, "accept": "json", "expected": 200},

        # /projects/{id}/tasks
        {"endpoint": "/projects/1/tasks", "method": "GET", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1/tasks", "method": "POST", "data": example_todo_data, "accept": "json", "expected": 201},
        {"endpoint": "/projects/1/tasks", "method": "DELETE", "data": None, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/tasks", "method": "PATCH", "data": None, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/tasks", "method": "PUT", "data": example_todo_data, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/tasks", "method": "OPTIONS", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1/tasks", "method": "HEAD", "data": None, "accept": "json", "expected": 200},
        
        # TODO: need work below
        # /projects/{id}/tasks/{todoId}
        {"endpoint": "/projects/1/tasks/1", "method": "GET", "data": None, "accept": "json", "expected": 404},
        {"endpoint": "/projects/1/tasks/1", "method": "POST", "data": example_todo_data, "accept": "json", "expected": 404},
        {"endpoint": "/projects/1/tasks/1", "method": "DELETE", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1/tasks/1", "method": "PATCH", "data": None, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/tasks/1", "method": "PUT", "data": example_todo_data, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/tasks/1", "method": "OPTIONS", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1/tasks/1", "method": "HEAD", "data": None, "accept": "json", "expected": 404},

        # /projects/{id}/categories
        {"endpoint": "/projects/1/categories", "method": "GET", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1/categories", "method": "POST", "data": {"id": "1"}, "accept": "json","expected": 201},
        {"endpoint": "/projects/1/categories", "method": "PUT", "data": {"id": "1"}, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/categories", "method": "DELETE", "data": None, "accept": "json", "expected": 405}, # REMOVES LINK ASSOCIATION
        {"endpoint": "/projects/1/categories", "method": "PATCH", "data": None, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/categories", "method": "OPTIONS", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1/categories", "method": "HEAD", "data": None, "accept": "json", "expected": 200},
    
        # TODO: need work below
        # /projects/{id}/categories/{catId}
        {"endpoint": "/projects/1/categories/1", "method": "GET", "data": None, "accept": "json", "expected": 404},
        {"endpoint": "/projects/1/categories/1", "method": "POST", "data": {"id": "1"}, "accept": "json","expected": 404},
        {"endpoint": "/projects/1/categories/1", "method": "PUT", "data": {"id": "1"}, "accept": "json", "expected": 405},
        {"endpoint": "/projects/2/categories/2", "method": "DELETE", "data": None, "accept": "json", "expected": 200}, # REMOVES LINK ASSOCIATION
        {"endpoint": "/projects/1/categories/1", "method": "PATCH", "data": None, "accept": "json", "expected": 405},
        {"endpoint": "/projects/1/categories/1", "method": "OPTIONS", "data": None, "accept": "json", "expected": 200},
        {"endpoint": "/projects/1/categories/1", "method": "HEAD", "data": None, "accept": "json", "expected": 404},
    ]

    # Run all tests in random order
    name = ""
    random.shuffle(endpoints_to_test)  # shuffle the list in-place

    for ep in endpoints_to_test:
        
        expected_body = None

        curr = ep["endpoint"]
        if curr != name:
            print(f"\nTesting endpoint: {curr}")
            name = curr
        resp, _ = client.request(
            ep["method"], ep["endpoint"], data=ep.get("data"), accept=ep.get("accept", "json")
        )
        client.assert_response(resp, ep.get("expected", 200))

        expected_body = ep.get("expected_body")
        if expected_body is not None:
            actual_body = None
            if ep.get("accept") == "json":
                actual_body = resp.json() if resp.text.strip() else None
                if not contains_expected_subset(expected_body, actual_body):
                    print(f"❌ JSON body mismatch.\nExpected subset: {expected_body}\nGot: {actual_body}")
                else:
                    print(f"✅ JSON body contains expected data")
            elif ep.get("accept") == "xml":
                actual_body = ET.fromstring(resp.text) if resp.text.strip() else None
                if not xml_contains_subset(expected_body, actual_body):
                    print(f"❌ XML body mismatch.\nExpected subset: {expected_body}\nGot: {ET.tostring(actual_body, encoding='unicode')}")
                else:
                    print(f"✅ XML body contains expected data")



            # TODO: add a clean up function
