import React from 'react';
import { Link } from 'react-router-dom';
import './Home.css';

const Home = () => {
  return (
    <div className="home">
      <div className="container">
        <div className="hero-section">
          <h1>Welcome to E-Commerce</h1>
          <p>Discover amazing products with our microservices-powered platform</p>
          <Link to="/products" className="btn btn-primary">
            Shop Now
          </Link>
        </div>
        
        <div className="features">
          <div className="feature">
            <h3>Easy Shopping</h3>
            <p>Browse through our extensive product catalog</p>
          </div>
          <div className="feature">
            <h3>Secure Payments</h3>
            <p>Safe and secure payment processing</p>
          </div>
          <div className="feature">
            <h3>Fast Delivery</h3>
            <p>Quick and reliable order fulfillment</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;