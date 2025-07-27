package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pjariwala.model.Batch;
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
  private Batch.PaymentType paymentType;

  private Double monthlyFee;
  private Double oneTimeFee;

  @NotNull(message = "Occurrence type is required")
  private Batch.OccurrenceType occurrenceType;

  private Batch.BatchStatus batchStatus;
  private String notes;

  @NotBlank(message = "Coach ID is required")
  private String coachId;

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
