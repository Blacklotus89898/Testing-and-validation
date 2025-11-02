package ca.mcgill.story_testing.stepdefs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TodoStepDefinitions {

    private HttpResponse<String> response;
    private String requestBody;
    private final String BASE_URL = "http://localhost:4567";
    private Map<String, String> currentFields;
    private JSONObject lastCreatedResource; // Keeping for backward compatibility
    private JSONObject lastCreatedTodo;
    private JSONObject lastCreatedProject;
    private JSONObject lastCreatedCategory;
    private HttpClient httpClient;

    // Setup and teardown
    @Before
    public void setUp() {
        httpClient = HttpClient.newHttpClient();
        currentFields = new HashMap<>();
        lastCreatedResource = null;
        lastCreatedTodo = null;
        lastCreatedProject = null;
        lastCreatedCategory = null;
        requestBody = null;
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        // Clean up any resources if needed
        httpClient = null;
        currentFields = null;
        lastCreatedResource = null;
        lastCreatedTodo = null;
        lastCreatedProject = null;
        lastCreatedCategory = null;
        requestBody = null;
    }

    // Common system state steps
    @Given("the Todos API service is running")
    public void verifyAPIIsRunning() throws IOException, InterruptedException {
        response = sendRequest("GET", "/todos", null);
        assertEquals(200, response.statusCode(), "API should be running and return 200 OK");
    }

    @Given("the system has been reset to a clean state")
    public void resetSystemToCleanState() throws IOException, InterruptedException {
        // Delete all todos
        response = sendRequest("GET", "/todos", null);
        if (response.statusCode() == 200) {
            JSONObject resp = new JSONObject(response.body());
            JSONArray todos = resp.getJSONArray("todos");
            for (int i = 0; i < todos.length(); i++) {
                JSONObject todo = todos.getJSONObject(i);
                String id = todo.getString("id");
                sendRequest("DELETE", "/todos/" + id, null);
            }
        }

        // Delete all projects
        response = sendRequest("GET", "/projects", null);
        if (response.statusCode() == 200) {
            JSONObject resp = new JSONObject(response.body());
            JSONArray projects = resp.getJSONArray("projects");
            for (int i = 0; i < projects.length(); i++) {
                JSONObject project = projects.getJSONObject(i);
                String id = project.getString("id");
                sendRequest("DELETE", "/projects/" + id, null);
            }
        }

        // Delete all categories
        response = sendRequest("GET", "/categories", null);
        if (response.statusCode() == 200) {
            JSONObject resp = new JSONObject(response.body());
            JSONArray categories = resp.getJSONArray("categories");
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                String id = category.getString("id");
                sendRequest("DELETE", "/categories/" + id, null);
            }
        }
    }

    // Resource creation steps
    @Given("I have a todo with title {string} and description {string}")
    public void prepareTodoWithTitleAndDescription(String title, String description) {
        currentFields = new HashMap<>();
        currentFields.put("title", title);
        currentFields.put("description", description);
    }

    @Given("I have a project with title {string} and description {string}")
    public void prepareProjectWithTitleAndDescription(String title, String description) {
        currentFields = new HashMap<>();
        currentFields.put("title", title);
        currentFields.put("description", description);
    }

    @Given("I have a todo with missing field {string}")
    public void prepareTodoWithMissingField(String field) {
        currentFields = new HashMap<>();
        // Add all required fields except the one that should be missing
        if (!field.equals("title")) {
            currentFields.put("title", "Default Title");
        }
        if (!field.equals("description")) {
            currentFields.put("description", "Default Description");
        }
    }

    @Given("I have a project with missing field {string}")
    public void prepareProjectWithMissingField(String field) {
        currentFields = new HashMap<>();
        // Add all required fields except the one that should be missing
        // if (!field.equals("title")) {
        //     currentFields.put("title", "Default Title");
        // }
        // if (!field.equals("description")) {
        //     currentFields.put("description", "Default Description");
        // }
    }

    @Given("I have a todo with title {string} and the following optional fields")
    public void prepareTodoWithOptionalFields(String title, DataTable fields) {
        currentFields = new HashMap<>();
        currentFields.put("title", title);
        
        List<Map<String, String>> rows = fields.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String fieldName = row.get("field");
            String fieldValue = row.get("value");
            currentFields.put(fieldName, fieldValue);
        }
    }
  
    @Given("I have a project with title {string} and the following optional fields")
    public void prepareProjectWithOptionalFields(String title, DataTable fields) {
        currentFields = new HashMap<>();
        currentFields.put("title", title);
        
        List<Map<String, String>> rows = fields.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String fieldName = row.get("field");
            String fieldValue = row.get("value");
            currentFields.put(fieldName, fieldValue);
        }
    }

    // Resource existence verification
    @Given("a todo already exists in the system")
    public void verifyTodoExists() throws IOException, InterruptedException {
        JSONObject todoData = new JSONObject();
        todoData.put("title", "Original Title");
        response = sendRequest("POST", "/todos", todoData.toString());
        assertEquals(201, response.statusCode(), "Todo should be created successfully");
        if (response.statusCode() == 201) {
            lastCreatedResource = new JSONObject(response.body()); // Keep for backward compatibility
            lastCreatedTodo = new JSONObject(response.body());
        }
    }

    @Given("a project already exists in the system")
    public void verifyProjectExists() throws IOException, InterruptedException {
        JSONObject projectData = new JSONObject();
        projectData.put("title", "Original Title");
        projectData.put("description", "An existing project for testing");
        response = sendRequest("POST", "/projects", projectData.toString());
        assertEquals(201, response.statusCode(), "Project should be created successfully");
        if (response.statusCode() == 201) {
            lastCreatedProject = new JSONObject(response.body());
        }
    }

    @Given("a category already exists in the system")
    public void verifyCategoryExists() throws IOException, InterruptedException {
        JSONObject categoryData = new JSONObject();
        categoryData.put("title", "Existing Category");
        categoryData.put("description", "An existing category for testing");
        response = sendRequest("POST", "/categories", categoryData.toString());
        assertEquals(201, response.statusCode(), "Category should be created successfully");
        if (response.statusCode() == 201) {
            lastCreatedCategory = new JSONObject(response.body());
        }
    }

    @Given("there are existing todos in the system")
    public void createExistingTodos() throws IOException, InterruptedException {
        // Create first todo
        JSONObject firstTodo = new JSONObject();
        firstTodo.put("title", "First todo");
        response = sendRequest("POST", "/todos", firstTodo.toString());
        assertEquals(201, response.statusCode(), "First todo should be created successfully");

        // Create second todo
        JSONObject secondTodo = new JSONObject();
        secondTodo.put("title", "Second todo");
        response = sendRequest("POST", "/todos", secondTodo.toString());
        assertEquals(201, response.statusCode(), "Second todo should be created successfully");
    }

    @Given("there are existing projects in the system")
    public void createExistingprojects() throws IOException, InterruptedException {
        // Create first todo
        JSONObject firstProject = new JSONObject();
        firstProject.put("title", "First Project");
        response = sendRequest("POST", "/projects", firstProject.toString());
        assertEquals(201, response.statusCode(), "First Project should be created successfully");

        // Create second Project
        JSONObject secondProject = new JSONObject();
        secondProject.put("title", "Second Project");
        response = sendRequest("POST", "/projects", secondProject.toString());
        assertEquals(201, response.statusCode(), "Second todo should be created successfully");
    }

    @Given("the system has no todos")
    public void ensureSystemHasNoTodos() {
        try {
            response = sendRequest("GET", "/todos", null);
            assertEquals(200, response.statusCode(), "GET /todos should return 200");

            JSONObject resp = new JSONObject(response.body());
            if (resp.has("todos")) {
                JSONArray todos = resp.getJSONArray("todos");
                for (int i = 0; i < todos.length(); i++) {
                    JSONObject todo = todos.getJSONObject(i);
                    String id = todo.has("id") ? todo.get("id").toString() : String.valueOf(todo.getInt("id"));
                    sendRequest("DELETE", "/todos/" + id, null);
                }

                // Verify cleanup
                response = sendRequest("GET", "/todos", null);
                JSONObject resp2 = new JSONObject(response.body());
                JSONArray todos2 = resp2.getJSONArray("todos");
                assertEquals(0, todos2.length(), "Expected no todos after cleanup");
            } else {
                // If no "todos" field, assume empty or not applicable
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to ensure system has no todos", e);
        }
    }

    @Given("the system has no projects")
    public void ensureSystemHasNoprojects() {
        try {
            response = sendRequest("GET", "/projects", null);
            assertEquals(200, response.statusCode(), "GET /projects should return 200");

            JSONObject resp = new JSONObject(response.body());
            if (resp.has("projects")) {
                JSONArray projects = resp.getJSONArray("projects");
                for (int i = 0; i < projects.length(); i++) {
                    JSONObject todo = projects.getJSONObject(i);
                    String id = todo.has("id") ? todo.get("id").toString() : String.valueOf(todo.getInt("id"));
                    sendRequest("DELETE", "/projects/" + id, null);
                }

                // Verify cleanup
                response = sendRequest("GET", "/projects", null);
                JSONObject resp2 = new JSONObject(response.body());
                JSONArray projects2 = resp2.getJSONArray("projects");
                assertEquals(0, projects2.length(), "Expected no projects after cleanup");
            } else {
                // If no "projects" field, assume empty or not applicable
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to ensure system has no projects", e);
        }
    }

    // Action steps - Creation
    @When("I create the todo")
    public void createTodo() throws IOException, InterruptedException {
        JSONObject todoData = new JSONObject();
        // Convert doneStatus string to boolean if present
        for (Map.Entry<String, String> entry : currentFields.entrySet()) {
            if (entry.getKey().equals("doneStatus")) {
                todoData.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
            } else {
                todoData.put(entry.getKey(), entry.getValue());
            }
        }
        requestBody = todoData.toString();
        response = sendRequest("POST", "/todos", requestBody);
        if (response.statusCode() == 201) {
            lastCreatedResource = new JSONObject(response.body()); // Keep for backward compatibility
            lastCreatedTodo = new JSONObject(response.body());
        }
    }
    
    @When("I create the project")
    public void createProject() throws IOException, InterruptedException {
        JSONObject projectData = new JSONObject();
        // Convert doneStatus string to boolean if present
        for (Map.Entry<String, String> entry : currentFields.entrySet()) {
            if (entry.getKey().equals("completed") || entry.getKey().equals("active")) {
                projectData.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
            } else {
                projectData.put(entry.getKey(), entry.getValue());
            }
        }
        requestBody = projectData.toString();
        response = sendRequest("POST", "/projects", requestBody);
        if (response.statusCode() == 201) {
            lastCreatedResource = new JSONObject(response.body()); // Keep for backward compatibility
            lastCreatedTodo = new JSONObject(response.body());
        }
    }

    @When("I try to create the todo")
    public void attemptToCreateTodo() {
        JSONObject todoData = new JSONObject(currentFields);
        requestBody = todoData.toString();
        try {
            response = sendRequest("POST", "/todos", requestBody);
            // Expecting a client error (e.g., 400). Do not populate lastCreatedResource on failure.
            if (response.statusCode() == 201) {
                lastCreatedResource = new JSONObject(response.body()); // Keep for backward compatibility
                lastCreatedTodo = new JSONObject(response.body());
            } else {
                lastCreatedResource = null;
                lastCreatedTodo = null;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to send create request", e);
        }
    }
    @When("I try to create the project")
    public void attemptToCreateproject() {
        JSONObject projectData = new JSONObject(currentFields);
        requestBody = projectData.toString();
        try {
            response = sendRequest("POST", "/projects", requestBody);
            // Expecting a client error (e.g., 400). Do not populate lastCreatedResource on failure.
            if (response.statusCode() == 201) {
                lastCreatedResource = new JSONObject(response.body()); // Keep for backward compatibility
                lastCreatedProject = new JSONObject(response.body());
            } else {
                lastCreatedResource = null;
                lastCreatedProject = null;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to send create request", e);
        }
    }

    @When("I create a todo with title \"New Todo\" and description \"To be linked\"")
    public void createTodoForLinking() throws IOException, InterruptedException {
        JSONObject todoData = new JSONObject();
        todoData.put("title", "New Todo");
        todoData.put("description", "To be linked");
        requestBody = todoData.toString();
        response = sendRequest("POST", "/todos", requestBody);
        if (response.statusCode() == 201) {
            lastCreatedResource = new JSONObject(response.body()); // Keep for backward compatibility
            lastCreatedTodo = new JSONObject(response.body());
        }
    }
    

    // Action steps - Retrieval
    @When("I request all todos")
    public void requestAllTodos() throws IOException, InterruptedException {
        response = sendRequest("GET", "/todos", null);
    }
 
    @When("I request all projects")
    public void requestAllProjects() throws IOException, InterruptedException {
        response = sendRequest("GET", "/projects", null);
    }

    @When("I request the todo with id {int}")
    public void requestSpecificTodo(int id) throws IOException, InterruptedException {
        response = sendRequest("GET", "/todos/" + id, null);
    }

    @When("I request the project with id {int}")
    public void requestSpecificProject(int id) throws IOException, InterruptedException {
        response = sendRequest("GET", "/projects/" + id, null);
    }

    // Action steps - Update
    @When("I update the todo with a new title")
    public void updateTodoTitle() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("title", "Updated Title");

        // Get the ID of the last created todo
        String todoId;
        if (lastCreatedResource != null) {
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
            response = sendRequest("PUT", "/todos/" + todoId, updateData.toString());
        } else {
            // Fallback to todo/1 if no todo was created in previous step
            response = sendRequest("PUT", "/todos/1", updateData.toString());
        }
    }
   
    @When("I update the project with a new title")
    public void updateProjectTitle() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("title", "Updated Title");

        // Get the ID of the last created project
        String projectId = lastCreatedProject.getString("id");

        // String projectId;
        // if (lastCreatedResource != null) {
        //     if (lastCreatedResource.has("projects")) {
        //         projectId = lastCreatedResource.getJSONArray("projects").getJSONObject(0).getString("id");
        //     } else {
        //         projectId = lastCreatedResource.getString("id");
        //     }
            response = sendRequest("PUT", "/projects/" + projectId, updateData.toString());
        // } else {
        //     // Fallback to project/1 if no project was created in previous step
        //     response = sendRequest("PUT", "/projects/1", updateData.toString());
        // }
    }


    @When("I update the todo by adding a description")
    public void updateTodoDescription() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("description", "Updated description");

        // Get the ID of the last created todo
        String todoId;
        if (lastCreatedResource != null) {
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        } else {
            todoId = "1"; // Fallback to todo/1 if no todo was created
        }

        response = sendRequest("POST", "/todos/" + todoId, updateData.toString());
    }

    @When("I update the project by adding a description")
    public void updateProjectDescription() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("description", "Updated description");

        // Get the ID of the last created todo
        // String projectId;
        // if (lastCreatedResource != null) {
        //     if (lastCreatedResource.has("projects")) {
        //         projectId = lastCreatedResource.getJSONArray("projects").getJSONObject(0).getString("id");
        //     } else {
        //         projectId = lastCreatedResource.getString("id");
        //     }
        // } else {
        //     projectId = "1"; // Fallback to todo/1 if no todo was created
        // }

        String projectId = lastCreatedProject.getString("id");

        response = sendRequest("POST", "/projects/" + projectId, updateData.toString());
    }

    @When("I attempt to update a todo that does not exist")
    public void attemptUpdateNonexistentTodo() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject(currentFields);
        response = sendRequest("PUT", "/todos/999999", updateData.toString());
    }
    
    @When("I attempt to update a project that does not exist")
    public void attemptUpdateNonexistentProject() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject(currentFields);
        response = sendRequest("PUT", "/projects/999999", updateData.toString());
    }

    // Action steps - Linking
    @When("I link the created todo to an existing project")
    public void linkTodoToProject() throws IOException, InterruptedException {
        String todoId = null;
        String projectId = "1"; // Default fallback
        
        // Get todo ID
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedTodo.getString("id");
            }
        } else if (lastCreatedResource != null) { // Backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        }
        
        // Get project ID if we have a newly created project
        if (lastCreatedProject != null) {
            if (lastCreatedProject.has("projects")) {
                projectId = lastCreatedProject.getJSONArray("projects").getJSONObject(0).getString("id");
            } else {
                projectId = lastCreatedProject.getString("id");
            }
        }
        
        if (todoId != null) {
            JSONObject requestData = new JSONObject();
            requestData.put("id", projectId);
            response = sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I try to link a non-existent todo to a category")
    public void attemptToLinkNonexistentTodoToCategory() throws IOException, InterruptedException {
        JSONObject requestData = new JSONObject();
        String categoryId = "1"; // Default fallback
        
        // Get category ID if we have a newly created category
        if (lastCreatedCategory != null) {
            if (lastCreatedCategory.has("categories")) {
                categoryId = lastCreatedCategory.getJSONArray("categories").getJSONObject(0).getString("id");
            } else {
                categoryId = lastCreatedCategory.getString("id");
            }
        }

        requestData.put("id", categoryId);
        
        // Using a non-existent todo ID
        response = sendRequest("POST", "/todos/999999/categories", requestData.toString());
    }

    @When("I try to link the todo to a non-existent project")
    public void linkTodoToNonExistentProject() throws IOException, InterruptedException {
        String todoId = null;
        
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id"); 
            } else {
                todoId = lastCreatedTodo.getString("id");
            }
        } else if (lastCreatedResource != null) { // Fallback for backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id"); 
            }
        }

        if (todoId != null) {
            JSONObject requestData = new JSONObject();
            requestData.put("id", "999"); // Use a non-existent project ID
            response = sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I attempt to link the todo without specifying a project")
    public void attemptToLinkTodoWithoutProject() throws IOException, InterruptedException {
        String todoId = null;
        
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedTodo.getString("id");
            }
        } else if (lastCreatedResource != null) { // Fallback for backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        }

        if (todoId != null) {
            // Send an empty JSON object as the body
            JSONObject requestData = new JSONObject();
            response = sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I link the created todo to an existing category")
    public void linkTodoToCategory() throws IOException, InterruptedException {
        String todoId = null;
        String categoryId = "1"; // Default fallback
        
        // Get todo ID
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedTodo.getString("id");
            }
        } else if (lastCreatedResource != null) { // Backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        }
        
        // Get category ID if we have a newly created category
        if (lastCreatedCategory != null) {
            if (lastCreatedCategory.has("categories")) {
                categoryId = lastCreatedCategory.getJSONArray("categories").getJSONObject(0).getString("id");
            } else {
                categoryId = lastCreatedCategory.getString("id");
            }
        }

        if (todoId != null) {
            JSONObject requestData = new JSONObject();
            requestData.put("id", categoryId);
            response = sendRequest("POST", "/todos/" + todoId + "/categories", requestData.toString());
        }
    }

    @When("I attempt to link a todo to a category using a title instead of an ID")
    public void linkTodoToCategoryByTitle() throws IOException, InterruptedException {
        String todoId = null;
        
        // Get todo ID
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedTodo.getString("id");
            }
        } else if (lastCreatedResource != null) { // Backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        }

        JSONObject requestData = new JSONObject();
        requestData.put("title", "Some Title");

        if (todoId != null) {
            response = sendRequest("POST", "/todos/" + todoId + "/categories", requestData.toString());
            if (response.statusCode() == 201) {
                lastCreatedCategory = new JSONObject(response.body());
            }
        } else {
            response = sendRequest("POST", "/todos/1/categories", requestData.toString());
            if (response.statusCode() == 201) {
                lastCreatedCategory = new JSONObject(response.body());
            }
        }
    }

    // Action steps Deletion
    @When("I delete the todo")
    public void deleteTodo() throws IOException, InterruptedException {
         String todoId = lastCreatedTodo.getString("id");
            response = sendRequest("DELETE", "/todos/" + todoId, null);
    }

    @When("I delete the project")
    public void deleteProject() throws IOException, InterruptedException {
         String projectId = lastCreatedProject.getString("id");
            response = sendRequest("DELETE", "/projects/" + projectId, null);
    }

    @When("I attempt to delete a todo that does not exist")
    public void attemptDeleteNonexistentTodo() throws IOException, InterruptedException {
        response = sendRequest("DELETE", "/todos/999999", null);  
    }

    @When("I attempt to delete a project that does not exist")
    public void attemptDeleteNonexistentProject() throws IOException, InterruptedException {
        response = sendRequest("DELETE", "/projects/999999", null);  
    }

    @When("I attempt to delete a todo without Id")
    public void attemptDeleteTodoWihtoutId() throws IOException, InterruptedException {
        response = sendRequest("DELETE", "/todos", null);
    }

    @When("I attempt to delete a project without Id")
    public void attemptDeleteprojectWihtoutId() throws IOException, InterruptedException {
        response = sendRequest("DELETE", "/projects", null);
    }

    // Response verification steps
    @Then("the operation should succeed with status {int}")
    public void verifySuccessStatus(int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode(), "Operation should have succeeded with status " + expectedStatus);
    }

    @Then("the operation should fail with status {int}")
    public void verifyFailureStatus(int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode(), "Operation should have failed with status " + expectedStatus);
    }

@Then("The todo should no longer exist with status {int}")
public void verifyNonExistenceTodoStatus(int expectedStatus) throws IOException, InterruptedException {
    String todoId = lastCreatedTodo.optString("id", null);

    response = sendRequest("GET", "/todos/" + todoId, null);
    assertEquals(expectedStatus, response.statusCode(), "Expected status code mismatch when verifying todo non-existence.");
}

@Then("The project should no longer exist with status {int}")
public void verifyNonExistenceProjectStatus(int expectedStatus) throws IOException, InterruptedException {
    String projectId = lastCreatedProject.optString("id", null);

    response = sendRequest("GET", "/projects/" + projectId, null);
    assertEquals(expectedStatus, response.statusCode(), "Expected status code mismatch when verifying project non-existence.");
}


    @Then("the error message should include {string}")
    public void verifyErrorMessage(String expectedError) {
        JSONObject responseObject = new JSONObject(response.body());
        if (responseObject.has("errorMessages")) {
            JSONArray errors = responseObject.getJSONArray("errorMessages");
            boolean found = false;
            for (int i = 0; i < errors.length(); i++) {
                String actualError = errors.getString(i);
                if (expectedError.contains(" for ") && expectedError.contains(" entity ")) {
                    // Split expected error into prefix and suffix around the dynamic ID
                    String[] parts = expectedError.split(" for ");
                    String prefix = parts[0];
                    String suffix = parts[1].substring(parts[1].indexOf(" "));
                    
                    // Check if actual error starts and ends with our expected parts
                    if (actualError.startsWith(prefix + " for ") && actualError.endsWith(suffix)) {
                        found = true;
                        break;
                    }
                } else if (actualError.contains(expectedError)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String errorMessage = String.join(", ", errors.toList().stream().map(Object::toString).toList());
                assertTrue(false, "Error message should match pattern: " + expectedError + 
                    "\nActual error message: " + errorMessage);
            }
        } else {
            String actualError = responseObject.getString("error");
            if (expectedError.contains(" for ") && expectedError.contains(" entity ")) {
                String[] parts = expectedError.split(" for ");
                String prefix = parts[0];
                String suffix = parts[1].substring(parts[1].indexOf(" "));
                assertTrue(
                    actualError.startsWith(prefix + " for ") && actualError.endsWith(suffix),
                    "Error message should match pattern: " + expectedError + "\nActual: " + actualError
                );
            } else {
                assertTrue(
                    actualError.contains(expectedError),
                    "Error message should contain: " + expectedError + "\nActual: " + actualError
                );
            }
        }
    }

    @Then("the error message should be {string}")
    public void verifyExactErrorMessage(String expectedMessage) {
        JSONObject responseObject = new JSONObject(response.body());
        String actualMessage;
        if (responseObject.has("errorMessages")) {
            JSONArray errors = responseObject.getJSONArray("errorMessages");
            actualMessage = String.join(", ", errors.toList().stream().map(Object::toString).toList());
        } else {
            actualMessage = responseObject.getString("error");
        }
        assertEquals(expectedMessage, actualMessage, "Error message should match exactly");
    }

    // Resource state verification steps
    @Then("the created todo should include the title {string}")
    public void verifyTodoTitle(String expectedTitle) {
        JSONObject responseObject = new JSONObject(response.body());
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should match the expected value");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should match the expected value");
        }
    }

    @Then("the created project should include the title {string}")
    public void verifyProjectTitle(String expectedTitle) {
        JSONObject responseObject = new JSONObject(response.body());
        if (responseObject.has("projects")) {
            JSONObject project = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedTitle, project.getString("title"), "Todo title should match the expected value");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should match the expected value");
        }
    }


    @Then("the created todo should include the description {string}")
    public void verifyTodoDescription(String expectedDescription) {
        JSONObject responseObject = new JSONObject(response.body());
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedDescription, todo.getString("description"), "Todo description should match the expected value");
        } else {
            assertEquals(expectedDescription, responseObject.getString("description"), "Todo description should match the expected value");
        }
    }

    @Then("the created todo should include the optional fields")
    public void verifyTodoOptionalFields() {
        JSONObject responseObject = new JSONObject(response.body());
        JSONObject todo;
        if (responseObject.has("todos")) {
            todo = responseObject.getJSONArray("todos").getJSONObject(0);
        } else {
            todo = responseObject;
        }

        assertTrue(todo.has("doneStatus"), "Todo should have doneStatus field");
        assertTrue(todo.has("description") && !todo.getString("description").isEmpty(), 
            "Todo should have a non-empty description");
    }

    @Then("the created project should include the optional fields")
    public void verifyProjectOptionalFields() {
        JSONObject responseObject = new JSONObject(response.body());
        JSONObject project;
        if (responseObject.has("projects")) {
            project = responseObject.getJSONArray("projects").getJSONObject(0);
        } else {
            project = responseObject;
        }

        assertTrue(project.has("completed"), "project should have completed field");
        assertTrue(project.has("active") ,"project should have a status field");
    }

    @Then("the response should include a list of todos")
    public void verifyTodosList() {
        JSONObject responseObject = new JSONObject(response.body());
        assertTrue(responseObject.has("todos"), "Response should contain todos field");
        JSONArray todos = responseObject.getJSONArray("todos");
        assertTrue(todos.length() > 0, "Response should contain at least one todo");
    }

    @Then("the response should include a list of projects")
    public void verifyProjectList() {
        JSONObject responseObject = new JSONObject(response.body());
        assertTrue(responseObject.has("projects"), "Response should contain projects field");
        JSONArray projects = responseObject.getJSONArray("projects");
        assertTrue(projects.length() > 0, "Response should contain at least one project");
    }

    @Then("the response should include an empty list")
    public void verifyEmptyList() {
        JSONObject responseObject = new JSONObject(response.body());
        assertTrue(responseObject.has("todos"), "Response should contain todos field");
        JSONArray todos = responseObject.getJSONArray("todos");
        assertEquals(0, todos.length(), "Response should contain an empty list of todos");
    }

    @Then("the response should include an empty project list")
    public void verifyEmptyProjectList() {
        JSONObject responseObject = new JSONObject(response.body());
        assertTrue(responseObject.has("projects"), "Response should contain projects field");
        JSONArray projects = responseObject.getJSONArray("projects");
        assertEquals(0, projects.length(), "Response should contain an empty list of projects");
    }

    @Then("the todo should appear as linked to the project")
    public void verifyTodoProjectLink() throws IOException, InterruptedException {
        String todoId = null;
        String projectId = "1"; // Default fallback
        
        // Get todo ID
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedTodo.getString("id");
            }
        } else if (lastCreatedResource != null) { // Backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        }
        
        // Get project ID if we have a newly created project
        if (lastCreatedProject != null) {
            if (lastCreatedProject.has("projects")) {
                projectId = lastCreatedProject.getJSONArray("projects").getJSONObject(0).getString("id");
            } else {
                projectId = lastCreatedProject.getString("id");
            }
        }

        if (todoId != null) {
            response = sendRequest("GET", "/todos/" + todoId + "/tasksof", null);
            assertEquals(200, response.statusCode(), "Should be able to get todo's projects");
            
            JSONObject responseObject = new JSONObject(response.body());
            JSONArray projects = responseObject.getJSONArray("projects");
            
            boolean found = false;
            for (int i = 0; i < projects.length(); i++) {
                JSONObject project = projects.getJSONObject(i);
                if (project.getString("id").equals(projectId)) {
                    found = true;
                    break;
                }
            }
            
            assertTrue(found, "Todo should be linked to project with id " + projectId);
        }
    }

    @Then("the todo should appear as linked to the category")
    public void verifyTodoCategoryLink() throws IOException, InterruptedException {
        String todoId = null;
        String categoryId = "1"; // Default fallback
        
        // Get todo ID
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedTodo.getString("id"); 
            }
        } else if (lastCreatedResource != null) { // Backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        }
        
        // Get category ID if we have a newly created category
        if (lastCreatedCategory != null) {
            if (lastCreatedCategory.has("categories")) {
                categoryId = lastCreatedCategory.getJSONArray("categories").getJSONObject(0).getString("id");
            } else {
                categoryId = lastCreatedCategory.getString("id");
            }
        }

        if (todoId != null) {
            response = sendRequest("GET", "/todos/" + todoId + "/categories", null);
            assertEquals(200, response.statusCode(), "Should be able to get todo's categories");
            
            JSONObject responseObject = new JSONObject(response.body());
            JSONArray categories = responseObject.getJSONArray("categories");
            
            boolean found = false;
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                if (category.getString("id").equals(categoryId)) {
                    found = true;
                    break;
                }
            }
            
            assertTrue(found, "Todo should be linked to category with id " + categoryId);
        }
    }

    @Then("a new empty project should be created and linked to the todo")
    public void verifyEmptyProjectCreation() throws IOException, InterruptedException {
        if (lastCreatedResource != null) {
            String todoId;
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
            
            response = sendRequest("GET", "/todos/" + todoId + "/tasksof", null);
            assertEquals(200, response.statusCode(), "Should be able to get todo's projects");
            
            JSONObject responseObject = new JSONObject(response.body());
            JSONArray projects = responseObject.getJSONArray("projects");
            
            boolean found = false;
            for (int i = 0; i < projects.length(); i++) {
                JSONObject project = projects.getJSONObject(i);
                JSONArray tasks = project.getJSONArray("tasks");
                for (int j = 0; j < tasks.length(); j++) {
                    if (tasks.getJSONObject(j).getString("id").equals(todoId)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            
            assertTrue(found, "Todo should be linked to a project");
        }
    }

    @Then("the response should show the new project associated with the todo")
    public void verifyNewProjectAssociation() {
        // do nothing
    }

    @Then("a new category should be created and linked to the todo")
    public void verifyNewCategoryLink() throws IOException, InterruptedException {
        String todoId = null;
        String categoryId = "1"; // Default fallback
        
        // Get todo ID
        if (lastCreatedTodo != null) {
            if (lastCreatedTodo.has("todos")) {
                todoId = lastCreatedTodo.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedTodo.getString("id");
            }
        } else if (lastCreatedResource != null) { // Backward compatibility
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        }
        
        // Get category ID if we have a newly created category
        if (lastCreatedCategory != null) {
            if (lastCreatedCategory.has("categories")) {
                categoryId = lastCreatedCategory.getJSONArray("categories").getJSONObject(0).getString("id");
            } else {
                categoryId = lastCreatedCategory.getString("id");
            }
        }

        if (todoId != null) {
            response = sendRequest("GET", "/todos/" + todoId + "/categories", null);
            assertEquals(200, response.statusCode(), "Should be able to get todo's categories");
            
            JSONObject responseObject = new JSONObject(response.body());
            JSONArray categories = responseObject.getJSONArray("categories");
            
            boolean found = false;
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                if (category.getString("id").equals(categoryId)) {
                    found = true;
                    break;
                }
            }
            
            assertTrue(found, "Todo should be linked to category with id " + categoryId);
        }
    }

    @Then("the todo's title should reflect the updated value")
    public void verifyUpdatedTitle() throws IOException, InterruptedException {
        // Get the ID of the last created todo
        String todoId;
        if (lastCreatedResource != null) {
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        } else {
            todoId = "1"; // Fallback to todo/1 if no todo was created
        }

        // Get the latest state of the todo
        response = sendRequest("GET", "/todos/" + todoId, null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedTitle = "Updated Title";
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should be updated");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should be updated");
        }
    }

    @Then("the project's title should reflect the updated value")
    public void verifyUpdatedProjectTitle() throws IOException, InterruptedException {
        // Get the ID of the last created todo
        // String projectId;
        // if (lastCreatedResource != null) {
        //     if (lastCreatedResource.has("projects")) {
        //         projectId = lastCreatedResource.getJSONArray("projects").getJSONObject(0).getString("id");
        //     } else {
        //         projectId = lastCreatedResource.getString("id");
        //     }
        // } else {
        //     projectId = "1"; // Fallback to todo/1 if no todo was created
        // }

        String projectId = lastCreatedProject.getString("id");

        // Get the latest state of the todo
        response = sendRequest("GET", "/projects/" + projectId, null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedTitle = "Updated Title";
        if (responseObject.has("projects")) {
            JSONObject todo = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should be updated");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should be updated");
        }
    }

    @Then("the todo's description should reflect the updated value")
    public void verifyUpdatedDescription() throws IOException, InterruptedException {
        // Get the ID of the last created todo
        String todoId;
        if (lastCreatedResource != null) {
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        } else {
            todoId = "1"; // Fallback to todo/1 if no todo was created
        }

        // Get the latest state of the todo
        response = sendRequest("GET", "/todos/" + todoId, null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedDescription = "Updated description";
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedDescription, todo.getString("description"), "Todo description should be updated");
        } else {
            assertEquals(expectedDescription, responseObject.getString("description"), "Todo description should be updated");
        }
    }

    @Then("the project's description should reflect the updated value")
    public void verifyUpdatedProjectDescription() throws IOException, InterruptedException {
        // Get the ID of the last created todo
        // String projectId;
        // if (lastCreatedResource != null) {
        //     if (lastCreatedResource.has("projects")) {
        //         projectId = lastCreatedResource.getJSONArray("projects").getJSONObject(0).getString("id");
        //     } else {
        //         projectId = lastCreatedResource.getString("id");
        //     }
        // } else {
        //     projectId = "1"; // Fallback to todo/1 if no todo was created
        // }

        String projectId = lastCreatedProject.getString("id");

        // Get the latest state of the todo
        response = sendRequest("GET", "/projects/" + projectId, null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedDescription = "Updated description";
        if (responseObject.has("projects")) {
            JSONObject todo = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedDescription, todo.getString("description"), "Todo description should be updated");
        } else {
            assertEquals(expectedDescription, responseObject.getString("description"), "Todo description should be updated");
        }
    }

    @Then("the todo's title should remain unchanged")
    public void verifyUnchangedTitle() throws IOException, InterruptedException {
        // Get the ID of the last created todo
        String todoId;
        if (lastCreatedResource != null) {
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
        } else {
            todoId = "1"; // Fallback to todo/1 if no todo was created
        }

        // Get the latest state of the todo
        response = sendRequest("GET", "/todos/" + todoId, null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedTitle = "Original Title";
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should remain unchanged");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should remain unchanged");
        }
    }

    @Then("the project's title should remain unchanged")
    public void verifyUnchangedProjectTitle() throws IOException, InterruptedException {
        // Get the ID of the last created todo
        // String projectId;
        // if (lastCreatedResource != null) {
        //     if (lastCreatedResource.has("projects")) {
        //         projectId = lastCreatedResource.getJSONArray("projects").getJSONObject(0).getString("id");
        //     } else {
        //         projectId = lastCreatedResource.getString("id");
        //     }
        // } else {
        //     projectId = "1"; // Fallback to todo/1 if no todo was created
        // }
        String projectId = lastCreatedProject.getString("id");

        // Get the latest state of the todo
        response = sendRequest("GET", "/projects/" + projectId, null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedTitle = "Original Title";
        if (responseObject.has("projects")) {
            JSONObject todo = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should remain unchanged");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should remain unchanged");
        }
    }

    // Helper methods
    private HttpResponse<String> sendRequest(String method, String endpoint, String body) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint));

        switch (method.toUpperCase()) {
            case "GET" -> requestBuilder.GET();
            case "POST" -> requestBuilder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            case "PUT" -> requestBuilder.header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            case "DELETE" -> requestBuilder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }


}
