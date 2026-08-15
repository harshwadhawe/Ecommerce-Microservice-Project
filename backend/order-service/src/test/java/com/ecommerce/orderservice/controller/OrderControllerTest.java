package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.EmptyCartException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.PaymentDeclinedException;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.ecommerce.orderservice.config.SecurityConfig;
import com.ecommerce.orderservice.security.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest includes Filter beans, so the real JwtAuthenticationFilter runs here -- these requests
 * carry a genuinely signed token rather than a mock principal. Two consequences worth knowing:
 * mocking that filter would break every request (a Mockito mock never calls chain.doFilter), and a
 * bogus token silently clears the context, leaving Authentication null inside the controller.
 * The real SecurityConfig is imported so the chain matches production exactly.
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "jwt.secret=" + OrderControllerTest.SECRET)
class OrderControllerTest {

    static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256!!";

    private static final String TOKEN = "Bearer " + Jwts.builder()
            .setClaims(Map.of("userId", "7"))
            .setSubject("a@b.com")
            .setExpiration(new Date(System.currentTimeMillis() + 600000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
            .compact();

    private static final String BODY = """
            {"recipientName":"Ada Lovelace","address":"1 Main St","city":"Chicago","country":"USA",
             "postalCode":"60601","cardholderName":"Ada Lovelace","cardNumber":"4111111111111111",
             "expiryDate":"12/30","cvv":"123"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private Order paidOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId("7");
        order.setOrderNumber("ORD-20260815-120000-4242");
        order.addItem(new OrderItem("p1", "Laptop", new BigDecimal("999.99"), 1, null));
        order.markPaid("txn-1", "Payment processed successfully");
        return order;
    }

    @Test
    void placingAnOrderReturnsCreatedWithItsItems() throws Exception {
        when(orderService.placeOrder(anyString(), anyString(), any())).thenReturn(paidOrder());

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-20260815-120000-4242"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalAmount").value(999.99))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"))
                // the item must not serialize its parent order back out again
                .andExpect(jsonPath("$.items[0].order").doesNotExist());
    }

    @Test
    void aDeclinedCardIsPaymentRequiredNotAServerError() throws Exception {
        Order failed = new Order();
        failed.setOrderNumber("ORD-1");
        failed.markPaymentFailed("txn-2", "Card declined");
        when(orderService.placeOrder(anyString(), anyString(), any()))
                .thenThrow(new PaymentDeclinedException("Card declined", failed));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("Card declined"))
                .andExpect(jsonPath("$.orderNumber").value("ORD-1"))
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
    }

    @Test
    void anEmptyCartIsABadRequest() throws Exception {
        when(orderService.placeOrder(anyString(), anyString(), any()))
                .thenThrow(new EmptyCartException("Your cart is empty"));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Your cart is empty"));
    }

    @Test
    void missingShippingDetailsAreRejectedFieldByField() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientName\":\"Ada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.cardNumber").exists());
    }

    @Test
    void orderHistoryIsScopedToTheAuthenticatedUser() throws Exception {
        when(orderService.getOrdersForUser("7")).thenReturn(List.of(paidOrder()));

        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-20260815-120000-4242"));
    }

    @Test
    void anotherUsersOrderIsForbidden() throws Exception {
        when(orderService.getOrderForUser(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("This order belongs to another user"));

        mockMvc.perform(get("/api/orders/1").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownOrderIsNotFound() throws Exception {
        when(orderService.getOrderForUser(anyLong(), anyString()))
                .thenThrow(new OrderNotFoundException("Order not found: 99"));

        mockMvc.perform(get("/api/orders/99").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusValueIsValidated() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/orders/1/status").header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusCanBeAdvanced() throws Exception {
        Order shipped = paidOrder();
        shipped.changeStatus(OrderStatus.SHIPPED);
        when(orderService.updateStatus(anyLong(), anyString(), any())).thenReturn(shipped);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/orders/1/status").header(HttpHeaders.AUTHORIZATION, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}
