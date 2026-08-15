package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.PaymentRequestDto;
import com.ecommerce.orderservice.dto.PaymentResponseDto;
import com.ecommerce.orderservice.exception.DownstreamUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PaymentClient {

    private final WebClient webClient;

    @Value("${app.payment-service.url:http://localhost:8085}")
    private String paymentServiceUrl;

    public PaymentClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * A declined card is a normal outcome and comes back as a FAILED response. Only an unreachable
     * or erroring payment service throws -- the caller must be able to tell "your card was refused"
     * from "we could not ask".
     */
    public PaymentResponseDto charge(PaymentRequestDto request) {
        try {
            return webClient.post()
                    .uri(paymentServiceUrl + "/api/payment/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResponseDto.class)
                    .block();
        } catch (Exception e) {
            throw new DownstreamUnavailableException("payment-service could not be reached", e);
        }
    }
}
