package com.ecommerce.orderservice.config;

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
            @Value("${app.downstream.response-timeout-ms:10000}") int responseTimeoutMs) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
