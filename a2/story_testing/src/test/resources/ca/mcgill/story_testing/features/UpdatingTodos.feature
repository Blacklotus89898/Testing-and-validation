Feature: Updating todos
  As a user of the Todo List API
  I want to update details of existing todos
  So that I can modify my tasks as needed

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state
    
  @focus
  Scenario: Normal Flow - Update the title of an existing todo
    Given a todo already exists in the system
    When I update the todo with a new title
    Then the operation should succeed with status 200
    And the todo's title should reflect the updated value

  @focus
  Scenario: Error Flow - Attempt to update a non-existent todo
    When I attempt to update a todo that does not exist
    Then the operation should fail with status 404
    And the error message should include "Invalid GUID for entity todo"

  @focus
  Scenario: Alternate Flow - Add a description to an existing todo
    Given a todo already exists in the system
    When I update the todo by adding a description
    Then the operation should succeed with status 200
    And the todo's description should reflect the updated value
    And the todo's title should remain unchanged
