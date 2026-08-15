package com.ecommerce.cartservice.exception;

/**
 * product-service could not be reached or answered with a server error. Distinct from
 * {@link ProductNotAvailableException}, which means the product genuinely cannot be bought.
 */
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
