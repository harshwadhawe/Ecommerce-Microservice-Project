package com.ecommerce.orderservice.config;

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
     * The response timeout is 10s, not the 3s cart-service uses: payment-service deliberately
     * sleeps 1-3 seconds to imitate a gateway, and a tighter budget turns normal checkouts into
     * intermittent 503s.
     *
     * Without these, a hung dependency holds an order request thread open indefinitely.
     * A read timeout surfaces as {@link ReadTimeoutException}, which the clients report as
     * "service unavailable" (503) rather than misreporting it as a bad request.
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            @Value("${app.downstream.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.downstream.response-timeout-ms:10000}") int responseTimeoutMs,
            @Value("${app.downstream.max-idle-time-ms:5000}") int maxIdleTimeMs) {

        // Pooled connections must be dropped before the server hangs up on them. Tomcat closes idle
        // keep-alive connections after 20s; without a shorter client-side idle limit the pool hands
        // out a socket the server has already closed and the request dies with
        // "PrematureCloseException: Connection prematurely closed BEFORE response" -- which this
        // service would then report as a downstream outage.
        ConnectionProvider connectionProvider = ConnectionProvider.builder("downstream")
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
