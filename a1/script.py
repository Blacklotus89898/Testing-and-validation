import requests
import json
import random

BASE_URL = "http://localhost:4567"  # Change if needed

def pretty_print(resp):
    """Utility to print request + response details nicely"""
    print(f"\n[{resp.request.method}] {resp.request.url}")
    print("Status:", resp.status_code)
    try:
        print(json.dumps(resp.json(), indent=2))
    except Exception:
        print(resp.text)

def random_suffix():
    return str(random.randint(1000, 9999))


# -----------------------------
# TODOS TESTING
# -----------------------------
def test_todos():
    print("\n========== TODOS TEST ==========")

    # 1. Create a todo
    todo_data = {
        "title": f"Test Todo {random_suffix()}",
        "description": "Testing create flow",
        "done": False
    }
    r = requests.post(f"{BASE_URL}/todos", json=todo_data)
    pretty_print(r)
    todo_id = r.json().get("id")

    # 2. Get all todos
    r = requests.get(f"{BASE_URL}/todos")
    pretty_print(r)

    # 3. Get single todo
    r = requests.get(f"{BASE_URL}/todos/{todo_id}")
    pretty_print(r)

    # 4. Update todo
    update_data = {"title": "Updated Title", "done": True}
    r = requests.put(f"{BASE_URL}/todos/{todo_id}", json=update_data)
    pretty_print(r)

    # 5. Edge case: Get todo with invalid ID
    r = requests.get(f"{BASE_URL}/todos/999999")
    pretty_print(r)

    # Return ID so it can be linked to a project
    return todo_id


# -----------------------------
# PROJECTS TESTING
# -----------------------------
def test_projects(todo_id):
    print("\n========== PROJECTS TEST ==========")

    # 1. Create a project
    project_data = {"name": f"Test Project {random_suffix()}"}
    r = requests.post(f"{BASE_URL}/projects", json=project_data)
    pretty_print(r)
    project_id = r.json().get("id")

    # 2. Get all projects
    r = requests.get(f"{BASE_URL}/projects")
    pretty_print(r)

    # 3. Get single project
    r = requests.get(f"{BASE_URL}/projects/{project_id}")
    pretty_print(r)

    # 4. Update project
    update_data = {"name": "Updated Project Name"}
    r = requests.put(f"{BASE_URL}/projects/{project_id}", json=update_data)
    pretty_print(r)

    # 5. Assign todo to project (if API supports /projects/:id/todos or similar)
    try:
        r = requests.post(f"{BASE_URL}/projects/{project_id}/todos", json={"todo_id": todo_id})
        pretty_print(r)
    except Exception as e:
        print("Assigning todo to project not supported by API:", e)

    # 6. Get project again (check if todo linked)
    r = requests.get(f"{BASE_URL}/projects/{project_id}")
    pretty_print(r)

    # 7. Edge case: Delete invalid project
    r = requests.delete(f"{BASE_URL}/projects/999999")
    pretty_print(r)

    # 8. Delete project
    r = requests.delete(f"{BASE_URL}/projects/{project_id}")
    pretty_print(r)

    return project_id


# -----------------------------
# CLEANUP
# -----------------------------
def cleanup(todo_id):
    """Make sure test todo is removed"""
    r = requests.delete(f"{BASE_URL}/todos/{todo_id}")
    pretty_print(r)


# -----------------------------
# MAIN
# -----------------------------
if __name__ == "__main__":
    todo_id = test_todos()
    project_id = test_projects(todo_id)
    cleanup(todo_id)
