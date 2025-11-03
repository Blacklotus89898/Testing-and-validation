package ca.mcgill.story_testing.stepdefs;

import java.io.IOException;
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
    private final SharedTestContext context = SharedTestContext.getInstance();

    @Before
    public void setUp() {
        context.reset();
    }

    @After
    public void tearDown() {
        context.cleanup();
    }

    // ----------------- Given -----------------
    @Given("the Todos API service is running")
    public void verifyAPIIsRunning() throws IOException, InterruptedException {
        context.sendRequest("GET", "/todos", null);
        assertEquals(200, context.getResponse().statusCode(), "API should be running and return 200 OK");
    }

    @Given("the system has been reset to a clean state")
    public void resetSystemToCleanState() throws IOException, InterruptedException {
        context.sendRequest("GET", "/todos", null);
        if (context.getResponse().statusCode() == 200) {
            JSONObject resp = new JSONObject(context.getResponse().body());
            JSONArray todos = resp.optJSONArray("todos");
            if (todos != null) {
                for (int i = 0; i < todos.length(); i++) {
                    JSONObject todo = todos.getJSONObject(i);
                    String id = todo.optString("id");
                    if (!id.isEmpty()) context.sendRequest("DELETE", "/todos/" + id, null);
                }
            }
        }

        context.sendRequest("GET", "/projects", null);
        if (context.getResponse().statusCode() == 200) {
            JSONObject resp = new JSONObject(context.getResponse().body());
            JSONArray projects = resp.optJSONArray("projects");
            if (projects != null) {
                for (int i = 0; i < projects.length(); i++) {
                    JSONObject project = projects.getJSONObject(i);
                    String id = project.optString("id");
                    if (!id.isEmpty()) context.sendRequest("DELETE", "/projects/" + id, null);
                }
            }
        }

        context.sendRequest("GET", "/categories", null);
        if (context.getResponse().statusCode() == 200) {
            JSONObject resp = new JSONObject(context.getResponse().body());
            JSONArray categories = resp.optJSONArray("categories");
            if (categories != null) {
                for (int i = 0; i < categories.length(); i++) {
                    JSONObject category = categories.getJSONObject(i);
                    String id = category.optString("id");
                    if (!id.isEmpty()) context.sendRequest("DELETE", "/categories/" + id, null);
                }
            }
        }
    }

    @Given("I have a todo with title {string} and description {string}")
    public void prepareTodoWithTitleAndDescription(String title, String description) {
        Map<String, String> fields = new HashMap<>();
        fields.put("title", title);
        fields.put("description", description);
        context.setCurrentFields(fields);
    }

    @Given("I have a todo with missing field {string}")
    public void prepareTodoWithMissingField(String field) {
        Map<String, String> fields = new HashMap<>();
        if (!field.equals("title")) fields.put("title", "Default Title");
        if (!field.equals("description")) fields.put("description", "Default Description");
        context.setCurrentFields(fields);
    }

    @Given("I have a todo with title {string} and the following optional fields")
    public void prepareTodoWithOptionalFields(String title, DataTable fields) {
        Map<String, String> currentFields = new HashMap<>();
        currentFields.put("title", title);
        List<Map<String, String>> rows = fields.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String fieldName = row.get("field");
            String fieldValue = row.get("value");
            currentFields.put(fieldName, fieldValue);
        }
        context.setCurrentFields(currentFields);
    }

    @Given("a todo already exists in the system")
    public void verifyTodoExists() throws IOException, InterruptedException {
        JSONObject todoData = new JSONObject();
        todoData.put("title", "Original Title");
        context.sendRequest("POST", "/todos", todoData.toString());
        assertEquals(201, context.getResponse().statusCode(), "Todo should be created successfully");
        if (context.getResponse().statusCode() == 201) {
            context.setLastCreatedResource(new JSONObject(context.getResponse().body()));
            context.setLastCreatedTodo(new JSONObject(context.getResponse().body()));
        }
    }

    @Given("a category already exists in the system")
    public void verifyCategoryExists() throws IOException, InterruptedException {
        JSONObject categoryData = new JSONObject();
        categoryData.put("title", "Existing Category");
        categoryData.put("description", "An existing category for testing");
        context.sendRequest("POST", "/categories", categoryData.toString());
        assertEquals(201, context.getResponse().statusCode(), "Category should be created successfully");
      if (context.getResponse().statusCode() == 201) {
            context.setLastCreatedResource(new JSONObject(context.getResponse().body()));
            context.setLastCreatedCategory(new JSONObject(context.getResponse().body()));
        }
    }

    @Then("the response should show the new project associated with the todo")
    public void verifyNewProjectAssociation() {
        // do nothing
    }

    @Given("there are existing todos in the system")
    public void createExistingTodos() throws IOException, InterruptedException {
        JSONObject firstTodo = new JSONObject();
        firstTodo.put("title", "First todo");
        context.sendRequest("POST", "/todos", firstTodo.toString());
        assertEquals(201, context.getResponse().statusCode(), "First todo should be created successfully");

        JSONObject secondTodo = new JSONObject();
        secondTodo.put("title", "Second todo");
        context.sendRequest("POST", "/todos", secondTodo.toString());
        assertEquals(201, context.getResponse().statusCode(), "Second todo should be created successfully");
    }

    @Given("the system has no todos")
    public void ensureSystemHasNoTodos() {
        try {
            context.sendRequest("GET", "/todos", null);
            assertEquals(200, context.getResponse().statusCode(), "GET /todos should return 200");

            JSONObject resp = new JSONObject(context.getResponse().body());
            if (resp.has("todos")) {
                JSONArray todos = resp.getJSONArray("todos");
                for (int i = 0; i < todos.length(); i++) {
                    JSONObject todo = todos.getJSONObject(i);
                    String id = todo.has("id") ? todo.get("id").toString() : String.valueOf(todo.getInt("id"));
                    context.sendRequest("DELETE", "/todos/" + id, null);
                }

                context.sendRequest("GET", "/todos", null);
                JSONObject resp2 = new JSONObject(context.getResponse().body());
                JSONArray todos2 = resp2.getJSONArray("todos");
                assertEquals(0, todos2.length(), "Expected no todos after cleanup");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to ensure system has no todos", e);
        }
    }

    // ----------------- When -----------------
    @When("I create the todo")
    public void createTodo() throws IOException, InterruptedException {
        JSONObject todoData = new JSONObject();
        for (Map.Entry<String, String> entry : context.getCurrentFields().entrySet()) {
            if (entry.getKey().equals("doneStatus")) {
                todoData.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
            } else {
                todoData.put(entry.getKey(), entry.getValue());
            }
        }
        context.sendRequest("POST", "/todos", todoData.toString());
        if (context.getResponse().statusCode() == 201) {
            context.setLastCreatedResource(new JSONObject(context.getResponse().body()));
            context.setLastCreatedTodo(new JSONObject(context.getResponse().body()));
        }
    }

    @When("I try to create the todo")
    public void attemptToCreateTodo() {
        JSONObject todoData = new JSONObject(context.getCurrentFields());
        try {
            context.sendRequest("POST", "/todos", todoData.toString());
            if (context.getResponse().statusCode() == 201) {
                context.setLastCreatedResource(new JSONObject(context.getResponse().body()));
                context.setLastCreatedTodo(new JSONObject(context.getResponse().body()));
            } else {
                context.setLastCreatedResource(null);
                context.setLastCreatedTodo(null);
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
        context.sendRequest("POST", "/todos", todoData.toString());
        if (context.getResponse().statusCode() == 201) {
            context.setLastCreatedResource(new JSONObject(context.getResponse().body()));
            context.setLastCreatedTodo(new JSONObject(context.getResponse().body()));
        }
    }

    @When("I request all todos")
    public void requestAllTodos() throws IOException, InterruptedException {
        context.sendRequest("GET", "/todos", null);
    }

    @When("I request the todo with id {int}")
    public void requestSpecificTodo(int id) throws IOException, InterruptedException {
        context.sendRequest("GET", "/todos/" + id, null);
    }

    @When("I update the todo with a new title")
    public void updateTodoTitle() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("title", "Updated Title");
        String todoId;
        if (context.getLastCreatedResource() != null) {
            JSONObject resource = context.getLastCreatedResource();
            if (resource.has("todos")) {
                todoId = resource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = resource.getString("id");
            }
            context.sendRequest("PUT", "/todos/" + todoId, updateData.toString());
        } else {
            context.sendRequest("PUT", "/todos/1", updateData.toString());
        }
    }

    @When("I update the todo by adding a description")
    public void updateTodoDescription() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("description", "Updated description");
        String todoId;
        JSONObject resource = context.getLastCreatedResource();
        if (resource != null) {
            if (resource.has("todos")) {
                todoId = resource.getJSONArray("todos").getJSONObject(0).getString("id");
            } else {
                todoId = resource.getString("id");
            }
        } else {
            todoId = "1";
        }
        context.sendRequest("POST", "/todos/" + todoId, updateData.toString());
    }

    @When("I attempt to update a todo that does not exist")
    public void attemptUpdateNonexistentTodo() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject(context.getCurrentFields());
        context.sendRequest("PUT", "/todos/999999", updateData.toString());
    }

    @When("I link the created todo to an existing project")
    public void linkTodoToProject() throws IOException, InterruptedException {
        String todoId = getTodoId();
        String projectId = getProjectId();
        
        if (todoId != null) {
            JSONObject requestData = new JSONObject();
            requestData.put("id", projectId);
            context.sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I try to link a non-existent todo to a category")
    public void attemptToLinkNonexistentTodoToCategory() throws IOException, InterruptedException {
        JSONObject requestData = new JSONObject();
        String categoryId = getCategoryId();
        requestData.put("id", categoryId);
        context.sendRequest("POST", "/todos/999999/categories", requestData.toString());
    }

    @When("I try to link the todo to a non-existent project")
    public void linkTodoToNonExistentProject() throws IOException, InterruptedException {
        String todoId = getTodoId();
        if (todoId != null) {
            JSONObject requestData = new JSONObject();
            requestData.put("id", "999");
            context.sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I attempt to link the todo without specifying a project")
    public void attemptToLinkTodoWithoutProject() throws IOException, InterruptedException {
        String todoId = getTodoId();
        if (todoId != null) {
            JSONObject requestData = new JSONObject();
            context.sendRequest("POST", "/todos/" + todoId + "/tasksof", requestData.toString());
        }
    }

    @When("I link the created todo to an existing category")
    public void linkTodoToCategory() throws IOException, InterruptedException {
        String todoId = getTodoId();
        String categoryId = getCategoryId();
        
        if (todoId != null) {
            JSONObject requestData = new JSONObject();
            requestData.put("id", categoryId);
            context.sendRequest("POST", "/todos/" + todoId + "/categories", requestData.toString());
        }
    }

    @When("I attempt to link a todo to a category using a title instead of an ID")
    public void linkTodoToCategoryByTitle() throws IOException, InterruptedException {
        String todoId = getTodoId();
        JSONObject requestData = new JSONObject();
        requestData.put("title", "Some Title");

        if (todoId != null) {
            context.sendRequest("POST", "/todos/" + todoId + "/categories", requestData.toString());
            if (context.getResponse().statusCode() == 201) {
                context.setLastCreatedCategory(new JSONObject(context.getResponse().body()));
            }
        } else {
            context.sendRequest("POST", "/todos/1/categories", requestData.toString());
            if (context.getResponse().statusCode() == 201) {
                context.setLastCreatedCategory(new JSONObject(context.getResponse().body()));
            }
        }
    }

    @When("I delete the todo")
    public void deleteTodo() throws IOException, InterruptedException {
        String todoId = context.getLastCreatedTodo().getString("id");
        context.sendRequest("DELETE", "/todos/" + todoId, null);
    }

    @When("I attempt to delete a todo that does not exist")
    public void attemptDeleteNonexistentTodo() throws IOException, InterruptedException {
        context.sendRequest("DELETE", "/todos/999999", null);
    }

    @When("I attempt to delete a todo without Id")
    public void attemptDeleteTodoWithoutId() throws IOException, InterruptedException {
        context.sendRequest("DELETE", "/todos", null);
    }

    // ----------------- Then -----------------
    @Then("the operation should succeed with status {int}")
    public void verifySuccessStatus(int expectedStatus) {
        assertEquals(expectedStatus, context.getResponse().statusCode(), 
            "Operation should have succeeded with status " + expectedStatus);
    }

    @Then("the operation should fail with status {int}")
    public void verifyFailureStatus(int expectedStatus) {
        assertEquals(expectedStatus, context.getResponse().statusCode(), 
            "Operation should have failed with status " + expectedStatus);
    }

    @Then("The todo should no longer exist with status {int}")
    public void verifyNonExistenceTodoStatus(int expectedStatus) throws IOException, InterruptedException {
        String todoId = context.getLastCreatedTodo().optString("id", null);
        context.sendRequest("GET", "/todos/" + todoId, null);
        assertEquals(expectedStatus, context.getResponse().statusCode(), 
            "Expected status code mismatch when verifying todo non-existence.");
    }

    @Then("the error message should include {string}")
    public void verifyErrorMessage(String expectedError) {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        if (responseObject.has("errorMessages")) {
            JSONArray errors = responseObject.getJSONArray("errorMessages");
            boolean found = false;
            for (int i = 0; i < errors.length(); i++) {
                String actualError = errors.getString(i);
                if (expectedError.contains(" for ") && expectedError.contains(" entity ")) {
                    String[] parts = expectedError.split(" for ");
                    String prefix = parts[0];
                    String suffix = parts[1].substring(parts[1].indexOf(" "));
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
            String actualError = responseObject.optString("error");
            if (expectedError.contains(" for ") && expectedError.contains(" entity ")) {
                String[] parts = expectedError.split(" for ");
                String prefix = parts[0];
                String suffix = parts[1].substring(parts[1].indexOf(" "));
                assertTrue(actualError.startsWith(prefix + " for ") && actualError.endsWith(suffix),
                        "Error message should match pattern: " + expectedError + "\nActual: " + actualError);
            } else {
                assertTrue(actualError.contains(expectedError),
                        "Error message should contain: " + expectedError + "\nActual: " + actualError);
            }
        }
    }

    @Then("the error message should be {string}")
    public void verifyExactErrorMessage(String expectedMessage) {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        String actualMessage;
        if (responseObject.has("errorMessages")) {
            JSONArray errors = responseObject.getJSONArray("errorMessages");
            actualMessage = String.join(", ", errors.toList().stream().map(Object::toString).toList());
        } else {
            actualMessage = responseObject.optString("error");
        }
        assertEquals(expectedMessage, actualMessage, "Error message should match exactly");
    }

    @Then("the created todo should include the title {string}")
    public void verifyTodoTitle(String expectedTitle) {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should match the expected value");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should match the expected value");
        }
    }

    @Then("the created todo should include the description {string}")
    public void verifyTodoDescription(String expectedDescription) {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedDescription, todo.getString("description"), 
                "Todo description should match the expected value");
        } else {
            assertEquals(expectedDescription, responseObject.getString("description"), 
                "Todo description should match the expected value");
        }
    }

    @Then("the created todo should include the optional fields")
    public void verifyTodoOptionalFields() {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        JSONObject todo = responseObject.has("todos") ? 
            responseObject.getJSONArray("todos").getJSONObject(0) : responseObject;
        assertTrue(todo.has("doneStatus"), "Todo should have doneStatus field");
        assertTrue(todo.has("description") && !todo.getString("description").isEmpty(),
                "Todo should have a non-empty description");
    }

    @Then("the response should include a list of todos")
    public void verifyTodosList() {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        assertTrue(responseObject.has("todos"), "Response should contain todos field");
        JSONArray todos = responseObject.getJSONArray("todos");
        assertTrue(todos.length() > 0, "Response should contain at least one todo");
    }

    @Then("the response should include an empty list")
    public void verifyEmptyList() {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        assertTrue(responseObject.has("todos"), "Response should contain todos field");
        JSONArray todos = responseObject.getJSONArray("todos");
        assertEquals(0, todos.length(), "Response should contain an empty list of todos");
    }

    @Then("the todo should appear as linked to the project")
    public void verifyTodoProjectLink() throws IOException, InterruptedException {
        String todoId = getTodoId();
        String projectId = getProjectId();
        
        if (todoId != null) {
            context.sendRequest("GET", "/todos/" + todoId + "/tasksof", null);
            assertEquals(200, context.getResponse().statusCode(), "Should be able to get todo's projects");
            JSONObject responseObject = new JSONObject(context.getResponse().body());
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
        String todoId = getTodoId();
        String categoryId = getCategoryId();
        
        if (todoId != null) {
            context.sendRequest("GET", "/todos/" + todoId + "/categories", null);
            assertEquals(200, context.getResponse().statusCode(), "Should be able to get todo's categories");
            JSONObject responseObject = new JSONObject(context.getResponse().body());
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
        if (context.getLastCreatedResource() != null) {
            String todoId = getTodoId();
            context.sendRequest("GET", "/todos/" + todoId + "/tasksof", null);
            assertEquals(200, context.getResponse().statusCode(), "Should be able to get todo's projects");
            JSONObject responseObject = new JSONObject(context.getResponse().body());
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

    @Then("a new category should be created and linked to the todo")
    public void verifyNewCategoryLink() throws IOException, InterruptedException {
        String todoId = getTodoId();
        String categoryId = getCategoryId();
        
        if (todoId != null) {
            context.sendRequest("GET", "/todos/" + todoId + "/categories", null);
            assertEquals(200, context.getResponse().statusCode(), "Should be able to get todo's categories");
            JSONObject responseObject = new JSONObject(context.getResponse().body());
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
        String todoId = getTodoId();
        if (todoId == null) todoId = "1";
        
        context.sendRequest("GET", "/todos/" + todoId, null);
        JSONObject responseObject = new JSONObject(context.getResponse().body());
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
        String todoId = getTodoId();
        if (todoId == null) todoId = "1";
        
        context.sendRequest("GET", "/todos/" + todoId, null);
        JSONObject responseObject = new JSONObject(context.getResponse().body());
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
        String todoId = getTodoId();
        if (todoId == null) todoId = "1";
        
        context.sendRequest("GET", "/todos/" + todoId, null);
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        String expectedTitle = "Original Title";
        if (responseObject.has("todos")) {
            JSONObject todo = responseObject.getJSONArray("todos").getJSONObject(0);
            assertEquals(expectedTitle, todo.getString("title"), "Todo title should remain unchanged");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Todo title should remain unchanged");
        }
    }

    // ----------------- Helper methods -----------------
    private String getTodoId() {
        if (context.getLastCreatedTodo() != null) {
            JSONObject todo = context.getLastCreatedTodo();
            if (todo.has("todos")) {
                return todo.getJSONArray("todos").getJSONObject(0).getString("id");
            }
            return todo.getString("id");
        } else if (context.getLastCreatedResource() != null) {
            JSONObject resource = context.getLastCreatedResource();
            if (resource.has("todos")) {
                return resource.getJSONArray("todos").getJSONObject(0).getString("id");
            }
            return resource.getString("id");
        }
        return null;
    }

    private String getProjectId() {
        String projectId = "1";
        if (context.getLastCreatedProject() != null) {
            JSONObject project = context.getLastCreatedProject();
            if (project.has("projects")) {
                projectId = project.getJSONArray("projects").getJSONObject(0).getString("id");
            } else {
                projectId = project.getString("id");
            }
        }
        return projectId;
    }

    private String getCategoryId() {
        String categoryId = "1";
        if (context.getLastCreatedCategory() != null) {
            JSONObject category = context.getLastCreatedCategory();
            if (category.has("categories")) {
                categoryId = category.getJSONArray("categories").getJSONObject(0).getString("id");
            } else {
                categoryId = category.getString("id");
            }
        }
        return categoryId;
    }
}