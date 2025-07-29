package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
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
@DynamoDBTable(tableName = "GCA_Attendance")
public class Attendance {

  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBRangeKey(attributeName = "attendanceId")
  private String attendanceId;

  @DynamoDBAttribute(attributeName = "sessionId")
  private String sessionId;

  @DynamoDBAttribute(attributeName = "studentId")
  private String studentId;

  @DynamoDBAttribute(attributeName = "isPresent")
  private Boolean isPresent;

  @DynamoDBAttribute(attributeName = "markedByCoachId")
  private String markedByCoachId;

  @DynamoDBAttribute(attributeName = "markedAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime markedAt;

  @DynamoDBAttribute(attributeName = "notes")
  private String notes;
}
