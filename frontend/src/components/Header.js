import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { fetchCart } from '../api';
import { AUTH_CHANGED, CART_CHANGED, clearSession, getUser, isLoggedIn, subscribe } from '../auth';
import './Header.css';

const Header = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(getUser());
  const [itemCount, setItemCount] = useState(0);

  const refreshCartCount = useCallback(async () => {
    const current = getUser();
    if (!current) {
      setItemCount(0);
      return;
    }
    try {
      const cart = await fetchCart(current.id);
      setItemCount(cart.totalItems || 0);
    } catch {
      setItemCount(0);
    }
  }, []);

  useEffect(() => {
    const onAuth = () => {
      setUser(getUser());
      refreshCartCount();
    };
    const unsubscribeAuth = subscribe(AUTH_CHANGED, onAuth);
    const unsubscribeCart = subscribe(CART_CHANGED, refreshCartCount);
    refreshCartCount();
    return () => {
      unsubscribeAuth();
      unsubscribeCart();
    };
  }, [refreshCartCount]);

  const handleLogout = () => {
    clearSession();
    navigate('/');
  };

  return (
    <header className="header">
      <div className="container">
        <div className="header-content">
          <Link to="/" className="logo">
            <h1>E-Commerce</h1>
          </Link>
          <nav className="nav">
            <Link to="/" className="nav-link">Home</Link>
            <Link to="/products" className="nav-link">Products</Link>
            <Link to="/cart" className="nav-link">
              Cart{itemCount > 0 ? ` (${itemCount})` : ''}
            </Link>
            {isLoggedIn() && user ? (
              <>
                <Link to="/orders" className="nav-link">Orders</Link>
                <span className="nav-link">Hi, {user.firstName}</span>
                <button type="button" onClick={handleLogout} className="nav-link logout-btn">
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="nav-link">Login</Link>
                <Link to="/register" className="nav-link">Register</Link>
              </>
            )}
          </nav>
        </div>
      </div>
    </header>
  );
};

export default Header;
