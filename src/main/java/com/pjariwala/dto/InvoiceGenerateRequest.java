package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceGenerateRequest {
  private String studentId;
  private String batchId; // Optional, if invoice is for a specific batch

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate billingPeriodStart;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate billingPeriodEnd;
}
