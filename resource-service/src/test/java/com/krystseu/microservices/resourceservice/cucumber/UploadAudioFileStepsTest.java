package com.krystseu.microservices.resourceservice.cucumber;

import com.krystseu.microservices.resourceservice.config.TestLocalStackConfig;
import com.netflix.discovery.DiscoveryClient;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import java.io.IOException;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TestLocalStackConfig.class})
@ActiveProfiles("test")
@CucumberContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UploadAudioFileStepsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiscoveryClient discoveryClient;

    private byte[] audioData;
    private ResultActions result;

    @Given("a valid audio file")
    public void aValidAudioFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("Test_Audio.mp3");
        audioData = StreamUtils.copyToByteArray(resource.getInputStream());
    }

    @Given("an invalid audio file")
    public void anInvalidAudioFile() {
        audioData = "mock invalid audio data".getBytes();
    }

    @When("I upload the audio file")
    public void iUploadTheAudioFile() throws Exception {
        result = mockMvc.perform(MockMvcRequestBuilders.post("/api/resources/upload")
                .contentType("audio/mpeg")
                .content(audioData));
    }

    @Then("the audio file should be stored successfully")
    public void theAudioFileShouldBeStoredSuccessfully() throws Exception {
        result.andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Then("an error should be returned")
    public void anErrorShouldBeReturned() throws Exception {
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}