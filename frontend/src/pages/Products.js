import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { addToCart, errorMessage, fetchProducts, searchProducts } from '../api';
import { getUser, isLoggedIn } from '../auth';
import './Products.css';

const Products = () => {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [notice, setNotice] = useState(null);
  const [addingId, setAddingId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    // Search is a product-service query, not a client-side filter, so it covers descriptions and
    // tags too. Debounced to avoid a request per keystroke.
    const timer = setTimeout(async () => {
      try {
        const term = searchTerm.trim();
        const page = term ? await searchProducts(term) : await fetchProducts();
        if (!cancelled) {
          setProducts(page.content || []);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(errorMessage(err, 'Failed to load products'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }, searchTerm ? 300 : 0);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [searchTerm]);

  const handleAddToCart = async (product) => {
    if (!isLoggedIn()) {
      navigate('/login');
      return;
    }
    setAddingId(product.id);
    setNotice(null);
    try {
      await addToCart(getUser().id, product.id, 1);
      setNotice({ type: 'success', text: `${product.name} added to your cart` });
    } catch (err) {
      setNotice({ type: 'error', text: errorMessage(err, 'Could not add to cart') });
    } finally {
      setAddingId(null);
    }
  };

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

        {notice && (
          <div className={notice.type === 'success' ? 'success-message' : 'error-message'}>
            {notice.text}
          </div>
        )}

        {loading && <div className="loading">Loading products...</div>}
        {error && <div className="error">{error}</div>}
        {!loading && !error && products.length === 0 && (
          <p>No products found{searchTerm ? ` for "${searchTerm}"` : ''}.</p>
        )}

        <div className="products-grid">
          {products.map((product) => (
            <div key={product.id} className="product-card">
              {product.imageUrl && <img src={product.imageUrl} alt={product.name} />}
              <div className="product-info">
                <h3>{product.name}</h3>
                <p className="description">{product.description}</p>
                <p className="price">${Number(product.price).toFixed(2)}</p>
                <p className="category">{product.category} - {product.brand}</p>
                <p className="stock">
                  {product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : 'Out of stock'}
                </p>
                <button
                  className="btn btn-primary"
                  onClick={() => handleAddToCart(product)}
                  disabled={addingId === product.id || product.stockQuantity < 1}
                >
                  {addingId === product.id ? 'Adding...' : 'Add to Cart'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Products;
