Feature: Retrieve todos
  As a user of the TODO List API
  I want to retrieve existing todos or specific todo items
  So that I can view and verify my saved tasks

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario: Normal Flow - Retrieve all todos
    Given there are existing todos in the system
    When I send a GET request to "/todos"
    Then the response status should be 200
    And the response should contain a list of todos

  Scenario: Error Flow - Retrieve a non-existent todo
    When I send a GET request to "/todos/100"
    Then the response status should be 404
    And the error message should contain "Could not find an instance with todos/100"

  Scenario: Alternate Flow - Retrieve todos when none exist
    Given the system has no todos
    When I send a GET request to "/todos"
    Then the response status should be 200
    And the response should contain an empty list
