Feature: Linking Todos to Projects
  As a user of the Todo List API
  I want to link todos to specific projects
  So that I can organize tasks by project context

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario: Normal Flow - Successfully link todo to project
    When I create a todo with title "New Todo" and description "To be linked"
    Then the todo response status should be 201
    When I send a POST request to "/todos/3/tasksof" with body id "1"
    Then the todo response status should be 201
    And the todo should be linked to the project

  Scenario: Error Flow - Link todo to invalid project
    When I create a todo with title "Another Todo" and description "To be linked"
    Then the todo response status should be 201
    When I send a POST request to "/todos/3/tasksof" with body id "999"
    Then the todo response status should be 404
    And the error message should indicate "Could not find thing matching value for id"

  Scenario: Alternate Flow - Link todo without project ID (Known Bug - Creates Empty Project)
    When I create a todo with title "Todo without project" and description "Testing empty body"
    Then the todo response status should be 201
    When I send a POST request to "/todos/3/tasksof" with empty body
    # Bug: Instead of rejecting the request, it creates a new empty project and links to it
    Then the todo response status should be 201
    And the response should contain an empty project with id "2"
    And the response should show todo "3" linked to project "2"
