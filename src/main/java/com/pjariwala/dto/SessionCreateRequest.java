package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {
  private String batchId;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate sessionDate;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime startTime;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime endTime;

  private String coachId;
  private String notes;
}
