Feature: Retrieve todos
  As a user of the TODO List API
  I want to retrieve existing todos or specific todo items
  So that I can view and verify my saved tasks

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  @focus
  Scenario: Normal Flow - Retrieve all todos
    Given there are existing todos in the system
    When I request all todos
    Then the operation should succeed with status 200
    And the response should include a list of todos

  @focus
  Scenario: Error Flow - Retrieve a non-existent todo
    When I request the todo with id 100
    Then the operation should fail with status 404
    And the error message should include "Could not find an instance with todos/100"

  @focus
  Scenario: Alternate Flow - Retrieve todos when none exist
    Given the system has no todos
    When I request all todos
    Then the operation should succeed with status 200
    And the response should include an empty list
