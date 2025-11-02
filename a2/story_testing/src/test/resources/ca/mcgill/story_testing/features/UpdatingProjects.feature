Feature: Updating projects
  As a user of the Todo List API
  I want to update details of existing projects
  So that I can modify my tasks as needed

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state
    
  Scenario: Normal Flow - Update the title of an existing project
    Given a project already exists in the system
    When I update the project with a new title
    Then the operation should succeed with status 200
    And the project's title should reflect the updated value

  Scenario: Error Flow - Attempt to update a non-existent project
    When I attempt to update a project that does not exist
    Then the operation should fail with status 404
    And the error message should include "Invalid GUID for entity project"

  Scenario: Alternate Flow - Add a description to an existing project
    Given a project already exists in the system
    When I update the project by adding a description
    Then the operation should succeed with status 200
    And the project's description should reflect the updated value
    And the project's title should remain unchanged
