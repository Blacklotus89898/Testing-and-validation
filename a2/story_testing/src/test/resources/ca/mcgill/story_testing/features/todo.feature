Feature: Todo Management API

  Scenario: Get all todos
    When I send a GET todo request to "/todos"
    Then the todo response status should be 200

  Scenario: Create a new todo
    Given I have a todo with title "Buy milk" and description "Buy 2L milk"
    When I send a POST request to "/todos"
    Then the todo response status should be 201
