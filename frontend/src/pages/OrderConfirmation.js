import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './OrderConfirmation.css';

const OrderConfirmation = () => {
  const location = useLocation();
  const { orderId, total } = location.state || { orderId: 'ORD-12345', total: 1699.97 };

  return (
    <div className="order-confirmation">
      <div className="container">
        <div className="confirmation-content">
          <div className="success-icon">
            <span>✓</span>
          </div>
          
          <h1>Order Confirmed!</h1>
          
          <div className="order-details">
            <p>Thank you for your order. Your order has been successfully placed.</p>
            
            <div className="order-info">
              <div className="info-item">
                <span className="label">Order ID:</span>
                <span className="value">{orderId}</span>
              </div>
              <div className="info-item">
                <span className="label">Total Amount:</span>
                <span className="value">${total.toFixed(2)}</span>
              </div>
              <div className="info-item">
                <span className="label">Status:</span>
                <span className="value">Processing</span>
              </div>
            </div>
            
            <div className="next-steps">
              <h3>What's next?</h3>
              <ul>
                <li>You will receive an email confirmation shortly</li>
                <li>Your order will be processed within 24 hours</li>
                <li>You'll get a tracking number once your order ships</li>
                <li>Estimated delivery: 3-5 business days</li>
              </ul>
            </div>
            
            <div className="actions">
              <Link to="/products" className="btn btn-primary">
                Continue Shopping
              </Link>
              <Link to="/" className="btn btn-secondary">
                Back to Home
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmation;