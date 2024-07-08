package com.krystseu.microservices.resourceservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration
@Profile("test")
public class TestLocalStackConfig {

    public static LocalStackContainer localStackContainer;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    static {
        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
                .withServices(LocalStackContainer.Service.S3)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(1));

        localStackContainer.start();
        String debugPoint = "";
        // Add a JVM shutdown hook to ensure that the container is stopped when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(localStackContainer::stop));
    }

    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                TestLocalStackConfig.localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                                "us-east-1"
                        )
                )
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .build();

        // Create the bucket if it doesn't exist
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }

        return amazonS3;
    }
}