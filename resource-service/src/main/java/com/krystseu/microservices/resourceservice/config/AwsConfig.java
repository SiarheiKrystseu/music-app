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
    private String s3Endpoint;

    @Value("${cloud.aws.s3.staging-bucket-name}")
    private String stagingBucketName;

    @Value("${cloud.aws.s3.permanent-bucket-name}")
    private String permanentBucketName;

    @Value("${cloud.aws.s3.stub-staging-bucket-name}")
    private String stubStagingBucketName;

    @Value("${cloud.aws.s3.stub-permanent-bucket-name}")
    private String stubPermanentBucketName;

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withPathStyleAccessEnabled(true) // This is important for LocalStack
                .build();

        // Create buckets if they don't exist
        createBucketIfNotExists(amazonS3, stagingBucketName);
        createBucketIfNotExists(amazonS3, permanentBucketName);
        createBucketIfNotExists(amazonS3, stubStagingBucketName);
        createBucketIfNotExists(amazonS3, stubPermanentBucketName);

        return amazonS3;
    }

    private void createBucketIfNotExists(AmazonS3 amazonS3, String bucketName) {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            log.info("Creating bucket: {}", bucketName);
            amazonS3.createBucket(bucketName);
        }
    }
}

