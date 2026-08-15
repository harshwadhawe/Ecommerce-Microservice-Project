package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentResponseDto;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    private static final String VALID_BODY = """
            {"orderId":"order-1","amount":19.99,"paymentMethod":"CARD","cardNumber":"4111111111111111",
             "cvv":"123","expiryDate":"12/30","cardholderName":"A B"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void returnsProcessedPayment() throws Exception {
        when(paymentService.processPayment(any())).thenReturn(
                new PaymentResponseDto("tx-1", "order-1", new BigDecimal("19.99"), "SUCCESS", "ok"));

        mockMvc.perform(post("/api/payment/process").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("tx-1"));
    }

    @Test
    void rejectsMissingCardDetails() throws Exception {
        mockMvc.perform(post("/api/payment/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"amount\":19.99}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/payment/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("19.99", "0")))
                .andExpect(status().isBadRequest());
    }
}
