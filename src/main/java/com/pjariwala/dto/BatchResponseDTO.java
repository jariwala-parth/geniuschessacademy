package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pjariwala.enums.BatchStatus;
import com.pjariwala.enums.OccurrenceType;
import com.pjariwala.enums.PaymentType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponseDTO {
  private String batchId;
  private String batchName;
  private Integer batchSize;
  private Integer currentStudents;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  private BatchTimingDTO batchTiming;
  private PaymentType paymentType;

  private Double fixedMonthlyFee;
  private Double perSessionFee;

  private OccurrenceType occurrenceType;
  private BatchStatus batchStatus;
  private String notes;
  private String coachId;
  private String timezone;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime updatedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BatchTimingDTO {
    private List<String> daysOfWeek;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
  }
}
