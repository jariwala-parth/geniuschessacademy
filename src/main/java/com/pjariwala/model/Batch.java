package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.pjariwala.enums.BatchStatus;
import com.pjariwala.enums.OccurrenceType;
import com.pjariwala.enums.PaymentType;
import com.pjariwala.util.LocalDateConverter;
import com.pjariwala.util.LocalDateTimeConverter;
import com.pjariwala.util.LocalTimeConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "GCA_Batches")
public class Batch {

  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBRangeKey(attributeName = "batchId")
  private String batchId;

  @DynamoDBAttribute(attributeName = "batchName")
  private String batchName;

  @DynamoDBAttribute(attributeName = "batchSize")
  private Integer batchSize;

  @DynamoDBAttribute(attributeName = "startDate")
  @DynamoDBTypeConverted(converter = LocalDateConverter.class)
  private LocalDate startDate;

  @DynamoDBAttribute(attributeName = "endDate")
  @DynamoDBTypeConverted(converter = LocalDateConverter.class)
  private LocalDate endDate;

  @DynamoDBAttribute(attributeName = "batchTiming")
  private BatchTiming batchTiming;

  @DynamoDBAttribute(attributeName = "paymentType")
  @DynamoDBTypeConvertedEnum
  private PaymentType paymentType;

  // Applicable if paymentType is FIXED_MONTHLY
  @DynamoDBAttribute(attributeName = "fixedMonthlyFee")
  private Double fixedMonthlyFee;

  // Applicable if paymentType is PER_SESSION or PER_ATTENDANCE
  @DynamoDBAttribute(attributeName = "perSessionFee")
  private Double perSessionFee;

  @DynamoDBAttribute(attributeName = "occurrenceType")
  @DynamoDBTypeConvertedEnum
  private OccurrenceType occurrenceType;

  @DynamoDBAttribute(attributeName = "batchStatus")
  @DynamoDBIndexHashKey(
      globalSecondaryIndexName = "batchStatus-startDate-index",
      attributeName = "batchStatus")
  @DynamoDBTypeConvertedEnum
  private BatchStatus batchStatus;

  @DynamoDBAttribute(attributeName = "notes")
  private String notes;

  @DynamoDBAttribute(attributeName = "coachId")
  @DynamoDBIndexHashKey(globalSecondaryIndexName = "coachId-index", attributeName = "coachId")
  private String coachId;

  @DynamoDBAttribute(attributeName = "timezone")
  private String timezone;

  @DynamoDBAttribute(attributeName = "createdAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime createdAt;

  @DynamoDBAttribute(attributeName = "updatedAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime updatedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @DynamoDBDocument
  public static class BatchTiming {
    @DynamoDBAttribute(attributeName = "daysOfWeek")
    private List<String> daysOfWeek;

    @DynamoDBAttribute(attributeName = "startTime")
    @DynamoDBTypeConverted(converter = LocalTimeConverter.class)
    private LocalTime startTime;

    @DynamoDBAttribute(attributeName = "endTime")
    @DynamoDBTypeConverted(converter = LocalTimeConverter.class)
    private LocalTime endTime;
  }
}
