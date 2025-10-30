Feature: Linking Todos to Categories
  As a user of the Todo List API
  I want to associate todos with categories
  So that I can better organize and filter my tasks

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario: Normal Flow - Successfully link todo to category
    When I send a POST todo request to "/todos/1/categories" with body id "2"
    Then the todo response status should be 201
    And the todo should be linked to the category

  Scenario: Error Flow - Link non-existent todo to category
    When I send a POST todo request to "/todos/999/categories"
    Then the todo response status should be 404
    And the error message should indicate "Could not find parent thing for relationship todos/999/categories"

  Scenario: Alternate Flow - Link todo with category title instead of ID (Known Bug)
    When I send a POST todo request to "/todos/1/categories"
    And the body has title "Some title"
    Then the todo response status should be 201
    And the response should show todo "1" linked to category "3"
    # Bug: API accepts title instead of ID and creates an unintended category
