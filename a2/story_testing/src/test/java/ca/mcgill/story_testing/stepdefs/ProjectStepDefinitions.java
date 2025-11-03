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

public class ProjectStepDefinitions {
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
    @Given("I have a project with title {string} and description {string}")
    public void prepareProjectWithTitleAndDescription(String title, String description) {
        Map<String, String> fields = new HashMap<>();
        fields.put("title", title);
        fields.put("description", description);
        context.setCurrentFields(fields);
    }

    @Given("I have a project with missing field {string}")
    public void prepareProjectWithMissingField(String field) {
        context.setCurrentFields(new HashMap<>());
    }

    @Given("I have a project with title {string} and the following optional fields")
    public void prepareProjectWithOptionalFields(String title, DataTable fields) {
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

    @Given("a project already exists in the system")
    public void verifyProjectExists() throws IOException, InterruptedException {
        JSONObject projectData = new JSONObject();
        projectData.put("title", "Original Title");
        projectData.put("description", "An existing project for testing");
        context.sendRequest("POST", "/projects", projectData.toString());
        assertEquals(201, context.getResponse().statusCode(), "Project should be created successfully");
        if (context.getResponse().statusCode() == 201) {
            context.setLastCreatedProject(new JSONObject(context.getResponse().body()));
        }
    }

    @Given("there are existing projects in the system")
    public void createExistingprojects() throws IOException, InterruptedException {
        JSONObject firstProject = new JSONObject();
        firstProject.put("title", "First Project");
        context.sendRequest("POST", "/projects", firstProject.toString());
        assertEquals(201, context.getResponse().statusCode(), "First Project should be created successfully");

        JSONObject secondProject = new JSONObject();
        secondProject.put("title", "Second Project");
        context.sendRequest("POST", "/projects", secondProject.toString());
        assertEquals(201, context.getResponse().statusCode(), "Second Project should be created successfully");
    }

    @Given("the system has no projects")
    public void ensureSystemHasNoprojects() {
        try {
            context.sendRequest("GET", "/projects", null);
            assertEquals(200, context.getResponse().statusCode(), "GET /projects should return 200");

            JSONObject resp = new JSONObject(context.getResponse().body());
            if (resp.has("projects")) {
                JSONArray projects = resp.getJSONArray("projects");
                for (int i = 0; i < projects.length(); i++) {
                    JSONObject project = projects.getJSONObject(i);
                    String id = project.has("id") ? project.get("id").toString() : String.valueOf(project.getInt("id"));
                    context.sendRequest("DELETE", "/projects/" + id, null);
                }

                context.sendRequest("GET", "/projects", null);
                JSONObject resp2 = new JSONObject(context.getResponse().body());
                JSONArray projects2 = resp2.getJSONArray("projects");
                assertEquals(0, projects2.length(), "Expected no projects after cleanup");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to ensure system has no projects", e);
        }
    }

    // ----------------- When -----------------
    @When("I create the project")
    public void createProject() throws IOException, InterruptedException {
        JSONObject projectData = new JSONObject();
        for (Map.Entry<String, String> entry : context.getCurrentFields().entrySet()) {
            if (entry.getKey().equals("completed") || entry.getKey().equals("active")) {
                projectData.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
            } else {
                projectData.put(entry.getKey(), entry.getValue());
            }
        }
        context.sendRequest("POST", "/projects", projectData.toString());
        if (context.getResponse().statusCode() == 201) {
            context.setLastCreatedResource(new JSONObject(context.getResponse().body()));
            context.setLastCreatedProject(new JSONObject(context.getResponse().body()));
        }
    }

    @When("I try to create the project")
    public void attemptToCreateproject() {
        JSONObject projectData = new JSONObject(context.getCurrentFields());
        try {
            context.sendRequest("POST", "/projects", projectData.toString());
            if (context.getResponse().statusCode() == 201) {
                context.setLastCreatedResource(new JSONObject(context.getResponse().body()));
                context.setLastCreatedProject(new JSONObject(context.getResponse().body()));
            } else {
                context.setLastCreatedResource(null);
                context.setLastCreatedProject(null);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to send create request", e);
        }
    }

    @When("I request all projects")
    public void requestAllProjects() throws IOException, InterruptedException {
        context.sendRequest("GET", "/projects", null);
    }

    @When("I request the project with id {int}")
    public void requestSpecificProject(int id) throws IOException, InterruptedException {
        context.sendRequest("GET", "/projects/" + id, null);
    }

    @When("I update the project with a new title")
    public void updateProjectTitle() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("title", "Updated Title");
        String projectId = context.getLastCreatedProject().getString("id");
        context.sendRequest("PUT", "/projects/" + projectId, updateData.toString());
    }

    @When("I update the project by adding a description")
    public void updateProjectDescription() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject();
        updateData.put("description", "Updated description");
        String projectId = context.getLastCreatedProject().getString("id");
        context.sendRequest("POST", "/projects/" + projectId, updateData.toString());
    }

    @When("I attempt to update a project that does not exist")
    public void attemptUpdateNonexistentProject() throws IOException, InterruptedException {
        JSONObject updateData = new JSONObject(context.getCurrentFields());
        context.sendRequest("PUT", "/projects/999999", updateData.toString());
    }

    @When("I delete the project")
    public void deleteProject() throws IOException, InterruptedException {
        String projectId = context.getLastCreatedProject().getString("id");
        context.sendRequest("DELETE", "/projects/" + projectId, null);
    }

    @When("I attempt to delete a project that does not exist")
    public void attemptDeleteNonexistentProject() throws IOException, InterruptedException {
        context.sendRequest("DELETE", "/projects/999999", null);
    }

    @When("I attempt to delete a project without Id")
    public void attemptDeleteprojectWihtoutId() throws IOException, InterruptedException {
        context.sendRequest("DELETE", "/projects", null);
    }

    // ----------------- Then -----------------
    @Then("The project should no longer exist with status {int}")
    public void verifyNonExistenceProjectStatus(int expectedStatus) throws IOException, InterruptedException {
        String projectId = context.getLastCreatedProject().optString("id", null);
        context.sendRequest("GET", "/projects/" + projectId, null);
        assertEquals(expectedStatus, context.getResponse().statusCode(), 
            "Expected status code mismatch when verifying project non-existence.");
    }

    @Then("the created project should include the title {string}")
    public void verifyProjectTitle(String expectedTitle) {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        if (responseObject.has("projects")) {
            JSONObject project = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedTitle, project.getString("title"), 
                "Project title should match the expected value");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), 
                "Project title should match the expected value");
        }
    }

    @Then("the created project should include the optional fields")
    public void verifyProjectOptionalFields() {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        JSONObject project = responseObject.has("projects") ? 
            responseObject.getJSONArray("projects").getJSONObject(0) : responseObject;
        assertTrue(project.has("completed"), "project should have completed field");
        assertTrue(project.has("active"), "project should have a status field");
    }

    @Then("the response should include a list of projects")
    public void verifyProjectList() {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        assertTrue(responseObject.has("projects"), "Response should contain projects field");
        JSONArray projects = responseObject.getJSONArray("projects");
        assertTrue(projects.length() > 0, "Response should contain at least one project");
    }

    @Then("the response should include an empty project list")
    public void verifyEmptyProjectList() {
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        assertTrue(responseObject.has("projects"), "Response should contain projects field");
        JSONArray projects = responseObject.getJSONArray("projects");
        assertEquals(0, projects.length(), "Response should contain an empty list of projects");
    }

    @Then("the project's title should reflect the updated value")
    public void verifyUpdatedProjectTitle() throws IOException, InterruptedException {
        String projectId = context.getLastCreatedProject().getString("id");
        context.sendRequest("GET", "/projects/" + projectId, null);
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        String expectedTitle = "Updated Title";
        if (responseObject.has("projects")) {
            JSONObject project = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedTitle, project.getString("title"), "Project title should be updated");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), "Project title should be updated");
        }
    }

    @Then("the project's description should reflect the updated value")
    public void verifyUpdatedProjectDescription() throws IOException, InterruptedException {
        String projectId = context.getLastCreatedProject().getString("id");
        context.sendRequest("GET", "/projects/" + projectId, null);
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        String expectedDescription = "Updated description";
        if (responseObject.has("projects")) {
            JSONObject project = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedDescription, project.getString("description"), 
                "Project description should be updated");
        } else {
            assertEquals(expectedDescription, responseObject.getString("description"), 
                "Project description should be updated");
        }
    }

    @Then("the project's title should remain unchanged")
    public void verifyUnchangedProjectTitle() throws IOException, InterruptedException {
        String projectId = context.getLastCreatedProject().getString("id");
        context.sendRequest("GET", "/projects/" + projectId, null);
        JSONObject responseObject = new JSONObject(context.getResponse().body());
        String expectedTitle = "Original Title";
        if (responseObject.has("projects")) {
            JSONObject project = responseObject.getJSONArray("projects").getJSONObject(0);
            assertEquals(expectedTitle, project.getString("title"), 
                "Project title should remain unchanged");
        } else {
            assertEquals(expectedTitle, responseObject.getString("title"), 
                "Project title should remain unchanged");
        }
    }
}