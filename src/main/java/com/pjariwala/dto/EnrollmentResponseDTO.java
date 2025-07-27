package com.pjariwala.dto;

import com.pjariwala.model.Enrollment;
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
  private LocalDate enrollmentDate;
  private Enrollment.EnrollmentStatus enrollmentStatus;
  private Enrollment.PaymentStatus enrollmentPaymentStatus;
  private Double currentPaymentAmount;
  private String notes;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Additional fields for enriched responses
  private String batchName; // From Batch table
  private String studentName; // From User table
}
