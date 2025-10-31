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
    private JSONObject lastCreatedResource;
    private HttpClient httpClient;

    // Setup and teardown
    @Before
    public void setUp() {
        httpClient = HttpClient.newHttpClient();
        currentFields = new HashMap<>();
        lastCreatedResource = null;
        requestBody = null;
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        // Clean up any resources if needed
        httpClient = null;
        currentFields = null;
        lastCreatedResource = null;
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
        // Send shutdown request to the server
        try {
            sendRequest("POST", "/shutdown", null);
        } catch (IOException | InterruptedException e) {
            // Ignore connection errors as server will be down
        }
        
        // Give the server more time to fully shutdown and release resources
        Thread.sleep(3000);
        
        // Start the server again using ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "runTodoManagerRestAPI-1.5.5.jar");
        pb.directory(new File(System.getProperty("user.dir")).getParentFile());
        pb.start();
        
        // Give the server more time to fully initialize
        Thread.sleep(4000);
        
        // Verify server is running
        for (int i = 0; i < 5; i++) { // Try up to 5 times
            try {
                response = sendRequest("GET", "/todos", null);
                if (response.statusCode() == 200) {
                    return; // Server is up and running
                }
            } catch (IOException | InterruptedException e) {
                // Server might not be ready yet
                Thread.sleep(1000);
            }
        }
        
        throw new RuntimeException("Failed to restart the server after multiple attempts");
    }

    // Resource creation steps
    @Given("I have a todo with title {string} and description {string}")
    public void prepareTodoWithTitleAndDescription(String title, String description) {
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
        if (!field.equals("doneStatus")) {
            currentFields.put("doneStatus", "false");
        }
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

    // Resource existence verification
    @Given("a todo already exists in the system")
    public void verifyTodoExists() {
        // Using the default todo with id 1 that exists when server starts
    }

    @Given("there are existing todos in the system")
    public void createExistingTodos() {
        // do nothing
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

    // Action steps - Creation
    @When("I create the todo")
    public void createTodo() throws IOException, InterruptedException {
        JSONObject todoData = new JSONObject(currentFields);
        requestBody = todoData.toString();
        response = sendRequest("POST", "/todos", requestBody);
        if (response.statusCode() == 201) {
            lastCreatedResource = new JSONObject(response.body());
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
                lastCreatedResource = new JSONObject(response.body());
            } else {
                lastCreatedResource = null;
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
            lastCreatedResource = new JSONObject(response.body());
        }
    }
    

    // Action steps - Retrieval
    @When("I request all todos")
    public void requestAllTodos() throws IOException, InterruptedException {
        response = sendRequest("GET", "/todos", null);
    }

    @When("I request the todo with id {int}")
    public void requestSpecificTodo(int id) throws IOException, InterruptedException {
        response = sendRequest("GET", "/todos/" + id, null);
    }

    // Action steps - Update
    @When("I update the todo with a new title")
    public void updateTodoTitle() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("title", "Updated Title");
        response = sendRequest("PUT", "/todos/1", updateData.toString());
    }


    @When("I update the todo by adding a description")
    public void updateTodoDescription() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("description", "Updated description");
        response = sendRequest("POST", "/todos/1", updateData.toString());
    }

    @When("I attempt to update a todo that does not exist")
    public void attemptUpdateNonexistentTodo() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject(currentFields);
        response = sendRequest("PUT", "/todos/999999", updateData.toString());
    }

    // Action steps - Linking
    @When("I link the created todo to an existing project")
    public void linkTodoToProject() throws IOException, InterruptedException {
        if (lastCreatedResource != null) {
            String todoId;
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
            
            JSONObject requestData = new JSONObject();
            requestData.put("id", "1"); // Assuming project id 1 exists
            
            response = sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I try to link a non-existent todo to a category")
    public void attemptToLinkNonexistentTodoToCategory() throws IOException, InterruptedException {
        JSONObject requestData = new JSONObject();
        requestData.put("id", "1"); // Using a valid category ID
        
        // Using a non-existent todo ID
        response = sendRequest("POST", "/todos/999999/categories", requestData.toString());
    }

    @When("I try to link the todo to a non-existent project")
    public void linkTodoToNonExistentProject() throws IOException, InterruptedException {
        if (lastCreatedResource != null) {
            String todoId;
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
            
            // Send an empty JSON object as the body
            JSONObject requestData = new JSONObject();
            
            response = sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I attempt to link the todo without specifying a project")
    public void attemptToLinkTodoWithoutProject() throws IOException, InterruptedException {
        if (lastCreatedResource != null) {
            String todoId;
            if (lastCreatedResource.has("todos")) {
                todoId = lastCreatedResource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = lastCreatedResource.getString("id");
            }
            
            // Send an empty JSON object as the body
            JSONObject requestData = new JSONObject();
            
            response = sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I link an existing todo to an existing category")
    public void linkTodoToCategory() throws IOException, InterruptedException {
        JSONObject requestData = new JSONObject();
        requestData.put("id", "2"); // Using category id 2
        response = sendRequest("POST", "/todos/1/categories", requestData.toString()); // Using todo id 1
    }

    @When("I attempt to link a todo to a category using a title instead of an ID")
    public void linkTodoToCategoryByTitle() throws IOException, InterruptedException {
        JSONObject requestData = new JSONObject();
        requestData.put("title", "Some Title");
        response = sendRequest("POST", "/todos/1/categories", requestData.toString());
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

    @Then("the response should include a list of todos")
    public void verifyTodosList() {
        JSONObject responseObject = new JSONObject(response.body());
        assertTrue(responseObject.has("todos"), "Response should contain todos field");
        JSONArray todos = responseObject.getJSONArray("todos");
        assertTrue(todos.length() > 0, "Response should contain at least one todo");
    }

    @Then("the response should include an empty list")
    public void verifyEmptyList() {
        JSONObject responseObject = new JSONObject(response.body());
        assertTrue(responseObject.has("todos"), "Response should contain todos field");
        JSONArray todos = responseObject.getJSONArray("todos");
        assertEquals(0, todos.length(), "Response should contain an empty list of todos");
    }

    @Then("the todo should appear as linked to the project")
    public void verifyTodoProjectLink() throws IOException, InterruptedException {
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
                if (project.getString("id").equals("1")) {
                    found = true;
                    break;
                }
            }
            
            assertTrue(found, "Todo should be linked to project with id 1");
        }
    }

    @Then("the todo should appear as linked to the category")
    public void verifyTodoCategoryLink() throws IOException, InterruptedException {
        response = sendRequest("GET", "/todos/1/categories", null);
        assertEquals(200, response.statusCode(), "Should be able to get todo's categories");
        
        JSONObject responseObject = new JSONObject(response.body());
        JSONArray categories = responseObject.getJSONArray("categories");
        
        boolean found = false;
        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.getJSONObject(i);
            if (category.getString("id").equals("2")) {
                found = true;
                break;
            }
        }
        
        assertTrue(found, "Todo should be linked to category with id 2");
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
                if (project.getString("id").equals("2")) {
                    found = true;
                    break;
                }
            }
            
            assertTrue(found, "Todo should be linked to project with id 2");
        }
    }

    @Then("the response should show the new project associated with the todo")
    public void verifyNewProjectAssociation() {
        // do nothing
    }

    @Then("a new category should be created and linked to the todo")
    public void verifyNewCategoryLink() throws IOException, InterruptedException {
        response = sendRequest("GET", "/todos/1/categories", null);
        assertEquals(200, response.statusCode(), "Should be able to get todo's categories");
        
        JSONObject responseObject = new JSONObject(response.body());
        JSONArray categories = responseObject.getJSONArray("categories");
        
        boolean found = false;
        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.getJSONObject(i);
            if (category.getString("id").equals("3")) {
                found = true;
                break;
            }
        }
        
        assertTrue(found, "Todo should be linked to category with id 3");
    }

    @Then("the todo's title should reflect the updated value")
    public void verifyUpdatedTitle() throws IOException, InterruptedException {
        // Get the latest state of the todo
        response = sendRequest("GET", "/todos/1", null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedTitle = "Updated Title";
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should be updated");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should be updated");
        }
    }

    @Then("the todo's description should reflect the updated value")
    public void verifyUpdatedDescription() throws IOException, InterruptedException {
        // Get the latest state of the todo
        response = sendRequest("GET", "/todos/1", null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedDescription = "Updated description";
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedDescription, todo.getString("description"), "Todo description should be updated");
        } else {
            assertEquals(expectedDescription, responseObject.getString("description"), "Todo description should be updated");
        }
    }

    @Then("the todo's title should remain unchanged")
    public void verifyUnchangedTitle() throws IOException, InterruptedException {
        // Get the latest state of the todo
        response = sendRequest("GET", "/todos/1", null);
        JSONObject responseObject = new JSONObject(response.body());
        
        String expectedTitle = "scan paperwork";
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
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

    private JSONObject createResource(String resourceType, JSONObject data) throws IOException, InterruptedException {
        return null;
    }

    private boolean deleteResource(String resourceType, String id) throws IOException, InterruptedException {
        return false;
    }

    private boolean linkResources(String sourceType, String sourceId, String targetType, String targetId) throws IOException, InterruptedException {
        return false;
    }

    private HttpResponse<String> updateResource(String resourceType, String id, JSONObject data) throws IOException, InterruptedException {
        return null;
    }
}
