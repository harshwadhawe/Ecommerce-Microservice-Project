package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.ProductDto;
import com.ecommerce.cartservice.exception.ProductServiceUnavailableException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The distinction under test: "product-service says this product does not exist" (null, the caller
 * gets a 400) versus "product-service is down" (throw, the caller gets a 503). Collapsing the two
 * made every outage look like an invalid request.
 */
class ProductServiceTest {

    private MockWebServer productService;
    private ProductService service;

    @BeforeEach
    void setUp() throws IOException {
        productService = new MockWebServer();
        productService.start();
        service = new ProductService(WebClient.builder());
        ReflectionTestUtils.setField(service, "productServiceUrl",
                "http://localhost:" + productService.getPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        productService.shutdown();
    }

    @Test
    void returnsTheProductOnSuccess() {
        productService.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"id":"p1","name":"Laptop","price":999.99,"stockQuantity":5,"isActive":true}"""));

        ProductDto product = service.getProductSync("p1");

        assertEquals("p1", product.getId());
        assertEquals(5, product.getStockQuantity());
    }

    @Test
    void missingProductReturnsNull() {
        productService.enqueue(new MockResponse().setResponseCode(404));

        assertNull(service.getProductSync("gone"));
    }

    @Test
    void serverErrorIsAnOutageNotAMissingProduct() {
        productService.enqueue(new MockResponse().setResponseCode(500));

        assertThrows(ProductServiceUnavailableException.class, () -> service.getProductSync("p1"));
    }

    @Test
    void connectionFailureIsAnOutage() throws IOException {
        productService.shutdown();

        assertThrows(ProductServiceUnavailableException.class, () -> service.getProductSync("p1"));
    }
}
