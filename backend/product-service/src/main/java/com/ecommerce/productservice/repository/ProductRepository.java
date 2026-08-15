package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    List<Product> findByCategory(String category);
    
    List<Product> findByBrand(String brand);
    
    List<Product> findByIsActiveTrue();
    
    @Query("{'name': {'$regex': ?0, '$options': 'i'}}")
    List<Product> findByNameContainingIgnoreCase(String name);
    
    @Query("{'category': ?0, 'isActive': true}")
    Page<Product> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    @Query("{'brand': ?0, 'isActive': true}")
    Page<Product> findByBrandAndIsActiveTrue(String brand, Pageable pageable);
    
    @Query("{'price': {'$gte': ?0, '$lte': ?1}, 'isActive': true}")
    Page<Product> findByPriceBetweenAndIsActiveTrue(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    @Query("{'$or': [{'name': {'$regex': ?0, '$options': 'i'}}, {'description': {'$regex': ?0, '$options': 'i'}}, {'tags': {'$regex': ?0, '$options': 'i'}}], 'isActive': true}")
    Page<Product> findBySearchTermAndIsActiveTrue(String searchTerm, Pageable pageable);
    
    @Query("{'stockQuantity': {'$gt': 0}, 'isActive': true}")
    Page<Product> findInStockProducts(Pageable pageable);
    
    List<Product> findByIdIn(List<String> ids);
}