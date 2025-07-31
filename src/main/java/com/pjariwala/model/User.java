package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.pjariwala.util.LocalDateTimeConverter;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "GCA_Users")
public class User {

  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBRangeKey(attributeName = "userId")
  private String userId;

  @DynamoDBAttribute(attributeName = "userType")
  private String userType; // "COACH" or "STUDENT"

  @DynamoDBAttribute(attributeName = "username")
  @DynamoDBIndexHashKey(globalSecondaryIndexName = "username-index")
  private String username;

  @DynamoDBAttribute(attributeName = "email")
  @DynamoDBIndexHashKey(globalSecondaryIndexName = "email-index")
  private String email;

  @DynamoDBAttribute(attributeName = "name")
  private String name;

  @DynamoDBAttribute(attributeName = "phoneNumber")
  private String phoneNumber;

  @DynamoDBAttribute(attributeName = "cognitoSub")
  @DynamoDBIndexHashKey(globalSecondaryIndexName = "cognitoSub-index")
  private String cognitoSub;

  @DynamoDBAttribute(attributeName = "isActive")
  private Boolean isActive;

  @DynamoDBAttribute(attributeName = "createdAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime createdAt;

  @DynamoDBAttribute(attributeName = "updatedAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime updatedAt;

  // Student-specific fields
  @DynamoDBAttribute(attributeName = "guardianName")
  private String guardianName;

  @DynamoDBAttribute(attributeName = "guardianPhone")
  private String guardianPhone;

  @DynamoDBAttribute(attributeName = "joiningDate")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime joiningDate;
}
