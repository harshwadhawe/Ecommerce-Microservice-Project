package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddToCartDto;
import com.ecommerce.cartservice.dto.ProductDto;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.cartservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    private ProductDto product;

    @BeforeEach
    void setUp() {
        product = new ProductDto();
        product.setId("p1");
        product.setName("Laptop");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(5);
        product.setIsActive(true);
    }

    private void cartExists(Cart cart) {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void missingCartIsCreatedAndPersisted() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart cart = cartService.getCartByUserId("user-1");

        assertEquals("user-1", cart.getUserId());
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void readingAnExistingCartExtendsItsExpiration() {
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(new Cart("user-1")));

        cartService.getCartByUserId("user-1");

        verify(cartRepository).extendExpiration("user-1");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addToCartStoresProductDetailsFromProductService() {
        cartExists(new Cart("user-1"));
        when(productService.getProductSync("p1")).thenReturn(product);

        Cart cart = cartService.addToCart("user-1", new AddToCartDto("p1", 2));

        CartItem added = cart.findItemByProductId("p1");
        assertEquals("Laptop", added.getProductName());
        assertEquals(0, new BigDecimal("999.99").compareTo(added.getPrice()));
        assertEquals(2, added.getQuantity());
    }

    @Test
    void addToCartRejectsUnknownProduct() {
        when(productService.getProductSync("gone")).thenReturn(null);

        assertThrows(ProductNotAvailableException.class,
                () -> cartService.addToCart("user-1", new AddToCartDto("gone", 1)));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addToCartRejectsInactiveProduct() {
        product.setIsActive(false);
        when(productService.getProductSync("p1")).thenReturn(product);

        assertThrows(ProductNotAvailableException.class,
                () -> cartService.addToCart("user-1", new AddToCartDto("p1", 1)));
    }

    @Test
    void addToCartRejectsQuantityAboveStock() {
        when(productService.getProductSync("p1")).thenReturn(product);

        assertThrows(ProductNotAvailableException.class,
                () -> cartService.addToCart("user-1", new AddToCartDto("p1", 6)));
    }

    @Test
    void addToCartAllowsExactlyTheAvailableStock() {
        cartExists(new Cart("user-1"));
        when(productService.getProductSync("p1")).thenReturn(product);

        Cart cart = cartService.addToCart("user-1", new AddToCartDto("p1", 5));

        assertEquals(5, cart.getTotalItems());
    }

    @Test
    void updateQuantityRejectsMoreThanStock() {
        when(productService.getProductSync("p1")).thenReturn(product);

        assertThrows(ProductNotAvailableException.class,
                () -> cartService.updateCartItemQuantity("user-1", "p1", 99));
    }

    @Test
    void updateQuantityToZeroSkipsTheStockCheckAndRemovesTheItem() {
        Cart cart = new Cart("user-1");
        cart.addItem(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 2, null));
        cartExists(cart);

        Cart updated = cartService.updateCartItemQuantity("user-1", "p1", 0);

        assertTrue(updated.getItems().isEmpty());
        verify(productService, never()).getProductSync("p1");
    }

    @Test
    void validateCartStockFailsWhenStockDroppedBelowCartQuantity() {
        Cart cart = new Cart("user-1");
        cart.addItem(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 4, null));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        product.setStockQuantity(2);
        when(productService.getProductSync("p1")).thenReturn(product);

        assertFalse(cartService.validateCartStock("user-1"));
    }

    @Test
    void validateCartStockPassesWhenEveryItemIsStillAvailable() {
        Cart cart = new Cart("user-1");
        cart.addItem(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 1, null));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(productService.getProductSync("p1")).thenReturn(product);

        assertTrue(cartService.validateCartStock("user-1"));
    }

    @Test
    void clearCartEmptiesItemsButKeepsTheCart() {
        Cart cart = new Cart("user-1");
        cart.addItem(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 1, null));
        cartExists(cart);

        cartService.clearCart("user-1");

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }
}
