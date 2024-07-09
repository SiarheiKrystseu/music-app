package com.krystseu.microservices.resourceservice.e2e;

import com.krystseu.microservices.resourceservice.service.ResourceMessageSender;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import wiremock.com.jayway.jsonpath.JsonPath;

import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
public class UploadAudioSteps {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceMessageSender resourceMessageSender;

    private byte[] audioData;
    private String audioId;

    @Given("the user has a valid audio file")
    public void givenTheUserHasAValidAudioFile() throws Exception {
        audioData = Files.readAllBytes(Paths.get("src/test/resources/Test_Audio.mp3"));
    }

    @When("the user uploads the audio file")
    public void whenTheUserUploadsTheAudioFile() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/resources/upload")
                        .contentType("audio/mpeg")
                        .content(audioData))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        Integer id = JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Integer.class);
        audioId = String.valueOf(id); // Convert to String
    }

    @Then("the system processes the audio file")
    public void thenTheSystemProcessesTheAudioFile() {
        // This step is implicit in the "When" step, so we don't need to implement anything here.
    }

    @Then("the system returns an ID for the uploaded audio file")
    public void thenTheSystemReturnsAnIdForTheUploadedAudioFile() {
        Assertions.assertNotNull(audioId, "Expected a non-null ID for the uploaded audio file");
    }
}