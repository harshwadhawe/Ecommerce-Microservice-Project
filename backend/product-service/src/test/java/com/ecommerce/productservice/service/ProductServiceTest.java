package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductDto;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product stored;

    @BeforeEach
    void setUp() {
        stored = new Product("Laptop", "A laptop", new BigDecimal("999.99"), "Electronics", "Acme", 5);
        stored.setId("p1");
    }

    private ProductDto dto() {
        return new ProductDto("Laptop", "A laptop", new BigDecimal("999.99"), "Electronics", "Acme", 5);
    }

    @Test
    void createdProductsAreActiveByDefault() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product created = productService.createProduct(dto());

        assertTrue(created.getIsActive());
        assertEquals("Laptop", created.getName());
        assertEquals(5, created.getStockQuantity());
    }

    @Test
    void activeProductsAreFilteredByTheQueryNotPaddedWithNulls() {
        Product inactive = new Product("Old", "Old", new BigDecimal("1.00"), "Electronics", "Acme", 0);
        inactive.setIsActive(false);
        when(productRepository.findByIsActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stored)));

        Page<Product> page = productService.getActiveProducts(0, 10, "createdAt", "desc");

        assertEquals(1, page.getContent().size());
        assertFalse(page.getContent().contains(null));
        assertFalse(page.getContent().contains(inactive));
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void activeProductsHonourSortDirection() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(stored)));

        productService.getActiveProducts(2, 25, "price", "asc");

        verify(productRepository).findByIsActiveTrue(pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(25, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getValue().getSort().getOrderFor("price").getDirection());
    }

    @Test
    void updateRejectsUnknownId() {
        when(productRepository.findById("gone")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct("gone", dto()));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateDoesNotResurrectASoftDeletedProduct() {
        stored.setIsActive(false);
        when(productRepository.findById("p1")).thenReturn(Optional.of(stored));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(productService.updateProduct("p1", dto()).getIsActive());
    }

    @Test
    void getByIdRejectsUnknownId() {
        when(productRepository.findById("gone")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById("gone"));
    }

    @Test
    void deleteIsASoftDeleteThatKeepsTheDocument() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(stored));

        productService.deleteProduct("p1");

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertFalse(saved.getValue().getIsActive());
        verify(productRepository, never()).deleteById("p1");
    }

    @Test
    void stockCanBeDecrementedDownToZero() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(stored));

        productService.updateStock("p1", -5);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertEquals(0, saved.getValue().getStockQuantity());
    }

    @Test
    void stockCannotGoNegative() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(stored));

        assertThrows(IllegalArgumentException.class, () -> productService.updateStock("p1", -6));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void stockCanBeReplenished() {
        when(productRepository.findById("p1")).thenReturn(Optional.of(stored));

        productService.updateStock("p1", 10);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertEquals(15, saved.getValue().getStockQuantity());
    }
}
