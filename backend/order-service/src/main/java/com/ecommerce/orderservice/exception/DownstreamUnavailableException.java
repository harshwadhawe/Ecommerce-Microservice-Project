package com.ecommerce.orderservice.exception;

/** A dependency could not be reached -- distinct from that dependency saying "no". */
public class DownstreamUnavailableException extends RuntimeException {
    public DownstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
