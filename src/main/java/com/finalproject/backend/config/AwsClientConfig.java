package com.finalproject.backend.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableDynamoDBRepositories("com.finalproject")
public class AwsClientConfig {

  @Bean
  @Profile("default")
  public AmazonS3 awsS3Client() {
    return AmazonS3ClientBuilder.standard().withRegion("us-west-2").build();
  }

  @Bean
  @Profile("default")
  public AmazonSimpleEmailService awsSesClient() {
    return AmazonSimpleEmailServiceClientBuilder.standard().withRegion("us-west-2").build();
  }

  @Bean
  @Profile("default")
  public AmazonDynamoDB amazonDynamoDB() {
    return AmazonDynamoDBClientBuilder.standard().withRegion("us-west-2").build();
  }

}
