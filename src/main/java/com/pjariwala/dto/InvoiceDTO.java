package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pjariwala.enums.InvoiceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
  private String studentId;
  private String invoiceId;
  private String batchId;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate billingPeriodStart;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate billingPeriodEnd;

  private Double calculatedAmount;
  private Double amountPaid;
  private InvoiceStatus status;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dueDate;

  private List<InvoiceItemDTO> items;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime updatedAt;
}
