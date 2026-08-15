package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.ProductDto;
import com.ecommerce.cartservice.exception.ProductServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class ProductService {

    private final WebClient webClient;

    @Value("${app.product-service.url:http://localhost:8082}")
    private String productServiceUrl;

    public ProductService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Returns null only when product-service positively reports the product does not exist.
     * Anything else -- connection refused, timeout, 5xx -- is an outage and must not be reported
     * to the caller as "this product is unavailable", which would be a 400 for a server-side fault.
     */
    public ProductDto getProductSync(String productId) {
        try {
            return webClient.get()
                    .uri(productServiceUrl + "/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        } catch (Exception e) {
            throw new ProductServiceUnavailableException(
                    "product-service could not be reached for product " + productId, e);
        }
    }
}
