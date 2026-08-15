package com.ecommerce.cartservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Without these, a hung product-service holds a cart request thread open indefinitely.
     * A read timeout surfaces as {@link ReadTimeoutException}, which ProductService reports as
     * "product-service unavailable" (503) rather than misreporting it as a bad request.
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            @Value("${app.product-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.product-service.response-timeout-ms:3000}") int responseTimeoutMs) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
