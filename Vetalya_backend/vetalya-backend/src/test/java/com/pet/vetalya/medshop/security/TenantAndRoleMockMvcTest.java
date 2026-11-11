package com.pet.vetalya.medshop.security;

import com.pet.vetalya.medshop.billing.controller.InvoiceController;
import com.pet.vetalya.medshop.billing.dto.BillingDtos.*;
import com.pet.vetalya.medshop.billing.service.BillingService;
import com.pet.vetalya.medshop.gst.controller.GstReturnController;
import com.pet.vetalya.medshop.gst.dto.GstDtos.*;
import com.pet.vetalya.medshop.gst.entity.GstReturn;
import com.pet.vetalya.medshop.gst.service.GstReturnService;
import com.pet.vetalya.medshop.report.controller.ReportController;
import com.pet.vetalya.medshop.report.dto.ReportDtos.*;
import com.pet.vetalya.medshop.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {InvoiceController.class, ReportController.class, GstReturnController.class})
@EnableMethodSecurity
class TenantAndRoleMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingService billingService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private GstReturnService gstReturnService;

    @Test
    @DisplayName("Cashier cannot access profit report -> 403 Forbidden")
    void testCashierCannotAccessProfitReport() throws Exception {
        mockMvc.perform(get("/api/store/reports/profit")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CASHIER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager cannot access profit report -> 403 Forbidden")
    void testManagerCannotAccessProfitReport() throws Exception {
        mockMvc.perform(get("/api/store/reports/profit")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can access profit report -> 200 OK")
    void testOwnerCanAccessProfitReport() throws Exception {
        when(reportService.getProfitReport(any(), any())).thenReturn(
                new ProfitReportResponse(LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );

        mockMvc.perform(get("/api/store/reports/profit")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cashier cannot cancel invoices -> 403 Forbidden")
    void testCashierCannotCancelInvoice() throws Exception {
        mockMvc.perform(post("/api/store/invoices/inv-1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason": "Customer returned medicines"}
                """)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CASHIER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager can cancel invoices -> 200 OK")
    void testManagerCanCancelInvoice() throws Exception {
        when(billingService.cancelInvoice(eq("inv-1"), any())).thenReturn(
                new InvoiceResponse("inv-1", "INV/26-27/000001", LocalDate.now(), null, null, null, null, null,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
                        null, null, null, null, java.util.List.of())
        );

        mockMvc.perform(post("/api/store/invoices/inv-1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason": "Customer returned medicines"}
                """)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cashier cannot generate GST return -> 403 Forbidden")
    void testCashierCannotGenerateGstReturn() throws Exception {
        mockMvc.perform(post("/api/store/gst/returns/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"returnMonth": 11, "returnYear": 2026, "returnType": "GSTR1"}
                """)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CASHIER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager cannot mark GST return as FILED -> 403 Forbidden (Owner only)")
    void testManagerCannotMarkGstReturnFiled() throws Exception {
        mockMvc.perform(post("/api/store/gst/returns/gr-1/mark-filed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"filedRefNumber": "ARN1234567890"}
                """)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can mark GST return as FILED -> 200 OK")
    void testOwnerCanMarkGstReturnFiled() throws Exception {
        when(gstReturnService.markFiled(eq("gr-1"), any())).thenReturn(
                new GstReturnResponse("gr-1", "tenant-1", 11, 2026, GstReturn.Type.GSTR1,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        GstReturn.Status.FILED, "ARN1234567890", java.time.Instant.now())
        );

        mockMvc.perform(post("/api/store/gst/returns/gr-1/mark-filed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"filedRefNumber": "ARN1234567890"}
                """)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"))))
                .andExpect(status().isOk());
    }
}

