package com.ecommerce.cartservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Without the timeouts, a hung product-service holds a cart request thread open indefinitely.
     * A read timeout surfaces as {@link ReadTimeoutException}, which ProductService reports as
     * "product-service unavailable" (503) rather than misreporting it as a bad request.
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            @Value("${app.product-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.product-service.response-timeout-ms:3000}") int responseTimeoutMs,
            @Value("${app.product-service.max-idle-time-ms:5000}") int maxIdleTimeMs) {

        // Pooled connections must be dropped before the server hangs up on them. Tomcat closes idle
        // keep-alive connections after 20s; without a shorter client-side idle limit the pool hands
        // out a socket the server has already closed and the request dies with
        // "PrematureCloseException: Connection prematurely closed BEFORE response" -- which this
        // service would then report as a product-service outage.
        ConnectionProvider connectionProvider = ConnectionProvider.builder("product-service")
                .maxIdleTime(Duration.ofMillis(maxIdleTimeMs))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
