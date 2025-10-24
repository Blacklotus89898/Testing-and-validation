Feature: Test existing REST API

  Scenario: GET /api/hello returns 200
    Given the API base URL is "https://postman-echo.com"
    When I send a GET request to "/get?foo1=bar1"
    Then the response status should be 200
    And the response should contain "bar1"
