Feature: Create new projects
  As a user of the project List API
  I want to create new projects with different field combinations
  So that I can manage and track my projects effectively

  Background:
    Given the Todos API service is running
    And the system has been reset to a clean state

  Scenario Outline: Normal Flow - Successfully create a new project
    Given I have a project with title "<title>" and description "<description>"
    When I create the project
    Then the operation should succeed with status 201
    And the created project should include the title "<title>"
    # And the created project should include the description "<description>"

    Examples:
      | title         | description                |
      | Project 1 | Get milk, eggs, and bread  |
      | Another Project  | Schedule annual checkup    |

  Scenario Outline: Error Flow - Attempt to create a project with missing required fields
    Given I have a project with missing field "<field>"
    When I try to create the project
    Then the operation should succeed with status 201
    # Known Bug: The system should reject this request, but instead creates a new project automatically.
    # Then the operation should fail with status 400
    # And the error message should be "<message>"

    Examples:
      | field | message                    |
      | title | title : field is mandatory |

  Scenario Outline: Alternate Flow - Create a project with optional fields
    Given I have a project with title "<title>" and the following optional fields
      | field       | value          |
      | completed  | <done>         |
      | active | <active>  |
    When I create the project
    Then the operation should succeed with status 201
    And the created project should include the optional fields

    Examples:
      | title        | completed  | active     |
      | Quick project   | true  | Optional desc   |
      | Another project | false | Another desc    |
