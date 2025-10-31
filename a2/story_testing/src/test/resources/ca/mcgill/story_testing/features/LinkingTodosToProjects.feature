Feature: Linking todos to projects
  As a user of the Todo List API
  I want to link todos to specific projects
  So that I can organize my tasks by project context

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario: Normal Flow - Successfully link a todo to a project
    Given a project already exists in the system
    When I create a todo with title "New Todo" and description "To be linked"
    Then the operation should succeed with status 201
    When I link the created todo to an existing project
    Then the operation should succeed with status 201
    And the todo should appear as linked to the project

  Scenario: Error Flow - Attempt to link a todo to an invalid project
    When I create a todo with title "New Todo" and description "To be linked"
    Then the operation should succeed with status 201
    When I try to link the todo to a non-existent project
    Then the operation should fail with status 404
    And the error message should include "Could not find thing matching value for id"

  Scenario: Alternate Flow - Link todo without specifying a project (Known Bug)
    When I create a todo with title "New Todo" and description "To be linked"
    Then the operation should succeed with status 201
    When I attempt to link the todo without specifying a project
    Then the operation should succeed with status 201
    And a new empty project should be created and linked to the todo
    And the response should show the new project associated with the todo
    # Known Bug: The system should reject this request, but instead creates a new project automatically.
