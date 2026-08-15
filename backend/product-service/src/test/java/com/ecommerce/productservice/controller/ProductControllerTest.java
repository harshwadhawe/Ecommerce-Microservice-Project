package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String VALID_BODY = """
            {"name":"Laptop","description":"A laptop","price":999.99,
             "category":"Electronics","brand":"Acme","stockQuantity":5}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private Product product() {
        Product product = new Product("Laptop", "A laptop", new BigDecimal("999.99"), "Electronics", "Acme", 5);
        product.setId("p1");
        return product;
    }

    @Test
    void returnsProductById() throws Exception {
        when(productService.getProductById("p1")).thenReturn(product());

        mockMvc.perform(get("/api/products/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void unknownProductMapsToNotFound() throws Exception {
        when(productService.getProductById("gone")).thenThrow(new ProductNotFoundException("Product not found with id: gone"));

        mockMvc.perform(get("/api/products/gone"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found with id: gone"));
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(productService.createProduct(any())).thenReturn(product());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("p1"));
    }

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());
    }

    @Test
    void createRejectsZeroPrice() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("999.99", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());
    }

    @Test
    void createRejectsNegativeStock() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"stockQuantity\":5", "\"stockQuantity\":-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stockQuantity").exists());
    }

    @Test
    void listUsesDefaultPagination() throws Exception {
        Page<Product> page = new PageImpl<>(List.of(product()), Pageable.ofSize(10), 1);
        when(productService.getAllProducts(anyInt(), anyInt(), anyString(), anyString())).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("p1"));

        verify(productService).getAllProducts(0, 10, "createdAt", "desc");
    }

    @Test
    void searchPassesTheQueryThrough() throws Exception {
        when(productService.searchProducts(eq("lap"), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(product())));

        mockMvc.perform(get("/api/products/search").param("q", "lap"))
                .andExpect(status().isOk());

        verify(productService).searchProducts("lap", 0, 10, "createdAt", "desc");
    }

    @Test
    void deleteReturnsConfirmationMessage() throws Exception {
        mockMvc.perform(delete("/api/products/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));
    }

    @Test
    void insufficientStockMapsToBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Insufficient stock")).when(productService).updateStock("p1", -99);

        mockMvc.perform(put("/api/products/p1/stock").param("quantity", "-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient stock"));
    }
}
