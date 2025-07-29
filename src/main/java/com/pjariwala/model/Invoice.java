package com.pjariwala.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.pjariwala.enums.InvoiceStatus;
import com.pjariwala.util.LocalDateConverter;
import com.pjariwala.util.LocalDateTimeConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "GCA_Invoices")
public class Invoice {

  @DynamoDBHashKey(attributeName = "organizationId")
  private String organizationId;

  @DynamoDBRangeKey(attributeName = "invoiceId")
  private String invoiceId;

  @DynamoDBAttribute(attributeName = "studentId")
  private String studentId;

  @DynamoDBAttribute(attributeName = "batchId")
  private String batchId;

  @DynamoDBAttribute(attributeName = "billingPeriodStart")
  @DynamoDBTypeConverted(converter = LocalDateConverter.class)
  private LocalDate billingPeriodStart;

  @DynamoDBAttribute(attributeName = "billingPeriodEnd")
  @DynamoDBTypeConverted(converter = LocalDateConverter.class)
  private LocalDate billingPeriodEnd;

  @DynamoDBAttribute(attributeName = "calculatedAmount")
  private Double calculatedAmount;

  @DynamoDBAttribute(attributeName = "amountPaid")
  private Double amountPaid;

  @DynamoDBAttribute(attributeName = "status")
  @DynamoDBTypeConvertedEnum
  private InvoiceStatus status;

  @DynamoDBAttribute(attributeName = "dueDate")
  @DynamoDBTypeConverted(converter = LocalDateConverter.class)
  private LocalDate dueDate;

  @DynamoDBAttribute(attributeName = "items")
  private List<InvoiceItem> items;

  @DynamoDBAttribute(attributeName = "createdAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime createdAt;

  @DynamoDBAttribute(attributeName = "updatedAt")
  @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
  private LocalDateTime updatedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class InvoiceItem {
    private String sessionId;
    private LocalDate sessionDate;
    private String description;
    private Double amount;
    private String type; // "SESSION", "ATTENDANCE", "FIXED_MONTHLY"
  }
}
