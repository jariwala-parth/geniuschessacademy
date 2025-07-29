package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pjariwala.enums.BatchStatus;
import com.pjariwala.enums.OccurrenceType;
import com.pjariwala.enums.PaymentType;
import com.pjariwala.util.CustomLocalTimeDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequestDTO {
  @NotBlank(message = "Batch name is required")
  private String batchName;

  @NotNull(message = "Batch size is required")
  @Positive(message = "Batch size must be positive")
  private Integer batchSize;

  @NotNull(message = "Start date is required")
  private LocalDate startDate;

  @NotNull(message = "End date is required")
  private LocalDate endDate;

  @NotNull(message = "Batch timing is required")
  @Valid
  private BatchTimingDTO batchTiming;

  @NotNull(message = "Payment type is required")
  private PaymentType paymentType;

  private Double fixedMonthlyFee;
  private Double perSessionFee;

  @NotNull(message = "Occurrence type is required")
  private OccurrenceType occurrenceType;

  private BatchStatus batchStatus;
  private String notes;

  @NotBlank(message = "Coach ID is required")
  private String coachId;

  private String timezone;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BatchTimingDTO {
    @NotNull(message = "Days of week are required")
    private List<String> daysOfWeek;

    @NotNull(message = "Start time is required")
    @JsonDeserialize(using = CustomLocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonDeserialize(using = CustomLocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
  }
}
