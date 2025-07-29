package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.pjariwala.util.LocalDateConverter;
import com.pjariwala.util.LocalDateTimeConverter;
import com.pjariwala.util.LocalTimeConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "GCA_Sessions")
public class Session {

  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBRangeKey(attributeName = "sessionId")
  private String sessionId;

  @DynamoDBAttribute(attributeName = "batchId")
  private String batchId;

  @DynamoDBAttribute(attributeName = "sessionDate")
  @DynamoDBTypeConverted(converter = LocalDateConverter.class)
  private LocalDate sessionDate;

  @DynamoDBAttribute(attributeName = "startTime")
  @DynamoDBTypeConverted(converter = LocalTimeConverter.class)
  private LocalTime startTime;

  @DynamoDBAttribute(attributeName = "endTime")
  @DynamoDBTypeConverted(converter = LocalTimeConverter.class)
  private LocalTime endTime;

  @DynamoDBAttribute(attributeName = "status")
  @DynamoDBTypeConvertedEnum
  private SessionStatus status;

  @DynamoDBAttribute(attributeName = "coachId")
  private String coachId;

  @DynamoDBAttribute(attributeName = "notes")
  private String notes;

  @DynamoDBAttribute(attributeName = "createdAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime createdAt;

  @DynamoDBAttribute(attributeName = "updatedAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime updatedAt;

  public enum SessionStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
  }
}
