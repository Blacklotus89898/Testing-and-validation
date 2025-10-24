package ca.mcgill.story_testing.stepdefs;

import io.cucumber.java.en.*;
import io.restassured.response.Response;

import static io.restassured.RestAssured.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ApiStepDefinitions {

    private String baseUrl;
    private Response response;

    @Given("the API base URL is {string}")
    public void setBaseUrl(String url) {
        this.baseUrl = url;
    }

    @When("I send a GET request to {string}")
    public void sendGetRequest(String path) {
        response = get(baseUrl + path);
    }

    @Then("the response status should be {int}")
    public void checkStatusCode(int status) {
        assertThat(response.getStatusCode(), equalTo(status));
    }

    @Then("the response should contain {string}")
    public void checkResponseContains(String text) {
        assertThat(response.getBody().asString(), containsString(text));
    }
}
