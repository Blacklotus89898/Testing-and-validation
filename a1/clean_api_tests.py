import unittest
import requests
import sys
import uuid
from typing import Optional, Dict, Any, List
import json
import subprocess
import time
import os
import signal
import psutil

try:
    from colorama import init, Fore, Style, Back
    init(autoreset=True)  # Initialize colorama for Windows
    COLORS_AVAILABLE = True
except ImportError:
    # Fallback if colorama is not installed
    class Fore:
        RED = GREEN = YELLOW = BLUE = CYAN = MAGENTA = WHITE = RESET = ''
    class Style:
        BRIGHT = DIM = RESET_ALL = ''
    class Back:
        RED = GREEN = YELLOW = BLUE = CYAN = MAGENTA = WHITE = RESET = ''
    COLORS_AVAILABLE = False

BASE_URL = "http://localhost:4567"
SERVER_JAR = "runTodoManagerRestAPI-1.5.5.jar"
SERVER_PORT = 4567


class APITestBase(unittest.TestCase):
    """Base class with utilities for API testing that ensures test independence"""
    
    server_process = None
    
    @classmethod
    def setUpClass(cls):
        """Initialize the test framework - server will be started per test"""
        print(f"\n{Style.BRIGHT}{Back.BLUE} API UNIT TESTS - SERVER RESTART MODE {Style.RESET_ALL}")
        print(f"{Style.BRIGHT}{'='*70}{Style.RESET_ALL}")
        print(f"{Fore.CYAN}Server will be restarted between each test for complete isolation{Style.RESET_ALL}")
        print(f"{Style.BRIGHT}{'='*70}{Style.RESET_ALL}\n")
        
        # Check if jar file exists
        if not os.path.exists(SERVER_JAR):
            print(f"{Fore.RED}Error: {SERVER_JAR} not found in current directory{Style.RESET_ALL}")
            print(f"{Fore.YELLOW}   Please make sure the server jar file is present{Style.RESET_ALL}")
            sys.exit(1)
    
    @classmethod
    def tearDownClass(cls):
        """Clean shutdown of any remaining server processes"""
        print(f"\n{Style.BRIGHT}{'='*70}{Style.RESET_ALL}")
        print(f"{Fore.CYAN}Final cleanup - ensuring no server processes remain...{Style.RESET_ALL}")
        
        try:
            cls._stop_server()
        except Exception as e:
            print(f"{Fore.YELLOW}⚠️  Error during final server cleanup: {e}{Style.RESET_ALL}")
        
        # Force cleanup any remaining processes on the port
        cls._emergency_cleanup()
        
        print(f"{Fore.GREEN}✅ All tests completed. All server processes stopped.{Style.RESET_ALL}")
        print(f"{Style.BRIGHT}{'='*70}{Style.RESET_ALL}")
    
    @classmethod
    def _start_server(cls):
        """Start the Todo Manager server"""
        print(f"   {Fore.CYAN}🚀 Starting fresh server instance...{Style.RESET_ALL}")
        
        try:
            # Ensure any previous server instance is completely dead
            cls._emergency_cleanup()
            time.sleep(2)  # Wait longer for complete cleanup
            
            # Check for and remove any potential data persistence files
            cls._clean_data_files()
            
            # Start server process with clean environment
            cls.server_process = subprocess.Popen(
                ["java", "-jar", SERVER_JAR],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                creationflags=subprocess.CREATE_NEW_PROCESS_GROUP if os.name == 'nt' else 0,
                cwd=os.getcwd()  # Ensure consistent working directory
            )
            
            # Wait for server to be ready and verify clean state
            max_attempts = 40  # 20 seconds max - increased for reliability
            clean_state_verified = False
            
            for attempt in range(max_attempts):
                try:
                    resp = requests.get(BASE_URL + "/todos", timeout=3)
                    if resp.status_code == 200:  # Server is responding and endpoint works
                        # Verify we have the expected default state (2 todos)
                        data = resp.json()
                        todos = data.get("todos", [])
                        
                        if len(todos) == 2:  # Expected default state
                            # Double-check the default todos are the right ones
                            todo_ids = [todo.get("id") for todo in todos]
                            if "1" in todo_ids and "2" in todo_ids:
                                print(f"   {Fore.GREEN}✅ Server ready with CLEAN state ({len(todos)} default todos: {todo_ids}) - took {attempt * 0.5:.1f}s{Style.RESET_ALL}")
                                clean_state_verified = True
                                return True
                            else:
                                print(f"   {Fore.RED}❌ Wrong default todo IDs: {todo_ids} (expected: ['1', '2']){Style.RESET_ALL}")
                        else:
                            print(f"   {Fore.RED}❌ DIRTY STATE DETECTED: {len(todos)} todos (expected 2) - SERVER RESTART FAILED{Style.RESET_ALL}")
                            for i, todo in enumerate(todos):
                                print(f"      Todo {i+1}: ID={todo.get('id')}, Title='{todo.get('title')}'")
                            # Continue waiting in case server is still initializing
                except requests.ConnectionError:
                    pass  # Server not ready yet
                except requests.Timeout:
                    pass  # Server not ready yet
                except Exception as e:
                    print(f"   {Fore.YELLOW}⚠️  Server check error: {e}{Style.RESET_ALL}")
                
                time.sleep(0.5)
            
            if not clean_state_verified:
                print(f"   {Fore.RED}❌ SERVER FAILED TO START WITH CLEAN STATE{Style.RESET_ALL}")
                print(f"   {Fore.RED}🚨 This explains why test 1 fails when test 3 runs first{Style.RESET_ALL}")
            
            print(f"   {Fore.RED}❌ Server failed to start within 15 seconds{Style.RESET_ALL}")
            cls._stop_server()
            return False
            
        except Exception as e:
            print(f"   {Fore.RED}❌ Failed to start server: {e}{Style.RESET_ALL}")
            return False
    
    @classmethod
    def _stop_server(cls):
        """Stop the Todo Manager server - AGGRESSIVELY"""
        if cls.server_process:
            try:
                print(f"   {Fore.CYAN}🛑 Stopping server (PID: {cls.server_process.pid})...{Style.RESET_ALL}")
                
                # Skip graceful shutdown - go straight to force kill for reliable cleanup
                print(f"   {Fore.YELLOW}⚡ Force killing server process...{Style.RESET_ALL}")
                cls.server_process.kill()
                
                # Wait for process to actually die
                try:
                    cls.server_process.wait(timeout=5)
                    print(f"   {Fore.GREEN}✅ Server process killed{Style.RESET_ALL}")
                except subprocess.TimeoutExpired:
                    print(f"   {Fore.RED}❌ Server process refused to die{Style.RESET_ALL}")
                
            except Exception as e:
                print(f"   {Fore.YELLOW}⚠️  Error killing server: {e}{Style.RESET_ALL}")
            
            finally:
                cls.server_process = None
        
        # Always do emergency cleanup to be absolutely sure
        print(f"   {Fore.CYAN}🔥 Emergency cleanup to ensure no server remains...{Style.RESET_ALL}")
        cls._emergency_cleanup()
        
        # Wait longer for complete cleanup and port release
        print(f"   {Fore.CYAN}⏳ Waiting for complete server shutdown...{Style.RESET_ALL}")
        time.sleep(3)  # Increased wait time
        
        # Verify server is actually gone
        cls._verify_server_stopped()
    
    @classmethod
    def _kill_processes_on_port(cls, port):
        """Kill any processes using the specified port"""
        killed_count = 0
        try:
            for proc in psutil.process_iter(['pid', 'name', 'connections']):
                try:
                    connections = proc.info['connections']
                    if connections:
                        for conn in connections:
                            if hasattr(conn, 'laddr') and conn.laddr and conn.laddr.port == port:
                                print(f"   {Fore.YELLOW}🔪 Killing process {proc.info['pid']} ({proc.info['name']}) on port {port}{Style.RESET_ALL}")
                                psutil.Process(proc.info['pid']).terminate()
                                killed_count += 1
                                break
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess, AttributeError):
                    pass
        except Exception as e:
            print(f"   {Fore.YELLOW}⚠️  Could not check for processes on port {port}: {e}{Style.RESET_ALL}")
        
        if killed_count > 0:
            print(f"   {Fore.GREEN}✅ Killed {killed_count} processes on port {port}{Style.RESET_ALL}")
        return killed_count
    
    @classmethod
    def _emergency_cleanup(cls):
        """Emergency cleanup - kill all java processes running the todo server"""
        print(f"   {Fore.CYAN}🚨 Emergency cleanup - finding all Todo Manager processes...{Style.RESET_ALL}")
        
        killed_count = 0
        try:
            for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
                try:
                    if proc.info['name'] and 'java' in proc.info['name'].lower():
                        cmdline = proc.info['cmdline']
                        if cmdline and any(SERVER_JAR in arg for arg in cmdline):
                            print(f"   {Fore.YELLOW}🔪 Emergency kill: Todo Manager process {proc.info['pid']}{Style.RESET_ALL}")
                            psutil.Process(proc.info['pid']).kill()
                            killed_count += 1
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess, AttributeError):
                    pass
        except Exception as e:
            print(f"   {Fore.YELLOW}⚠️  Emergency cleanup error: {e}{Style.RESET_ALL}")
        
        # Also clean up by port
        port_killed = cls._kill_processes_on_port(SERVER_PORT)
        
        total_killed = killed_count + port_killed
        if total_killed > 0:
            print(f"   {Fore.GREEN}✅ Emergency cleanup complete - killed {total_killed} processes{Style.RESET_ALL}")
            # Wait a moment for processes to actually die
            time.sleep(2)
        else:
            print(f"   {Fore.GREEN}✅ No stray processes found{Style.RESET_ALL}")
    
    @classmethod
    def _verify_server_stopped(cls):
        """Verify that the server is actually stopped"""
        try:
            response = requests.get(BASE_URL, timeout=1)
            print(f"   {Fore.RED}❌ SERVER STILL RUNNING! Response: HTTP {response.status_code}{Style.RESET_ALL}")
            print(f"   {Fore.RED}🚨 This will cause test failures - server restart is not working{Style.RESET_ALL}")
            return False
        except requests.ConnectionError:
            print(f"   {Fore.GREEN}✅ Verified: Server is stopped (connection refused){Style.RESET_ALL}")
            return True
        except requests.Timeout:
            print(f"   {Fore.GREEN}✅ Verified: Server is stopped (timeout){Style.RESET_ALL}")
            return True
        except Exception as e:
            print(f"   {Fore.YELLOW}⚠️  Could not verify server state: {e}{Style.RESET_ALL}")
            return False
    
    @classmethod
    def _clean_data_files(cls):
        """Remove any potential data persistence files"""
        # Common patterns for data files that might persist state
        potential_data_files = [
            "todos.db", "todos.json", "data.db", "data.json",
            "todolist.db", "todolist.json", "storage.db", "storage.json",
            "runTodoManagerRestAPI.db", "runTodoManagerRestAPI.json"
        ]
        
        cleaned_count = 0
        for filename in potential_data_files:
            if os.path.exists(filename):
                try:
                    os.remove(filename)
                    print(f"   {Fore.YELLOW}🗑️  Removed data file: {filename}{Style.RESET_ALL}")
                    cleaned_count += 1
                except Exception as e:
                    print(f"   {Fore.YELLOW}⚠️  Could not remove {filename}: {e}{Style.RESET_ALL}")
        
        if cleaned_count == 0:
            print(f"   {Fore.GREEN}✅ No data persistence files found{Style.RESET_ALL}")
        else:
            print(f"   {Fore.GREEN}✅ Cleaned {cleaned_count} data files{Style.RESET_ALL}")

    def setUp(self):
        """Start fresh server and set up test data - ensures complete test independence"""
        print(f"\n{Fore.BLUE}🔄 Starting fresh test environment...{Style.RESET_ALL}")
        
        # Start a fresh server instance for this test
        if not self._start_server():
            self.fail("Failed to start server for test")
        
        # Debug: Check what todos exist after server restart
        self._debug_server_state()
        
        # Generate unique identifiers for this test run (though with server restart, IDs are predictable)
        self.test_id = str(uuid.uuid4())[:8]
        
        # Fresh test data - IDs will be predictable since server is fresh
        self.todo_data = {
            "title": f"Test Todo {self.test_id}",
            "doneStatus": False,
            "description": f"Test description for {self.test_id}"
        }
        
        self.project_data = {
            "title": f"Test Project {self.test_id}",
            "description": f"Test project description for {self.test_id}",
            "completed": False,
            "active": True
        }
        
        self.category_data = {
            "title": f"Test Category {self.test_id}",
            "description": f"Test category description for {self.test_id}"
        }
        
        # With server restart, we don't need to track resources for cleanup
        # but we'll keep these for compatibility with existing test methods
        self.created_todos = []
        self.created_projects = []
        self.created_categories = []
        self.created_relationships = []

    def tearDown(self):
        """Stop the server - complete cleanup by server restart"""
        print(f"   {Fore.BLUE}🏁 Test completed, stopping server for complete cleanup...{Style.RESET_ALL}")
        try:
            self._stop_server()
        except Exception as e:
            print(f"   {Fore.YELLOW}⚠️  Error during server cleanup: {e}{Style.RESET_ALL}")
            # Force cleanup any remaining processes
            self._emergency_cleanup()
        print(f"   {Fore.GREEN}✅ Fresh state guaranteed for next test{Style.RESET_ALL}")

    # Cleanup methods are no longer needed since we restart the server between tests
    # This ensures complete state isolation - every test starts with a fresh database

    def _print_test_info(self, method: str, endpoint: str, description: str = ""):
        """Print formatted test information"""
        print(f"{Fore.BLUE}Testing: {Style.BRIGHT}{method} {endpoint}{Style.RESET_ALL}")
        if description:
            print(f"   {Fore.CYAN}{description}{Style.RESET_ALL}")
            
    def _print_result(self, response: requests.Response, expected_codes: List[int] = None):
        """Print formatted test result"""
        if expected_codes is None:
            expected_codes = [200]
        
        status = response.status_code
        if status in expected_codes:
            print(f"   {Fore.GREEN}Success: HTTP {status}{Style.RESET_ALL}")
        else:
            print(f"   {Fore.YELLOW}Unexpected: HTTP {status} (expected: {expected_codes}){Style.RESET_ALL}")
        
        if hasattr(response, 'content') and response.content:
            size = len(response.content)
            print(f"   {Fore.CYAN}Response size: {size} bytes{Style.RESET_ALL}")
        print()

    def _create_todo(self, data: Dict = None) -> Optional[str]:
        """Helper to create a todo and track it for cleanup"""
        if data is None:
            data = self.todo_data
        
        response = requests.post(f"{BASE_URL}/todos", json=data)
        if response.status_code in [200, 201]:
            try:
                todo_id = response.json().get("id")
                if todo_id:
                    self.created_todos.append(todo_id)
                    return todo_id
            except:
                pass
        return None

    def _create_project(self, data: Dict = None) -> Optional[str]:
        """Helper to create a project and track it for cleanup"""
        if data is None:
            data = self.project_data
        
        response = requests.post(f"{BASE_URL}/projects", json=data)
        if response.status_code in [200, 201]:
            try:
                project_id = response.json().get("id")
                if project_id:
                    self.created_projects.append(project_id)
                    return project_id
            except:
                pass
        return None

    def _create_category(self, data: Dict = None) -> Optional[str]:
        """Helper to create a category and track it for cleanup"""
        if data is None:
            data = self.category_data
        
        response = requests.post(f"{BASE_URL}/categories", json=data)
        if response.status_code in [200, 201]:
            try:
                category_id = response.json().get("id")
                if category_id:
                    self.created_categories.append(category_id)
                    return category_id
            except:
                pass
        return None


class TestTodosAPI(APITestBase):
    """Tests for /todos endpoints - built from your specifications"""
    
    def test_01_get_all_todos(self):
        """GET /todos - Test default todos structure"""
        spec = {
            "method": "GET",
            "endpoint": "/todos",
            "expected_response_code": [200],
            "expected_response_body": {
                "todos": [
                    {
                        "id": "2",
                        "title": "file paperwork",
                        "doneStatus": "false",
                        "description": "",
                        "tasksof": [
                            {
                                "id": "1"
                            }
                        ]
                    },
                    {
                        "id": "1",
                        "title": "scan paperwork",
                        "doneStatus": "false",
                        "description": "",
                        "tasksof": [
                            {
                                "id": "1"
                            }
                        ],
                        "categories": [
                            {
                                "id": "1"
                            }
                        ]
                    }
                ]
            },
            "description": "Should see exactly 2 default todos if server restart worked properly"
        }
        
        self._run_test_from_spec("test_01_get_all_todos", spec)
    
    def test_02_head_todos(self):
        """HEAD /todos - Test headers without body"""
        spec = {
            "method": "HEAD",
            "endpoint": "/todos",
            "expected_response_code": [200],
            "validate_headers": True
        }
        
        self._run_test_from_spec("test_02_head_todos", spec)
    
    def test_03_create_todo(self):
        """POST /todos - Create a new todo with predictable ID"""
        spec = {
            "method": "POST",
            "endpoint": "/todos",
            "request_body": {
                "title": "Check emails",
                "doneStatus": False,
                "description": "Respond to my important emails"
            },
            "expected_response_code": [201],
            "expected_response_body": {
                "id": "3",  # Should be ID 3 since server starts fresh with 2 default todos (id=1,2)
                "title": "Check emails", 
                "doneStatus": "false",
                "description": "Respond to my important emails"
            }
        }
        
        self._run_test_from_spec("test_03_create_todo", spec)
    
    def test_04_get_specific_todo(self):
        """GET /todos/{id} - Get a specific todo by ID"""
        spec = {
            "method": "GET",
            "endpoint": "/todos/{id}", 
            "id_replacements": {
                "{id}": "1"
            },
            "expected_response_code": [200],
            "expected_response_body": {
                "todos": [
                    {
                        "id": "1",
                        "title": "scan paperwork",
                        "doneStatus": "false",
                        "description": "",
                        "tasksof": [
                            {
                                "id": "1"
                            }
                        ],
                        "categories": [
                            {
                                "id": "1"
                            }
                        ]
                    }
                ]
            },
            "description": "Get specific todo with ID=1 - returns single todo object (not array)"
        }
        
        self._run_test_from_spec("test_04_get_specific_todo", spec)
    
    def test_05_head_specific_todo(self):
        """HEAD /todos/{id} - Test headers for specific todo without body"""
        spec = {
            "method": "HEAD",
            "endpoint": "/todos/{id}",
            "id_replacements": {
                "{id}": "1"
            },
            "expected_response_code": [200],
            "validate_headers": True,
            "description": "HEAD request for specific todo with ID=1 - should return headers without body"
        }
        
        self._run_test_from_spec("test_05_head_specific_todo", spec)
    
    def test_06_update_todo_post(self):
        """POST /todos/{id} - Update a specific todo using POST method"""
        spec = {
            "method": "POST",
            "endpoint": "/todos/{id}", 
            "id_replacements": {
                "{id}": "1"
            },
            "request_body": {
                "doneStatus": True,
                "description": "A new description here!"
            },
            "expected_response_code": [200],
            "expected_response_body": {
                "id": "1",
                "title": "scan paperwork",
                "doneStatus": "true",
                "description": "A new description here!",
                "tasksof": [
                    {
                        "id": "1"
                    }
                ],
                "categories": [
                    {
                        "id": "1"
                    }
                ]
            },
            "description": "Update todo ID=1 using POST - change doneStatus and description"
        }
        
        self._run_test_from_spec("test_06_update_todo_post", spec)
    
    def test_07_replace_todo_put(self):
        """PUT /todos/{id} - Replace a specific todo using PUT method"""
        spec = {
            "method": "PUT",
            "endpoint": "/todos/{id}", 
            "id_replacements": {
                "{id}": "1"
            },
            "request_body": {
                "title": "Since this one replaces the todo, a new title is required!",
                "description": "Updated the description here!",
                "doneStatus": False
            },
            "expected_response_code": [200],
            "expected_response_body": {
                "id": "1",
                "title": "Since this one replaces the todo, a new title is required!",
                "doneStatus": "false",
                "description": "Updated the description here!"
            },
            "description": "Replace todo ID=1 using PUT - replaces entire todo (no relationships preserved)"
        }
        
        self._run_test_from_spec("test_07_replace_todo_put", spec)
    
    def test_08_delete_todo(self):
        """DELETE /todos/{id} - Delete a specific todo"""
        spec = {
            "method": "DELETE",
            "endpoint": "/todos/{id}", 
            "id_replacements": {
                "{id}": "1"
            },
            "expected_response_code": [200],
            "description": "Delete todo ID=1 - should return 200 and remove the todo"
        }
        
        self._run_test_from_spec("test_08_delete_todo", spec)
    
    def test_09_get_todo_projects(self):
        """GET /todos/{id}/tasksof - Get projects that a todo is a task of"""
        spec = {
            "method": "GET",
            "endpoint": "/todos/{id}/tasksof",
            "id_replacements": {
                "{id}": "1"
            },
            "expected_response_code": [200],
            "expected_response_body": {
                "projects": [
                    {
                        "id": "1",
                        "title": "Office Work",
                        "completed": "false",
                        "active": "false",
                        "description": "",
                        "tasks": [
                            {
                                "id": "1"
                            },
                            {
                                "id": "2"
                            }
                        ]
                    }
                ]
            },
            "description": "Get projects that todo ID=1 is a task of - should return Office Work project"
        }
        
        self._run_test_from_spec("test_09_get_todo_projects", spec)
    
    def test_10_get_todo_projects_bug(self):
        """GET /todos/{id}/tasksof - Test known bug with placeholder ID (returns duplicates)"""
        spec = {
            "method": "GET",
            "endpoint": "/todos/{id}/tasksof",
            # Purposefully no id_replacements - this catches a known bug
            "expected_response_code": [200],
            "expected_response_body": {
                "projects": [
                    {
                        "id": "1",
                        "title": "Office Work",
                        "completed": "false",
                        "active": "false",
                        "description": "",
                        "tasks": [
                            {
                                "id": "2"
                            },
                            {
                                "id": "1"
                            }
                        ]
                    },
                    {
                        "id": "1",
                        "title": "Office Work",
                        "completed": "false",
                        "active": "false",
                        "description": "",
                        "tasks": [
                            {
                                "id": "2"
                            },
                            {
                                "id": "1"
                            }
                        ]
                    }
                ]
            },
            "description": "Bug test: /todos/{id}/tasksof with literal {id} returns duplicate projects"
        }
        
        self._run_test_from_spec("test_10_get_todo_projects_bug", spec)
    
    def test_11_head_todo_projects(self):
        """HEAD /todos/{id}/tasksof - Test headers for todo projects relationship without body"""
        spec = {
            "method": "HEAD",
            "endpoint": "/todos/{id}/tasksof",
            "id_replacements": {
                "{id}": "1"
            },
            "expected_response_code": [200],
            "validate_headers": True,
            "description": "HEAD request for todo projects relationship - should return headers without body"
        }
        
        self._run_test_from_spec("test_11_head_todo_projects", spec)
    
    def test_12_link_todo_to_project(self):
        """POST /todos/{id}/tasksof - Link a todo to a project (create relationship)"""
        spec = {
            "method": "POST",
            "endpoint": "/todos/{id}/tasksof",
            "setup_objects": {
                "project": {"title": "Test Project", "description": "For testing"}
            },
            "id_replacements": {
                "{id}": "1"
            },
            "request_body": {"id": "2"},
            "expected_response_code": [201],
            "description": "Create relationship: link todo ID=1 to project ID=2 (created in setup)"
        }
        
        self._run_test_from_spec("test_12_link_todo_to_project", spec)
    
    def test_13_create_project_via_todo_bug(self):
        """POST /todos/{id}/tasksof - Bug: Creates new project when no request body provided"""
        spec = {
            "method": "POST",
            "endpoint": "/todos/{id}/tasksof",
            "id_replacements": {
                "{id}": "1"
            },
            # No request_body - this triggers the bug behavior
            "expected_response_code": [201],
            "expected_response_body": {
                "id": "2",
                "title": "",
                "completed": "false",
                "active": "false",
                "description": "",
                "tasks": [
                    {
                        "id": "1"
                    }
                ]
            },
            "description": "Bug test: POST /todos/1/tasksof with no body creates new project with default values"
        }
        
        self._run_test_from_spec("test_13_create_project_via_todo_bug", spec)
    
    def test_14_delete_todo_project_relationship(self):
        """DELETE /todos/{id}/tasksof/{id2} - Remove relationship between todo and project"""
        spec = {
            "method": "DELETE",
            "endpoint": "/todos/{id}/tasksof/{id2}",
            "id_replacements": {
                "{id}": "1",
                "{id2}": "1"
            },
            "expected_response_code": [200],
            "description": "Delete relationship: unlink todo ID=1 from project ID=1"
        }
        
        self._run_test_from_spec("test_14_delete_todo_project_relationship", spec)
    
    def test_15_get_todo_categories(self):
        """GET /todos/{id}/categories - Get categories that a todo belongs to"""
        spec = {
            "method": "GET",
            "endpoint": "/todos/{id}/categories",
            "id_replacements": {
                "{id}": "1"
            },
            "expected_response_code": [200],
            "expected_response_body": {
                "categories": [
                    {
                        "id": "1",
                        "title": "Office",
                        "description": ""
                    }
                ]
            },
            "description": "Get categories that todo ID=1 belongs to - should return Office category"
        }
        
        self._run_test_from_spec("test_15_get_todo_categories", spec)
    
    def test_16_get_todo_categories_bug(self):
        """GET /todos/{id}/categories - Test known bug with placeholder ID (still works)"""
        spec = {
            "method": "GET",
            "endpoint": "/todos/{id}/categories",
            # Purposefully no id_replacements - this catches a known bug
            "expected_response_code": [200],
            "expected_response_body": {
                "categories": [
                    {
                        "id": "1",
                        "title": "Office",
                        "description": ""
                    }
                ]
            },
            "description": "Bug test: /todos/{id}/categories with literal {id} still returns Office category"
        }
        
        self._run_test_from_spec("test_16_get_todo_categories_bug", spec)
    
    def test_17_head_todo_categories(self):
        """HEAD /todos/{id}/categories - Test headers for todo categories relationship without body"""
        spec = {
            "method": "HEAD",
            "endpoint": "/todos/{id}/categories",
            "id_replacements": {
                "{id}": "1"
            },
            "expected_response_code": [200],
            "validate_headers": True,
            "description": "HEAD request for todo categories relationship - should return headers without body"
        }
        
        self._run_test_from_spec("test_17_head_todo_categories", spec)
    
    def test_18_link_todo_to_category(self):
        """POST /todos/{id}/categories - Link a todo to an existing category (create relationship)"""
        spec = {
            "method": "POST",
            "endpoint": "/todos/{id}/categories",
            "id_replacements": {
                "{id}": "1"
            },
            "request_body": {"id": "2"},
            "expected_response_code": [201],
            "description": "Create relationship: link todo ID=1 to category ID=2 (assumes category exists)"
        }
        
        self._run_test_from_spec("test_18_link_todo_to_category", spec)
    
    def test_19_create_category_via_todo_bug(self):
        """POST /todos/{id}/categories - Bug: Creates new category when title provided instead of id"""
        spec = {
            "method": "POST",
            "endpoint": "/todos/{id}/categories",
            "id_replacements": {
                "{id}": "1"
            },
            "request_body": {"title": "Test title"},
            "expected_response_code": [201],
            "expected_response_body": {
                "id": "3",
                "title": "Test title",
                "description": ""
            },
            "description": "Bug test: POST /todos/1/categories with title creates new category instead of linking"
        }
        
        self._run_test_from_spec("test_19_create_category_via_todo_bug", spec)
    
    def test_20_delete_todo_category_relationship(self):
        """DELETE /todos/{id}/categories/{id2} - Remove relationship between todo and category"""
        spec = {
            "method": "DELETE",
            "endpoint": "/todos/{id}/categories/{id2}",
            "id_replacements": {
                "{id}": "1",
                "{id2}": "1"
            },
            "expected_response_code": [200],
            "description": "Delete relationship: unlink todo ID=1 from category ID=1"
        }
        
        self._run_test_from_spec("test_20_delete_todo_category_relationship", spec)
    
    def _run_test_from_spec(self, test_name: str, spec: dict):
        """Run a test based on your provided specification"""
        print(f"\n{Fore.YELLOW}Running test: {test_name}{Style.RESET_ALL}")
        
        # Setup phase - create any required objects
        setup_objects = {}
        if "setup_objects" in spec:
            print(f"{Fore.CYAN}Setting up required objects...{Style.RESET_ALL}")
            for obj_type, obj_data in spec["setup_objects"].items():
                if obj_type == "todo":
                    obj_id = self._create_todo(obj_data)
                    setup_objects["todo_id"] = obj_id
                    print(f"   Created todo with ID: {obj_id}")
                elif obj_type == "category":
                    obj_id = self._create_category(obj_data)
                    setup_objects["category_id"] = obj_id
                    print(f"   Created category with ID: {obj_id}")
                elif obj_type == "project":
                    obj_id = self._create_project(obj_data)
                    setup_objects["project_id"] = obj_id
                    print(f"   Created project with ID: {obj_id}")
        
        # Build endpoint URL with any ID replacements
        endpoint = spec["endpoint"]
        if "id_replacements" in spec:
            for placeholder, source in spec["id_replacements"].items():
                if source in setup_objects:
                    # Use ID from setup objects (dynamic ID)
                    endpoint = endpoint.replace(placeholder, setup_objects[source])
                elif source == "hardcoded":
                    # Use hardcoded ID from spec
                    endpoint = endpoint.replace(placeholder, spec.get("hardcoded_id", "1"))
                else:
                    # Use the source value directly (literal ID)
                    endpoint = endpoint.replace(placeholder, source)
                print(f"   {Fore.CYAN}🔄 ID replacement: {placeholder} → {source} in {endpoint}{Style.RESET_ALL}")
        
        # Print test info
        self._print_test_info(spec["method"], endpoint, spec.get("description", ""))
        
        # Print request body if provided
        if "request_body" in spec:
            print(f"   {Fore.MAGENTA}Request body: {json.dumps(spec['request_body'], indent=2)}{Style.RESET_ALL}")
        
        # Make the request
        url = f"{BASE_URL}{endpoint}"
        headers = spec.get("headers", {})
        
        if spec["method"] == "GET":
            response = requests.get(url, headers=headers)
        elif spec["method"] == "HEAD":
            response = requests.head(url, headers=headers)
        elif spec["method"] == "POST":
            if "request_body" in spec:
                response = requests.post(url, json=spec["request_body"], headers=headers)
            elif "raw_data" in spec:
                response = requests.post(url, data=spec["raw_data"], headers=headers)
            else:
                response = requests.post(url, headers=headers)
        elif spec["method"] == "PUT":
            response = requests.put(url, json=spec.get("request_body"), headers=headers)
        elif spec["method"] == "DELETE":
            response = requests.delete(url, headers=headers)
        else:
            self.fail(f"Unsupported HTTP method: {spec['method']}")
        
        # Validate response code
        expected_codes = spec["expected_response_code"]
        if not isinstance(expected_codes, list):
            expected_codes = [expected_codes]
        
        self._print_result(response, expected_codes)
        
        # Debug: Show actual response content for unexpected status codes
        if response.status_code not in expected_codes:
            print(f"   {Fore.YELLOW}📋 Actual response content: {response.text[:200]}...{Style.RESET_ALL}")
            print(f"   {Fore.YELLOW}📋 Response headers: {dict(response.headers)}{Style.RESET_ALL}")
        
        self.assertIn(response.status_code, expected_codes, 
                     f"Expected response code {expected_codes}, got {response.status_code}")
        
        # Validate headers if requested
        if spec.get("validate_headers", False):
            self._validate_headers(response, spec)
        
        # Validate expected headers if specified
        if "expected_headers" in spec:
            self._validate_expected_headers(response, spec["expected_headers"])
        
        # Validate response body if specified
        if "expected_response_body" in spec and response.status_code in expected_codes:
            try:
                response_data = response.json()
                expected_body = spec["expected_response_body"]
                
                print(f"   {Fore.CYAN}Validating response body...{Style.RESET_ALL}")
                
                # Use deep validation for complex structures
                self._validate_response_structure(response_data, expected_body, "")
                
                print(f"   {Fore.GREEN}Response body validation: All specified fields match{Style.RESET_ALL}")
                
            except json.JSONDecodeError:
                if spec.get("require_json_response", True):
                    self.fail("Expected JSON response but got non-JSON")
                else:
                    print(f"   {Fore.CYAN}Non-JSON response (as expected){Style.RESET_ALL}")
        
        # Resource tracking no longer needed - server restart provides complete cleanup
        # Each test runs with a fresh server instance and predictable IDs
        if response.status_code in [200, 201] and spec["method"] == "POST":
            try:
                response_data = response.json()
                if "id" in response_data:
                    resource_id = response_data["id"]
                    print(f"   {Fore.GREEN}✅ Created resource with ID: {resource_id} (will be cleaned by server restart){Style.RESET_ALL}")
            except:
                pass
        
        return response

    def _validate_response_structure(self, actual, expected, path=""):
        """Recursively validate response structure against expected structure"""
        if isinstance(expected, dict):
            # Validate dictionary structure
            self.assertIsInstance(actual, dict, f"Expected dict at {path}, got {type(actual)}")
            
            for key, expected_value in expected.items():
                current_path = f"{path}.{key}" if path else key
                self.assertIn(key, actual, f"Missing key '{key}' at {current_path}")
                self._validate_response_structure(actual[key], expected_value, current_path)
                print(f"   {Fore.GREEN}✓ Validated {current_path}{Style.RESET_ALL}")
                
        elif isinstance(expected, list):
            # Validate list structure - ORDER INDEPENDENT
            self.assertIsInstance(actual, list, f"Expected list at {path}, got {type(actual)}")
            
            if len(expected) > 0:
                # Check if this looks like a list of expected specific objects (has unique identifiers)
                if self._is_object_list_with_ids(expected):
                    self._validate_object_list_order_independent(actual, expected, path)
                else:
                    # Use first item as template for structure validation (original behavior)
                    expected_item_structure = expected[0]
                    for i, actual_item in enumerate(actual):
                        current_path = f"{path}[{i}]" if path else f"[{i}]"
                        self._validate_response_structure(actual_item, expected_item_structure, current_path)
                    print(f"   {Fore.GREEN}✓ Validated {len(actual)} items in list at {path}{Style.RESET_ALL}")
            else:
                print(f"   {Fore.CYAN}Empty list validation at {path}{Style.RESET_ALL}")
                
        else:
            # Validate primitive values
            if expected == "*":
                # Wildcard - just validate presence and type
                self.assertIsNotNone(actual, f"Value at {path} should not be None")
                if isinstance(actual, str):
                    self.assertTrue(len(actual) > 0, f"String value at {path} should not be empty")
                print(f"   {Fore.GREEN}✓ {path}: {actual} (wildcard match){Style.RESET_ALL}")
            elif expected != "":  # Empty string means we don't care about the exact value
                self.assertEqual(actual, expected, f"Value mismatch at {path}: expected '{expected}', got '{actual}'")
            # If expected is empty string, we skip validation (don't care about value)

    def _is_object_list_with_ids(self, expected_list):
        """Check if this is a list of objects with 'id' fields (indicating specific expected objects)"""
        return (len(expected_list) > 0 and 
                isinstance(expected_list[0], dict) and 
                'id' in expected_list[0])

    def _validate_object_list_order_independent(self, actual_list, expected_list, path):
        """Validate that all expected objects are present in actual list, regardless of order"""
        self.assertEqual(len(actual_list), len(expected_list), 
                        f"List length mismatch at {path}: expected {len(expected_list)}, got {len(actual_list)}")
        
        # Create a map of expected objects by their ID for easy lookup
        expected_by_id = {}
        for expected_obj in expected_list:
            if 'id' in expected_obj:
                expected_by_id[expected_obj['id']] = expected_obj
        
        # Check each actual object against the expected object with the same ID
        found_ids = set()
        for i, actual_obj in enumerate(actual_list):
            self.assertIsInstance(actual_obj, dict, f"Expected object at {path}[{i}], got {type(actual_obj)}")
            self.assertIn('id', actual_obj, f"Missing 'id' field in object at {path}[{i}]")
            
            actual_id = actual_obj['id']
            self.assertIn(actual_id, expected_by_id, 
                         f"Unexpected object with ID '{actual_id}' at {path}[{i}]")
            
            # Validate this specific object against its expected counterpart
            expected_obj = expected_by_id[actual_id]
            current_path = f"{path}[id={actual_id}]"
            
            for key, expected_value in expected_obj.items():
                self.assertIn(key, actual_obj, f"Missing key '{key}' in object {actual_id} at {current_path}")
                self._validate_response_structure(actual_obj[key], expected_value, f"{current_path}.{key}")
            
            found_ids.add(actual_id)
            print(f"   {Fore.GREEN}✓ Validated object with ID '{actual_id}' at {path}{Style.RESET_ALL}")
        
        # Ensure all expected objects were found
        expected_ids = set(expected_by_id.keys())
        missing_ids = expected_ids - found_ids
        if missing_ids:
            self.fail(f"Missing expected objects with IDs {missing_ids} at {path}")
        
        print(f"   {Fore.GREEN}✓ All {len(expected_list)} expected objects found at {path} (order independent){Style.RESET_ALL}")

    def _validate_headers(self, response, spec):
        """Validate common HTTP headers are present and reasonable"""
        print(f"   {Fore.CYAN}Validating response headers...{Style.RESET_ALL}")
        
        headers = response.headers
        
        # Check for common headers that should be present
        common_headers = {
            'Content-Type': 'Should specify content type',
        }
        
        for header_name, description in common_headers.items():
            if header_name in headers:
                header_value = headers[header_name]
                print(f"   {Fore.GREEN}✓ {header_name}: {header_value}{Style.RESET_ALL}")
            else:
                print(f"   {Fore.YELLOW}⚠ Missing {header_name} ({description}){Style.RESET_ALL}")
        
        # For HEAD requests, validate that there's no response body
        if spec["method"] == "HEAD":
            content_length = len(response.content)
            if content_length == 0:
                print(f"   {Fore.GREEN}✓ HEAD request has no response body (correct){Style.RESET_ALL}")
            else:
                print(f"   {Fore.YELLOW}⚠ HEAD request has {content_length} bytes in body (unexpected){Style.RESET_ALL}")
        
        # Show all headers for debugging
        print(f"   {Fore.CYAN}All response headers:{Style.RESET_ALL}")
        for header_name, header_value in headers.items():
            print(f"     {header_name}: {header_value}")
    
    def _validate_expected_headers(self, response, expected_headers):
        """Validate specific expected headers"""
        print(f"   {Fore.CYAN}Validating expected headers...{Style.RESET_ALL}")
        
        for header_name, expected_value in expected_headers.items():
            self.assertIn(header_name, response.headers, 
                         f"Expected header '{header_name}' not found")
            
            actual_value = response.headers[header_name]
            
            if expected_value == "*":  # Wildcard - just check presence
                print(f"   {Fore.GREEN}✓ {header_name}: {actual_value} (present){Style.RESET_ALL}")
            else:
                self.assertEqual(actual_value, expected_value,
                               f"Header '{header_name}' should be '{expected_value}', got '{actual_value}'")
                print(f"   {Fore.GREEN}✓ {header_name}: {actual_value} (matches expected){Style.RESET_ALL}")
    
    def _debug_server_state(self):
        """Debug method to check server state after restart"""
        try:
            response = requests.get(f"{BASE_URL}/todos", timeout=5)
            if response.status_code == 200:
                data = response.json()
                todos = data.get("todos", [])
                print(f"   {Fore.CYAN}🔍 Server state check: Found {len(todos)} todos after restart{Style.RESET_ALL}")
                for todo in todos:
                    print(f"      - ID {todo.get('id')}: {todo.get('title', 'No title')}")
            else:
                print(f"   {Fore.YELLOW}⚠️  Could not check server state: HTTP {response.status_code}{Style.RESET_ALL}")
        except Exception as e:
            print(f"   {Fore.YELLOW}⚠️  Error checking server state: {e}{Style.RESET_ALL}")
    
    # Relationship tracking methods removed - server restart provides complete cleanup


if __name__ == "__main__":
    print(f"{Style.BRIGHT}{Fore.CYAN}Ready to run tests from your specifications...{Style.RESET_ALL}\n")
    
    # Test suite will be populated with tests based on your specifications
    unittest.main(verbosity=2)