package com.pjariwala.controller;

import com.pjariwala.dto.InvoiceDTO;
import com.pjariwala.dto.InvoiceGenerateRequest;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.PaymentRecordRequest;
import com.pjariwala.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invoices")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Invoice Management", description = "APIs for managing invoices and payments")
public class InvoiceController {

  @Autowired private InvoiceService invoiceService;

  @PostMapping("/generate")
  @Operation(
      summary = "Generate invoice for a student/batch",
      description =
          "Triggers the generation of an invoice for a student, possibly for a specific billing"
              + " period or batch")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<InvoiceDTO> generateInvoice(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @PathVariable String organizationId,
      @RequestBody InvoiceGenerateRequest request) {

    log.info(
        "evt=invoice_generation_request coachId={} studentId={} batchId={} organizationId={}",
        coachId,
        request.getStudentId(),
        request.getBatchId(),
        organizationId);

    InvoiceDTO invoice = invoiceService.generateInvoice(request, coachId, organizationId);

    log.info(
        "evt=invoice_generated invoiceId={} studentId={}",
        invoice.getInvoiceId(),
        request.getStudentId());

    return ResponseEntity.ok(invoice);
  }

  @PostMapping("/{invoiceId}/payments")
  @Operation(
      summary = "Record payment for an invoice",
      description =
          "Allows coaches to manually record a payment received against an existing invoice")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<InvoiceDTO> recordPayment(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @PathVariable String organizationId,
      @PathVariable String invoiceId,
      @RequestBody PaymentRecordRequest request) {

    log.info(
        "evt=payment_record_request coachId={} invoiceId={} amount={} organizationId={}",
        coachId,
        invoiceId,
        request.getAmountPaid(),
        organizationId);

    InvoiceDTO invoice = invoiceService.recordPayment(invoiceId, request, coachId, organizationId);

    log.info("evt=payment_recorded invoiceId={} amount={}", invoiceId, request.getAmountPaid());

    return ResponseEntity.ok(invoice);
  }

  @GetMapping("/{invoiceId}")
  @Operation(
      summary = "Get invoice details by ID",
      description = "Retrieves the details of a specific invoice")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<InvoiceDTO> getInvoiceById(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @PathVariable String organizationId,
      @PathVariable String invoiceId) {

    log.info("evt=get_invoice_details invoiceId={} organizationId={}", invoiceId, organizationId);

    InvoiceDTO invoice = invoiceService.getInvoiceById(invoiceId, userId, organizationId);

    if (invoice == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(invoice);
  }

  @GetMapping("/student/{studentId}")
  @Operation(
      summary = "Get all invoices for a student",
      description =
          "Allows students and coaches to view all invoices related to a specific student")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<InvoiceDTO>> getInvoicesByStudent(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @PathVariable String organizationId,
      @PathVariable String studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    log.info(
        "evt=get_student_invoices studentId={} page={} size={} organizationId={}",
        studentId,
        page,
        size,
        organizationId);

    PageResponseDTO<InvoiceDTO> invoices =
        invoiceService.getInvoicesByStudent(studentId, page, size, userId, organizationId);

    return ResponseEntity.ok(invoices);
  }

  @GetMapping
  @Operation(
      summary = "Get all invoices with optional filters",
      description = "Retrieves a list of all invoices with optional filters for reporting purposes")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<InvoiceDTO>> getAllInvoices(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @PathVariable String organizationId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String dueDateStart,
      @RequestParam(required = false) String dueDateEnd,
      @RequestParam(required = false) String batchId,
      @RequestParam(required = false) String studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    log.info(
        "evt=get_all_invoices status={} dueDateStart={} dueDateEnd={} batchId={} studentId={}"
            + " page={} size={} organizationId={}",
        status,
        dueDateStart,
        dueDateEnd,
        batchId,
        studentId,
        page,
        size,
        organizationId);

    PageResponseDTO<InvoiceDTO> invoices =
        invoiceService.getAllInvoices(
            status,
            dueDateStart,
            dueDateEnd,
            batchId,
            studentId,
            page,
            size,
            userId,
            organizationId);

    return ResponseEntity.ok(invoices);
  }

  @PutMapping("/{invoiceId}")
  @Operation(summary = "Update invoice", description = "Update an existing invoice")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<InvoiceDTO> updateInvoice(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @PathVariable String organizationId,
      @PathVariable String invoiceId,
      @RequestBody InvoiceDTO invoiceDTO) {

    log.info(
        "evt=update_invoice_request coachId={} invoiceId={} organizationId={}",
        coachId,
        invoiceId,
        organizationId);

    InvoiceDTO invoice =
        invoiceService.updateInvoice(invoiceId, invoiceDTO, coachId, organizationId);

    log.info("evt=invoice_updated invoiceId={}", invoiceId);

    return ResponseEntity.ok(invoice);
  }

  @DeleteMapping("/{invoiceId}")
  @Operation(summary = "Delete invoice", description = "Delete an existing invoice")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> deleteInvoice(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @PathVariable String organizationId,
      @PathVariable String invoiceId) {

    log.info(
        "evt=delete_invoice_request coachId={} invoiceId={} organizationId={}",
        coachId,
        invoiceId,
        organizationId);

    invoiceService.deleteInvoice(invoiceId, coachId, organizationId);

    log.info("evt=invoice_deleted invoiceId={}", invoiceId);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/calculate-fees")
  @Operation(
      summary = "Calculate fees for a student",
      description = "Calculate fees based on attendance and batch type")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Double> calculateFees(
      @PathVariable String organizationId,
      @RequestParam String studentId,
      @RequestParam String batchId,
      @RequestParam String startDate,
      @RequestParam String endDate) {

    log.info(
        "evt=calculate_fees_request studentId={} batchId={} startDate={} endDate={}"
            + " organizationId={}",
        studentId,
        batchId,
        startDate,
        endDate,
        organizationId);

    Double fees =
        invoiceService.calculateFees(studentId, batchId, startDate, endDate, organizationId);

    return ResponseEntity.ok(fees);
  }
}
