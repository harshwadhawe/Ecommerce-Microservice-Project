package com.ecommerce.orderservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFound(OrderNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Map<String, String>> handleEmptyCart(EmptyCartException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "This order belongs to another user");
    }

    /** 402: the request was fine, the card was refused. The order number identifies the attempt. */
    @ExceptionHandler(PaymentDeclinedException.class)
    public ResponseEntity<Map<String, String>> handlePaymentDeclined(PaymentDeclinedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        if (ex.getOrder() != null) {
            error.put("orderNumber", ex.getOrder().getOrderNumber());
            error.put("status", ex.getOrder().getStatus().name());
        }
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleDownstream(DownstreamUnavailableException ex) {
        log.error("Downstream call failed", ex);
        return body(HttpStatus.SERVICE_UNAVAILABLE, "A required service is unavailable, please retry");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }
}
