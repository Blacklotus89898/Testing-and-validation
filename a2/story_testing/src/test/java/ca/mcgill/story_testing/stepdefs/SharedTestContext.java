package ca.mcgill.story_testing.stepdefs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class SharedTestContext {
    private static SharedTestContext instance;
    
    private HttpResponse<String> response;
    private final String BASE_URL = "http://localhost:4567";
    private Map<String, String> currentFields;
    private JSONObject lastCreatedResource;
    private JSONObject lastCreatedTodo;
    private JSONObject lastCreatedProject;
    private JSONObject lastCreatedCategory;
    private HttpClient httpClient;

    private SharedTestContext() {
        reset();
    }

    public static SharedTestContext getInstance() {
        if (instance == null) {
            instance = new SharedTestContext();
        }
        return instance;
    }

    public void reset() {
        httpClient = HttpClient.newHttpClient();
        currentFields = new HashMap<>();
        lastCreatedResource = null;
        lastCreatedTodo = null;
        lastCreatedProject = null;
        lastCreatedCategory = null;
    }

    public void cleanup() {
        httpClient = null;
        currentFields = null;
        lastCreatedResource = null;
        lastCreatedTodo = null;
        lastCreatedProject = null;
        lastCreatedCategory = null;
    }

    // Getters and setters
    public HttpResponse<String> getResponse() { 
        return response; 
    }

    public void setResponse(HttpResponse<String> response) { 
        this.response = response; 
    }

    public Map<String, String> getCurrentFields() { 
        return currentFields; 
    }

    public void setCurrentFields(Map<String, String> fields) { 
        this.currentFields = fields; 
    }

    public JSONObject getLastCreatedResource() { 
        return lastCreatedResource; 
    }

    public void setLastCreatedResource(JSONObject obj) { 
        this.lastCreatedResource = obj; 
    }

    public JSONObject getLastCreatedTodo() { 
        return lastCreatedTodo; 
    }

    public void setLastCreatedTodo(JSONObject obj) { 
        this.lastCreatedTodo = obj; 
    }

    public JSONObject getLastCreatedProject() { 
        return lastCreatedProject; 
    }

    public void setLastCreatedProject(JSONObject obj) { 
        this.lastCreatedProject = obj; 
    }

    public JSONObject getLastCreatedCategory() { 
        return lastCreatedCategory; 
    }

    public void setLastCreatedCategory(JSONObject obj) { 
        this.lastCreatedCategory = obj; 
    }

    // HTTP request helper
    public HttpResponse<String> sendRequest(String method, String endpoint, String body) throws IOException, InterruptedException {
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
        
        response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        return response;
    }
}