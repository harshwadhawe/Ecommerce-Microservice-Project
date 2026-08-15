import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { clearCart, errorMessage, fetchCart, removeCartItem, updateCartItem } from '../api';
import { getUser, isLoggedIn } from '../auth';
import './Cart.css';

const Cart = () => {
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setCart(await fetchCart(getUser().id));
      setError(null);
    } catch (err) {
      setError(errorMessage(err, 'Could not load your cart'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isLoggedIn()) {
      navigate('/login');
      return;
    }
    load();
  }, [load, navigate]);

  // Quantity changes are a cart-service call, not local state: it re-checks stock and can refuse.
  const runAction = async (action) => {
    setBusy(true);
    setError(null);
    try {
      setCart(await action());
    } catch (err) {
      setError(errorMessage(err, 'Could not update your cart'));
      await load();
    } finally {
      setBusy(false);
    }
  };

  const changeQuantity = (productId, quantity) =>
    runAction(() => updateCartItem(getUser().id, productId, quantity));

  const removeItem = (productId) =>
    runAction(() => removeCartItem(getUser().id, productId));

  const emptyCart = () =>
    runAction(async () => {
      await clearCart(getUser().id);
      return fetchCart(getUser().id);
    });

  if (loading) return <div className="loading">Loading your cart...</div>;

  const items = cart?.items || [];

  if (items.length === 0) {
    return (
      <div className="cart empty-cart">
        <div className="container">
          <h1>Shopping Cart</h1>
          {error && <div className="error-message">{error}</div>}
          <p>Your cart is empty</p>
          <Link to="/products" className="btn btn-primary">
            Continue Shopping
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="cart">
      <div className="container">
        <h1>Shopping Cart ({cart.totalItems} items)</h1>

        {error && <div className="error-message">{error}</div>}

        <div className="cart-content">
          <div className="cart-items">
            {items.map((item) => (
              <div key={item.productId} className="cart-item">
                {item.imageUrl && <img src={item.imageUrl} alt={item.productName} />}
                <div className="item-info">
                  <h3>{item.productName}</h3>
                  <p className="price">${Number(item.price).toFixed(2)}</p>
                </div>
                <div className="quantity-controls">
                  <button
                    onClick={() => changeQuantity(item.productId, item.quantity - 1)}
                    className="btn btn-secondary"
                    disabled={busy}
                  >
                    -
                  </button>
                  <span className="quantity">{item.quantity}</span>
                  <button
                    onClick={() => changeQuantity(item.productId, item.quantity + 1)}
                    className="btn btn-secondary"
                    disabled={busy}
                  >
                    +
                  </button>
                </div>
                <div className="item-total">
                  ${(Number(item.price) * item.quantity).toFixed(2)}
                </div>
                <button
                  onClick={() => removeItem(item.productId)}
                  className="btn btn-danger remove-btn"
                  disabled={busy}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>

          <div className="cart-summary">
            <h3>Order Summary</h3>
            <div className="summary-row">
              <span>Subtotal:</span>
              <span>${Number(cart.totalAmount).toFixed(2)}</span>
            </div>
            <div className="summary-row">
              <span>Shipping:</span>
              <span>Free</span>
            </div>
            <div className="summary-row total">
              <span>Total:</span>
              <span>${Number(cart.totalAmount).toFixed(2)}</span>
            </div>
            <Link to="/checkout" className="btn btn-success checkout-btn">
              Proceed to Checkout
            </Link>
            <button onClick={emptyCart} className="btn btn-secondary" disabled={busy}>
              Clear Cart
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Cart;
