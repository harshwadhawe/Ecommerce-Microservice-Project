package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddToCartDto;
import com.ecommerce.cartservice.dto.ProductDto;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.exception.CartNotFoundException;
import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.cartservice.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductService productService;

    public Cart getCartByUserId(String userId) {
        Optional<Cart> cart = cartRepository.findByUserId(userId);
        if (cart.isPresent()) {
            cartRepository.extendExpiration(userId);
            return cart.get();
        } else {
            Cart newCart = new Cart(userId);
            return cartRepository.save(newCart);
        }
    }

    public Cart addToCart(String userId, AddToCartDto addToCartDto) {
        ProductDto product = productService.getProductSync(addToCartDto.getProductId());
        if (product == null || !product.getIsActive()) {
            throw new ProductNotAvailableException("Product is not available");
        }

        if (product.getStockQuantity() < addToCartDto.getQuantity()) {
            throw new ProductNotAvailableException("Insufficient stock available");
        }

        Cart cart = getCartByUserId(userId);
        CartItem newItem = new CartItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                addToCartDto.getQuantity(),
                product.getImageUrl()
        );

        cart.addItem(newItem);
        return cartRepository.save(cart);
    }

    public Cart updateCartItemQuantity(String userId, String productId, Integer quantity) {
        Cart cart = getCartByUserId(userId);
        
        if (quantity > 0) {
            ProductDto product = productService.getProductSync(productId);
            if (product != null && product.getStockQuantity() < quantity) {
                throw new ProductNotAvailableException("Insufficient stock available");
            }
        }

        cart.updateItemQuantity(productId, quantity);
        return cartRepository.save(cart);
    }

    public Cart removeFromCart(String userId, String productId) {
        Cart cart = getCartByUserId(userId);
        cart.removeItem(productId);
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        Cart cart = getCartByUserId(userId);
        cart.clearCart();
        cartRepository.save(cart);
    }

    public void deleteCart(String userId) {
        cartRepository.deleteByUserId(userId);
    }

    public boolean validateCartStock(String userId) {
        Cart cart = getCartByUserId(userId);
        for (CartItem item : cart.getItems()) {
            ProductDto product = productService.getProductSync(item.getProductId());
            if (product == null || !product.getIsActive() || product.getStockQuantity() < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }
}