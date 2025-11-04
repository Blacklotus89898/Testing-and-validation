Feature: Retrieve projects
  As a user of the project List API
  I want to retrieve existing projects or specific project items
  So that I can view and verify my saved tasks

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario: Normal Flow - Retrieve all projects
    Given there are existing projects in the system
    When I request all projects
    Then the operation should succeed with status 200
    And the response should include a list of projects

  Scenario: Error Flow - Retrieve a non-existent project
    When I request the project with id 100
    Then the operation should fail with status 404
    And the error message should include "Could not find an instance with projects/100"

  Scenario: Alternate Flow - Retrieve projects when none exist
    Given the system has no projects
    When I request all projects
    Then the operation should succeed with status 200
    And the response should include an empty project list
