Feature: Delete projects
  As a user of the TODO List API
  I want to delete projects with 
  So that I can manage and track my tasks effectively

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario Outline: Normal Flow - Successfully delete a an existing project
    Given a project already exists in the system
    When I delete the project
    Then the operation should succeed with status 200
    And The project should no longer exist with status 404

  Scenario Outline: Error Flow - Attempt to delete a non-existent project
    When I attempt to delete a project that does not exist
    Then the operation should fail with status 404
    And the error message should include "Could not find any instances with projects"

  Scenario Outline: Alternate Flow - Attempt to delete project without id
    When I attempt to delete a project without Id
    Then the operation should fail with status 405