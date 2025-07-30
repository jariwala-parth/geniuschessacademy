package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.pjariwala.enums.ActionResult;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
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
@DynamoDBTable(tableName = "GCA_ActivityLogs")
public class ActivityLog {

  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBRangeKey(attributeName = "logId")
  private String logId;

  @DynamoDBAttribute(attributeName = "timestamp")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime timestamp;

  // GSI-1: Query by user ID and timestamp
  @DynamoDBIndexHashKey(attributeName = "userId", globalSecondaryIndexName = "UserIdIndex")
  private String userId;

  // GSI-2: Query by action type and timestamp
  @DynamoDBIndexHashKey(attributeName = "actionType", globalSecondaryIndexName = "ActionTypeIndex")
  @DynamoDBTypeConvertedEnum
  private ActionType actionType;

  @DynamoDBAttribute(attributeName = "userType")
  @DynamoDBTypeConvertedEnum
  private UserType userType;

  @DynamoDBAttribute(attributeName = "userName")
  private String userName;

  @DynamoDBAttribute(attributeName = "description")
  private String description;

  @DynamoDBAttribute(attributeName = "entityType")
  @DynamoDBTypeConvertedEnum
  private EntityType entityType;

  @DynamoDBAttribute(attributeName = "entityId")
  private String entityId;

  @DynamoDBAttribute(attributeName = "entityName")
  private String entityName;

  // Additional context data (JSON string)
  @DynamoDBAttribute(attributeName = "metadata")
  private String metadata;

  @DynamoDBAttribute(attributeName = "ipAddress")
  private String ipAddress;

  @DynamoDBAttribute(attributeName = "userAgent")
  private String userAgent;

  @DynamoDBAttribute(attributeName = "sessionId")
  private String sessionId;

  @DynamoDBAttribute(attributeName = "result")
  @DynamoDBTypeConvertedEnum
  private ActionResult result;

  @DynamoDBAttribute(attributeName = "errorMessage")
  private String errorMessage;
}
