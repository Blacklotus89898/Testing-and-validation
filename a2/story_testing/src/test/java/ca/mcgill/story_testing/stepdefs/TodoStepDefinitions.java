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
        
        // Wait a moment for the server to fully shutdown
        Thread.sleep(1000);
        
        // Start the server again using ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "runTodoManagerRestAPI-1.5.5.jar");
        pb.directory(new File(System.getProperty("user.dir")).getParentFile());
        pb.start();
        
        // Wait for server to start up
        Thread.sleep(2000);
        
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
    }

    @Given("there are existing todos in the system")
    public void createExistingTodos() {
    }

    @Given("the system has no todos")
    public void ensureSystemHasNoTodos() {
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
        JSONObject updateData = new JSONObject(currentFields);
        if (lastCreatedResource != null && lastCreatedResource.has("todos")) {
            JSONObject todo = lastCreatedResource.getJSONArray("todos").getJSONObject(0);
            response = sendRequest("PUT", "/todos/" + todo.getString("id"), updateData.toString());
        } else if (lastCreatedResource != null) {
            response = sendRequest("PUT", "/todos/" + lastCreatedResource.getString("id"), updateData.toString());
        }
    }

    @When("I update the todo by adding a description")
    public void updateTodoDescription() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject(currentFields);
        if (lastCreatedResource != null && lastCreatedResource.has("todos")) {
            JSONObject todo = lastCreatedResource.getJSONArray("todos").getJSONObject(0);
            response = sendRequest("PUT", "/todos/" + todo.getString("id"), updateData.toString());
        } else if (lastCreatedResource != null) {
            response = sendRequest("PUT", "/todos/" + lastCreatedResource.getString("id"), updateData.toString());
        }
    }

    @When("I attempt to update a todo that does not exist")
    public void attemptUpdateNonexistentTodo() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject(currentFields);
        response = sendRequest("PUT", "/todos/999999", updateData.toString());
    }

    // Action steps - Linking
    @When("I link the created todo to an existing project")
    public void linkTodoToProject() {
    }

    @When("I try to link a non-existent todo to a category")
    public void attemptToLinkNonexistentTodoToCategory() {
    }

    @When("I attempt to link a todo without specifying a project")
    public void linkTodoWithoutProject() {
    }

    @When("I attempt to link a todo to a category using a title instead of an ID")
    public void linkTodoToCategoryByTitle() {
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
                if (errors.getString(i).contains(expectedError)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                assertEquals(expectedError, responseObject.get("errorMessages").toString(), 
                    "Error message should contain: " + expectedError);
            }
        } else {
            assertEquals(expectedError, responseObject.get("error").toString(),
                "Error message should contain: " + expectedError);
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
    public void verifyTodoProjectLink() {
    }

    @Then("the todo should appear as linked to the category")
    public void verifyTodoCategoryLink() {
    }

    @Then("a new empty project should be created and linked to the todo")
    public void verifyEmptyProjectCreation() {
    }

    @Then("a new category should be created and linked to the todo")
    public void verifyNewCategoryLink() {
    }

    @Then("the todo's title should reflect the updated value")
    public void verifyUpdatedTitle() {
        JSONObject responseObject = new JSONObject(response.body());
        String currentTitle = currentFields.get("title");
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(currentTitle, todo.getString("title"), "Todo title should be updated");
        } else {
            assertEquals(currentTitle, responseObject.getString("title"), "Todo title should be updated");
        }
    }

    @Then("the todo's description should reflect the updated value")
    public void verifyUpdatedDescription() {
        JSONObject responseObject = new JSONObject(response.body());
        String currentDescription = currentFields.get("description");
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(currentDescription, todo.getString("description"), "Todo description should be updated");
        } else {
            assertEquals(currentDescription, responseObject.getString("description"), "Todo description should be updated");
        }
    }

    @Then("the todo's title should remain unchanged")
    public void verifyUnchangedTitle() {
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
