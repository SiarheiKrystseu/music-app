package com.krystseu.microservices.resourceservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AwsConfig {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.endpoint}")
    private String localstackEndpoint;

    @Value("${cloud.aws.s3.staging-bucket-name}")
    private String stagingBucketName;

    @Value("${cloud.aws.s3.permanent-bucket-name}")
    private String permanentBucketName;

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(localstackEndpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withPathStyleAccessEnabled(true) // This is important for LocalStack
                .build();

        // Create the bucket if it doesn't exist
        if (!amazonS3.doesBucketExistV2(stagingBucketName)) {
            log.info("Creating {}", stagingBucketName);
            amazonS3.createBucket(stagingBucketName);
        }
        if (!amazonS3.doesBucketExistV2(permanentBucketName)) {
            log.info("Creating {}", permanentBucketName);
            amazonS3.createBucket(permanentBucketName);
        }

        return amazonS3;
    }
}
