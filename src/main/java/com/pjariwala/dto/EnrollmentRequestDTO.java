package com.pjariwala.dto;

import com.pjariwala.model.Enrollment;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequestDTO {
  private String batchId;
  private String studentId;
  private LocalDate enrollmentDate;
  private Enrollment.EnrollmentStatus enrollmentStatus;
  private Enrollment.PaymentStatus enrollmentPaymentStatus;
  private Double currentPaymentAmount;
  private String notes;
}
