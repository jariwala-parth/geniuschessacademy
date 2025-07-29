package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDTO {
  private String sessionId;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate sessionDate;

  private String description;
  private Double amount;
  private String type; // "SESSION", "ATTENDANCE", "FIXED_MONTHLY"
}
