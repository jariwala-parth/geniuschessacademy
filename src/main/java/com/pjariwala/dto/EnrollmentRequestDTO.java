package com.pjariwala.dto;

import com.pjariwala.enums.EnrollmentStatus;
import com.pjariwala.enums.PaymentStatus;
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
  private EnrollmentStatus enrollmentStatus;
  private PaymentStatus enrollmentPaymentStatus;
  private Double currentPaymentAmount;
  private String notes;
}
