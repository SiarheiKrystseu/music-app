Feature: Upload Audio File
  As a user
  I want to upload audio files
  So that I can use them in my application

  Scenario: Upload a valid audio file
    Given a valid audio file
    When I upload the audio file
    Then the audio file should be stored successfully

  Scenario: Upload an invalid audio file
    Given an invalid audio file
    When I upload the audio file
    Then an error should be returned