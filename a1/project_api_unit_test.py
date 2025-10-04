import unittest
import requests
import sys

BASE_URL = "http://localhost:4567"  # change this to your API base URL


class TestProjectsAPI(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        """Check if the backend server is running before any tests."""
        print(f"🔍 Checking if backend is running at {BASE_URL} ...")
        try:
            resp = requests.get(BASE_URL, timeout=3)
            if resp.status_code >= 500:
                print(f"⚠️ Server responded with error status {resp.status_code}")
                sys.exit(1)
            else:
                print(f"✅ Server is reachable (status {resp.status_code})\n")
        except requests.ConnectionError:
            print(f"❌ Cannot connect to backend at {BASE_URL}. Is it running?")
            sys.exit(1)

    def setUp(self):
        # Example data for POST requests
        self.project_data = {"name": "Test Project", "description": "Unit test project"}
        self.task_data = {"title": "Test Task", "status": "pending"}
        self.category_data = {"name": "Urgent"}

    # ---------- /projects ----------
    def test_get_projects(self):
        """GET /projects"""
        r = requests.get(f"{BASE_URL}/projects")
        self.assertEqual(r.status_code, 200)

    def test_post_projects(self):
        """POST /projects"""
        r = requests.post(f"{BASE_URL}/projects", json=self.project_data)
        self.assertIn(r.status_code, [200, 201])
        self.project_id = r.json().get("id", None)

    # ---------- /projects/{id} ----------
    def test_get_project_by_id(self):
        """GET /projects/{id}"""
        pid = 1  # replace with valid id if necessary
        r = requests.get(f"{BASE_URL}/projects/{pid}")
        self.assertIn(r.status_code, [200, 404])

    def test_put_project_by_id(self):
        """PUT /projects/{id}"""
        pid = 1
        update = {"name": "Updated Project"}
        r = requests.put(f"{BASE_URL}/projects/{pid}", json=update)
        self.assertIn(r.status_code, [200, 204, 404])

    def test_delete_project_by_id(self):
        """DELETE /projects/{id}"""
        pid = 1
        r = requests.delete(f"{BASE_URL}/projects/{pid}")
        self.assertIn(r.status_code, [200, 204, 404])

    # ---------- /projects/{id}/tasks ----------
    def test_get_project_tasks(self):
        """GET /projects/{id}/tasks"""
        pid = 1
        r = requests.get(f"{BASE_URL}/projects/{pid}/tasks")
        self.assertIn(r.status_code, [200, 404])

    def test_post_project_task(self):
        """POST /projects/{id}/tasks"""
        pid = 1
        r = requests.post(f"{BASE_URL}/projects/{pid}/tasks", json=self.task_data)
        self.assertIn(r.status_code, [200, 201, 404])

    # ---------- /projects/{id}/tasks/{todoId} ----------
    def test_delete_project_task(self):
        """DELETE /projects/{id}/tasks/{todoId}"""
        pid = 1
        tid = 1  # replace with valid task ID
        r = requests.delete(f"{BASE_URL}/projects/{pid}/tasks/{tid}")
        self.assertIn(r.status_code, [200, 204, 404])

    # ---------- /projects/{id}/categories ----------
    def test_get_project_categories(self):
        """GET /projects/{id}/categories"""
        pid = 1
        r = requests.get(f"{BASE_URL}/projects/{pid}/categories")
        self.assertIn(r.status_code, [200, 404])

    def test_post_project_category(self):
        """POST /projects/{id}/categories"""
        pid = 1
        r = requests.post(f"{BASE_URL}/projects/{pid}/categories", json=self.category_data)
        self.assertIn(r.status_code, [200, 201, 404])

    # ---------- /projects/{id}/categories/{catId} ----------
    def test_delete_project_category(self):
        """DELETE /projects/{id}/categories/{catId}"""
        pid = 1
        cid = 1
        r = requests.delete(f"{BASE_URL}/projects/{pid}/categories/{cid}")
        self.assertIn(r.status_code, [200, 204, 404])


if __name__ == "__main__":
    unittest.main(verbosity=2)
