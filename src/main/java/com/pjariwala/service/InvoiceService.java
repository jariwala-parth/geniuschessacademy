package com.pjariwala.service;

import com.pjariwala.dto.InvoiceDTO;
import com.pjariwala.dto.InvoiceGenerateRequest;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.PaymentRecordRequest;

public interface InvoiceService {

  /** Generate invoice for a student/batch */
  InvoiceDTO generateInvoice(InvoiceGenerateRequest request, String coachId, String userType);

  /** Record payment for an invoice */
  InvoiceDTO recordPayment(
      String invoiceId, PaymentRecordRequest request, String coachId, String userType);

  /** Get invoice details by ID */
  InvoiceDTO getInvoiceById(String invoiceId);

  /** Get all invoices for a student */
  PageResponseDTO<InvoiceDTO> getInvoicesByStudent(String studentId, int page, int size);

  /** Get all invoices with optional filters */
  PageResponseDTO<InvoiceDTO> getAllInvoices(
      String status,
      String dueDateStart,
      String dueDateEnd,
      String batchId,
      String studentId,
      int page,
      int size);

  /** Update invoice */
  InvoiceDTO updateInvoice(
      String invoiceId, InvoiceDTO invoiceDTO, String coachId, String userType);

  /** Delete invoice */
  void deleteInvoice(String invoiceId, String coachId, String userType);

  /** Calculate fees based on attendance and batch type */
  Double calculateFees(String studentId, String batchId, String startDate, String endDate);
}
