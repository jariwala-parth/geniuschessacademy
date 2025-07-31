package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.pjariwala.enums.EnrollmentStatus;
import com.pjariwala.enums.PaymentStatus;
import com.pjariwala.util.LocalDateConverter;
import com.pjariwala.util.LocalDateTimeConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "GCA_Enrollments")
public class Enrollment {

  // Composite Primary Key
  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBRangeKey(attributeName = "enrollmentId")
  private String enrollmentId;

  @DynamoDBAttribute(attributeName = "batchId")
  private String batchId;

  @DynamoDBAttribute(attributeName = "studentId")
  @DynamoDBIndexHashKey(
      globalSecondaryIndexName = "studentId-batchId-index",
      attributeName = "studentId")
  private String studentId;

  @DynamoDBAttribute(attributeName = "enrollmentDate")
  @DynamoDBTypeConverted(converter = LocalDateConverter.class)
  private LocalDate enrollmentDate;

  @DynamoDBAttribute(attributeName = "enrollmentStatus")
  @DynamoDBTypeConvertedEnum
  private EnrollmentStatus enrollmentStatus;

  @DynamoDBAttribute(attributeName = "paymentStatus")
  @DynamoDBTypeConvertedEnum
  private PaymentStatus enrollmentPaymentStatus;

  @DynamoDBAttribute(attributeName = "currentPaymentAmount")
  private Double currentPaymentAmount;

  @DynamoDBAttribute(attributeName = "notes")
  private String notes;

  @DynamoDBAttribute(attributeName = "createdAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime createdAt;

  @DynamoDBAttribute(attributeName = "updatedAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime updatedAt;
}
