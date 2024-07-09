package com.krystseu.microservices.resourceservice.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;

@RunWith(Cucumber.class)
@ActiveProfiles("test")
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.krystseu.microservices.resourceservice.cucumber",
        plugin = {"pretty", "html:target/cucumber-reports.html"},
        monochrome = true
)
public class CucumberRunnerTest {
}
