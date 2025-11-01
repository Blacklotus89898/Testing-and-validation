Feature: Delete todos
  As a user of the TODO List API
  I want to delete todos with 
  So that I can manage and track my tasks effectively

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario Outline: Normal Flow - Successfully delete a an existing todo
    Given a todo already exists in the system
    When I delete the todo
    Then the operation should succeed with status 200
    And The todo should no longer exist with status 404

  Scenario Outline: Error Flow - Attempt to delete a non-existent todo
    When I attempt to delete a todo that does not exist
    Then the operation should fail with status 404
    And the error message should include "Could not find any instances with todos"

  Scenario Outline: Alternate Flow - Attempt to delete todo without id
    When I attempt to delete a todo without Id
    Then the operation should fail with status 405