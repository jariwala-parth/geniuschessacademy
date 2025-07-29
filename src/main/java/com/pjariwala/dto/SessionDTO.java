package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pjariwala.model.Session;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
  private String sessionId;
  private String batchId;
  private String batchName; // Added batch name

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate sessionDate;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime startTime;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime endTime;

  private Session.SessionStatus status;
  private String coachId;
  private String notes;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime updatedAt;
}
