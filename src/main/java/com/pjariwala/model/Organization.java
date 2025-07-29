package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.pjariwala.util.LocalDateTimeConverter;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "GCA_Organizations")
public class Organization {

  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBAttribute(attributeName = "organizationName")
  private String organizationName;

  @DynamoDBAttribute(attributeName = "description")
  private String description;

  @DynamoDBAttribute(attributeName = "ownerId")
  @DynamoDBIndexHashKey(globalSecondaryIndexName = "ownerId-index")
  private String ownerId;

  @DynamoDBAttribute(attributeName = "contactEmail")
  private String contactEmail;

  @DynamoDBAttribute(attributeName = "contactPhone")
  private String contactPhone;

  @DynamoDBAttribute(attributeName = "address")
  private String address;

  @DynamoDBAttribute(attributeName = "timezone")
  private String timezone;

  @DynamoDBAttribute(attributeName = "isActive")
  private Boolean isActive;

  @DynamoDBAttribute(attributeName = "subscriptionPlan")
  private String subscriptionPlan;

  @DynamoDBAttribute(attributeName = "maxUsers")
  private Integer maxUsers;

  @DynamoDBAttribute(attributeName = "createdAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime createdAt;

  @DynamoDBAttribute(attributeName = "updatedAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime updatedAt;
}
