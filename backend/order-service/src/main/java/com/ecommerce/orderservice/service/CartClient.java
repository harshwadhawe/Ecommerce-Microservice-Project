package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CartDto;
import com.ecommerce.orderservice.exception.DownstreamUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * cart-service authorizes on the caller's own token, so the shopper's bearer token is forwarded
 * rather than order-service holding a privileged credential of its own.
 */
@Service
public class CartClient {

    private final WebClient webClient;

    @Value("${app.cart-service.url:http://localhost:8083}")
    private String cartServiceUrl;

    public CartClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public CartDto getCart(String userId, String bearerToken) {
        try {
            return webClient.get()
                    .uri(cartServiceUrl + "/api/cart/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .bodyToMono(CartDto.class)
                    .block();
        } catch (Exception e) {
            throw new DownstreamUnavailableException("cart-service could not be reached", e);
        }
    }

    public void clearCart(String userId, String bearerToken) {
        try {
            webClient.delete()
                    .uri(cartServiceUrl + "/api/cart/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw new DownstreamUnavailableException("cart-service could not be reached", e);
        }
    }
}
