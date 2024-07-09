Feature: Upload audio file

  Scenario: User uploads a valid audio file
    Given the user has a valid audio file
    When the user uploads the audio file
    Then the system processes the audio file
    And the system returns an ID for the uploaded audio file