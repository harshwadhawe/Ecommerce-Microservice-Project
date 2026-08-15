import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './OrderConfirmation.css';

const OrderConfirmation = () => {
  const location = useLocation();
  const { orderId, transactionId, total, status } = location.state || {};

  // Reached by typing the URL or refreshing: there is no order to look up, because no
  // order-service stores one.
  if (!orderId) {
    return (
      <div className="order-confirmation">
        <div className="container">
          <div className="confirmation-content">
            <h1>No recent order</h1>
            <p>Order receipts are only shown right after checkout.</p>
            <div className="actions">
              <Link to="/products" className="btn btn-primary">Continue Shopping</Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

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
                <span className="label">Transaction ID:</span>
                <span className="value">{transactionId}</span>
              </div>
              <div className="info-item">
                <span className="label">Total Amount:</span>
                <span className="value">${Number(total || 0).toFixed(2)}</span>
              </div>
              <div className="info-item">
                <span className="label">Payment:</span>
                <span className="value">{status}</span>
              </div>
            </div>
            
            <div className="next-steps">
              <h3>What's next?</h3>
              <ul>
                <li>Your payment was processed and your cart has been emptied</li>
                <li>Keep the transaction ID above for reference</li>
                <li>Order history is not available yet — order-service is not implemented</li>
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