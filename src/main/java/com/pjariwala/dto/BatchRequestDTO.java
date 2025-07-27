package com.pjariwala.dto;

import com.pjariwala.model.Batch;
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
  private String batchName;
  private Integer batchSize;
  private LocalDate startDate;
  private LocalDate endDate;
  private BatchTimingDTO batchTiming;
  private Batch.PaymentType paymentType;
  private Double monthlyFee;
  private Double oneTimeFee;
  private Batch.OccurrenceType occurrenceType;
  private Batch.BatchStatus batchStatus;
  private String notes;
  private String coachId;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BatchTimingDTO {
    private List<String> daysOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
  }
}
