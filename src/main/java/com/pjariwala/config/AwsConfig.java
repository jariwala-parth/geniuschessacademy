package com.pjariwala.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

  @Value("${aws.region:us-east-1}")
  private String awsRegion;

  @Value("${aws.accessKeyId:}")
  private String accessKeyId;

  @Value("${aws.secretKey:}")
  private String secretKey;

  @Bean
  public AmazonDynamoDB amazonDynamoDB() {
    AmazonDynamoDBClientBuilder builder =
        AmazonDynamoDBClientBuilder.standard().withRegion(awsRegion);

    // If access key and secret are provided, use them (for local development)
    if (accessKeyId != null
        && !accessKeyId.isEmpty()
        && secretKey != null
        && !secretKey.isEmpty()) {
      BasicAWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretKey);
      builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
    }

    return builder.build();
  }

  @Bean
  public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
    return new DynamoDBMapper(amazonDynamoDB);
  }

  @Bean
  public AWSCognitoIdentityProvider awsCognitoIdentityProvider() {
    AWSCognitoIdentityProviderClientBuilder builder =
        AWSCognitoIdentityProviderClientBuilder.standard().withRegion(awsRegion);

    // If access key and secret are provided, use them (for local development)
    if (accessKeyId != null
        && !accessKeyId.isEmpty()
        && secretKey != null
        && !secretKey.isEmpty()) {
      BasicAWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretKey);
      builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
    }

    return builder.build();
  }
}
