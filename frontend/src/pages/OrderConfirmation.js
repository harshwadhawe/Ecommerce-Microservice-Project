import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { errorMessage, fetchOrder } from '../api';
import './OrderConfirmation.css';

const OrderConfirmation = () => {
  // The order id is in the URL rather than in router state, so a refresh or a shared link still
  // resolves -- order-service is the source of truth now, not whatever the previous page passed.
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchOrder(orderId)
      .then(setOrder)
      .catch((err) => setError(errorMessage(err, 'Could not load this order')));
  }, [orderId]);

  if (error) {
    return (
      <div className="order-confirmation">
        <div className="container">
          <div className="confirmation-content">
            <h1>Order unavailable</h1>
            <div className="error-message">{error}</div>
            <div className="actions">
              <Link to="/orders" className="btn btn-primary">View your orders</Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!order) return <div className="loading">Loading your order...</div>;

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
                <span className="label">Order Number:</span>
                <span className="value">{order.orderNumber}</span>
              </div>
              <div className="info-item">
                <span className="label">Transaction ID:</span>
                <span className="value">{order.paymentTransactionId}</span>
              </div>
              <div className="info-item">
                <span className="label">Total Amount:</span>
                <span className="value">${Number(order.totalAmount).toFixed(2)}</span>
              </div>
              <div className="info-item">
                <span className="label">Status:</span>
                <span className="value">{order.status}</span>
              </div>
              <div className="info-item">
                <span className="label">Ships to:</span>
                <span className="value">
                  {order.recipientName}, {order.address}, {order.city} {order.postalCode}, {order.country}
                </span>
              </div>
            </div>

            <div className="order-items">
              <h3>Items</h3>
              {order.items.map((item) => (
                <div key={item.productId} className="info-item">
                  <span className="label">{item.productName} x{item.quantity}</span>
                  <span className="value">${(Number(item.price) * item.quantity).toFixed(2)}</span>
                </div>
              ))}
            </div>

            <div className="actions">
              <Link to="/orders" className="btn btn-primary">
                View All Orders
              </Link>
              <Link to="/products" className="btn btn-secondary">
                Continue Shopping
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmation;
