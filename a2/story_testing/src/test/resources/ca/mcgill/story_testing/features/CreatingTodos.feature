Feature: Create new todos
  As a user of the TODO List API
  I want to create new todos with various field combinations
  So that I can track tasks effectively

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario Outline: Normal Flow - Successfully create a new todo
    Given I have a todo with title "<title>" and description "<description>"
    When I send a POST request to "/todos"
    Then the response status should be 201
    And the response should contain the title "<title>"
    And the response should contain the description "<description>"

    Examples:
      | title         | description                  |
      | Buy groceries | Get milk, eggs, and bread    |
      | Call dentist  | Schedule annual checkup      |

  Scenario Outline: Error Flow - Attempt to create a todo with missing required fields
    Given I have a todo with missing field "<field>"
    When I send a POST request to "/todos"
    Then the response status should be 400
    And the error message should contain "<message>"

    Examples:
      | field  | message                     |
      | title  | title : field is mandatory  |
      | both   | title : field is mandatory  |

  Scenario Outline: Alternate Flow - Create a todo with optional fields
    Given I have a todo with title "<title>" and optional fields
      | field       | value          |
      | doneStatus  | <done>         |
      | description | <description>  |
    When I send a POST request to "/todos"
    Then the response status should be 201
    And the response should include the optional fields

    Examples:
      | title        | done  | description     |
      | Quick task   | true  | Optional desc   |
      | Another task | false | Another desc    |
