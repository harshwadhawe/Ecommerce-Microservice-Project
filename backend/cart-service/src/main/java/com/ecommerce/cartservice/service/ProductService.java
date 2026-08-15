package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ProductService {

    private final WebClient webClient;

    @Value("${app.product-service.url:http://localhost:8082}")
    private String productServiceUrl;

    public ProductService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<ProductDto> getProduct(String productId) {
        return webClient.get()
                .uri(productServiceUrl + "/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .onErrorResume(throwable -> {
                    return Mono.empty();
                });
    }

    public ProductDto getProductSync(String productId) {
        try {
            return getProduct(productId).block();
        } catch (Exception e) {
            return null;
        }
    }
}