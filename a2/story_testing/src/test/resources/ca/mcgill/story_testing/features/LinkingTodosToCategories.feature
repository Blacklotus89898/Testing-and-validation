Feature: Linking todos to categories
  As a user of the Todo List API
  I want to associate todos with categories
  So that I can better organize and filter my tasks

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  @focus
  Scenario: Normal Flow - Successfully link a todo to a category
    Given a category already exists in the system
    When I create a todo with title "New Todo" and description "To be linked"
    Then the operation should succeed with status 201
    When I link the created todo to an existing category
    Then the operation should succeed with status 201
    And the todo should appear as linked to the category

  @focus
  Scenario: Error Flow - Attempt to link a non-existent todo to a category
    Given a category already exists in the system
    When I try to link a non-existent todo to a category
    Then the operation should fail with status 404
    And the error message should include "Could not find parent thing for relationship"

  @focus
  Scenario: Alternate Flow - Link todo using category title instead of ID (Known Bug)
    Given a todo already exists in the system
    When I attempt to link a todo to a category using a title instead of an ID
    Then the operation should succeed with status 201
    And a new category should be created and linked to the todo
    # Known Bug: The API incorrectly allows linking by title, creating an unintended category instead of returning an error.
