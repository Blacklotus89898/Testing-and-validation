package ca.mcgill.story_testing.stepdefs;


import io.cucumber.java.en.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.http.*;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;
import java.io.IOException;

public class TodoStepDefinitions {

    private HttpResponse<String> response;
    private String requestBody;

    @When("I send a GET todo request to {string}")
    public void i_send_a_get_request_to(String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:4567" + endpoint))
                .GET()
                .build();
        response = client.send(request, BodyHandlers.ofString());
    }

    @Then("the todo response status should be {int}")
    public void the_response_status_should_be(Integer statusCode) {
        assertEquals(statusCode.intValue(), response.statusCode());
    }

    @Given("I have a todo with title {string} and description {string}")
    public void i_have_a_todo_with_title_and_description(String title, String description) {
        // Simple JSON payload
        requestBody = String.format("{\"title\":\"%s\", \"description\":\"%s\"}", title, description);
    }

    @When("I send a POST request to {string}")
    public void i_send_a_post_request_to(String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:4567" + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        response = client.send(request, BodyHandlers.ofString());
    }
}
 
