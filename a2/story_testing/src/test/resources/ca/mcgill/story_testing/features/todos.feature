Feature: Todo List Management
  As a user of the Todo List API
  I want to manage my todos effectively
  So that I can keep track of my tasks and their relationships

  Background:
    Given the Todo Manager API is running
    And the system is in a clean state

  # Story 1: Creating Todos
  Scenario Outline: Normal Flow - Successfully create new todo
    Given I have a todo with title "<title>" and description "<description>"
    When I send a POST request to "/todos"
    Then the todo response status should be 201
    And the response should contain the title "<title>"
    And the response should contain the description "<description>"

    Examples:
      | title           | description                      |
      | Buy groceries   | Get milk, eggs, and bread       |
      | Call dentist    | Schedule annual checkup         |

  Scenario Outline: Error Flow - Create todo with missing fields
    Given I have a todo with missing field "<field>"
    When I send a POST request to "/todos"
    Then the todo response status should be 400
    And the error message should indicate "<message>"

    Examples:
      | field       | message                    |
      | title      | title : field is mandatory |
      | both       | title : field is mandatory |

  Scenario Outline: Alternate Flow - Create todo with optional fields
    Given I have a todo with title "<title>" and optional fields
      | field       | value            |
      | doneStatus  | <done>           |
      | description | <description>     |
    When I send a POST request to "/todos"
    Then the todo response status should be 201
    And the response should contain the optional fields

    Examples:
      | title        | done    | description        |
      | Quick task   | true    | Optional desc      |
      | Another task | false   | Another desc       |

  # Story 2: Retrieving Todos
  Scenario: Normal Flow - Retrieve all todos
    Given there are existing todos in the system
    When I send a GET todo request to "/todos"
    Then the todo response status should be 200
    And the response should contain a list of todos

  Scenario: Error Flow - Retrieve non-existent todo
    When I send a GET todo request to "/todos/100"
    Then the todo response status should be 404
    And the error message should indicate "Could not find an instance with todos/100"

  Scenario: Alternate Flow - Retrieve todos when none exist
    Given the system has no todos
    When I send a GET todo request to "/todos"
    Then the todo response status should be 200
    And the response should contain an empty list

  # Story 3: Linking Todos to Projects
  Scenario: Normal Flow - Successfully link todo to project
    Given the system is in a clean state
    When I create a todo with title "New Todo" and description "To be linked"
    Then the todo response status should be 201
    When I send a POST request to "/todos/3/tasksof" with body id "1"
    Then the todo response status should be 201
    And the todo should be linked to the project

  Scenario: Error Flow - Link todo to invalid project
    Given the system is in a clean state
    When I create a todo with title "Another Todo" and description "To be linked"
    Then the todo response status should be 201
    When I send a POST request to "/todos/3/tasksof" with body id "999"
    Then the todo response status should be 404
    And the error message should indicate "Could not find thing matching value for id"

  Scenario: Alternate Flow - Link todo without project ID (Known Bug - Creates Empty Project)
    Given the system is in a clean state
    When I create a todo with title "Todo without project" and description "Testing empty body"
    Then the todo response status should be 201
    When I send a POST request to "/todos/3/tasksof" with empty body
    # Bug: Instead of rejecting the request, it creates a new empty project and links to it
    Then the todo response status should be 201
    And the response should contain an empty project with id "2"
    And the response should show todo "3" linked to project "2"

  # Story 4: Linking Todos to Categories
  Scenario: Normal Flow - Successfully link todo to category
    Given the system is in a clean state
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

  # Story 5: Managing Todo Completion Status
  Scenario: Normal Flow - Mark todo as done
    Given there is a todo with id "1" in the system
    When I mark the todo as "true" for completion status
    Then the todo response status should be 200
    And the todo should be marked as completed

  Scenario: Error Flow - Mark non-existent todo as done
    When I mark todo "999" as "true" for completion status
    Then the todo response status should be 404
    And the error message should indicate "not found"

  Scenario: Alternate Flow - Toggle todo completion status
    Given there is a completed todo with id "1"
    When I mark the todo as "false" for completion status
    Then the todo response status should be 200
    And the todo should be marked as not completed