import React, { useState, useEffect } from 'react';
import './Products.css';

const Products = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      // This would normally call your product service API
      // For now, we'll simulate with mock data
      setTimeout(() => {
        setProducts([
          {
            id: '1',
            name: 'Laptop',
            description: 'High-performance laptop',
            price: 999.99,
            category: 'Electronics',
            brand: 'TechBrand',
            imageUrl: '/api/placeholder/300/200'
          },
          {
            id: '2',
            name: 'Smartphone',
            description: 'Latest smartphone with great features',
            price: 699.99,
            category: 'Electronics',
            brand: 'PhoneBrand',
            imageUrl: '/api/placeholder/300/200'
          },
          {
            id: '3',
            name: 'Headphones',
            description: 'Wireless noise-cancelling headphones',
            price: 199.99,
            category: 'Electronics',
            brand: 'AudioBrand',
            imageUrl: '/api/placeholder/300/200'
          }
        ]);
        setLoading(false);
      }, 1000);
    } catch (err) {
      setError('Failed to fetch products');
      setLoading(false);
    }
  };

  const filteredProducts = products.filter(product =>
    product.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    product.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) return <div className="loading">Loading products...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="products">
      <div className="container">
        <h1>Products</h1>
        
        <div className="search-bar">
          <input
            type="text"
            placeholder="Search products..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="form-control"
          />
        </div>

        <div className="products-grid">
          {filteredProducts.map(product => (
            <div key={product.id} className="product-card">
              <img src={product.imageUrl} alt={product.name} />
              <div className="product-info">
                <h3>{product.name}</h3>
                <p className="description">{product.description}</p>
                <p className="price">${product.price}</p>
                <p className="category">{product.category} - {product.brand}</p>
                <button className="btn btn-primary">Add to Cart</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Products;