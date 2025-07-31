package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pjariwala.enums.EnrollmentStatus;
import com.pjariwala.enums.PaymentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponseDTO {
  private String batchId;
  private String studentId;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate enrollmentDate;

  private EnrollmentStatus enrollmentStatus;
  private PaymentStatus enrollmentPaymentStatus;
  private Double currentPaymentAmount;
  private String notes;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime updatedAt;

  // Additional fields for enriched responses
  private String batchName; // From Batch table
  private String studentName; // From User table
}
