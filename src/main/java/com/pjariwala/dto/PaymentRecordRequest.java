package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pjariwala.enums.PaymentMethod;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordRequest {
  private Double amountPaid;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate paymentDate;

  private PaymentMethod paymentMethod;
  private String notes;
}
