import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage, fetchOrders } from '../api';
import { isLoggedIn } from '../auth';
import './Orders.css';

const STATUS_LABELS = {
  PENDING: 'Pending',
  PAID: 'Paid',
  PAYMENT_FAILED: 'Payment failed',
  CANCELLED: 'Cancelled',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered'
};

const Orders = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isLoggedIn()) {
      navigate('/login');
      return;
    }
    fetchOrders()
      .then(setOrders)
      .catch((err) => setError(errorMessage(err, 'Could not load your orders')))
      .finally(() => setLoading(false));
  }, [navigate]);

  if (loading) return <div className="loading">Loading your orders...</div>;

  return (
    <div className="orders">
      <div className="container">
        <h1>Your Orders</h1>

        {error && <div className="error-message">{error}</div>}

        {orders.length === 0 && !error && (
          <div className="empty-orders">
            <p>You have not placed any orders yet.</p>
            <Link to="/products" className="btn btn-primary">Start Shopping</Link>
          </div>
        )}

        {orders.map((order) => (
          <div key={order.id} className="order-card">
            <div className="order-header">
              <div>
                <h3>{order.orderNumber}</h3>
                <p className="order-date">{new Date(order.createdAt).toLocaleString()}</p>
              </div>
              <div className="order-header-right">
                <span className={`status status-${order.status.toLowerCase()}`}>
                  {STATUS_LABELS[order.status] || order.status}
                </span>
                <span className="order-total">${Number(order.totalAmount).toFixed(2)}</span>
              </div>
            </div>

            <div className="order-lines">
              {order.items.map((item) => (
                <div key={item.productId} className="order-line">
                  <span>{item.productName} x{item.quantity}</span>
                  <span>${(Number(item.price) * item.quantity).toFixed(2)}</span>
                </div>
              ))}
            </div>

            {order.status === 'PAYMENT_FAILED' && order.paymentMessage && (
              <p className="payment-message">Declined: {order.paymentMessage}</p>
            )}

            <Link to={`/order-confirmation/${order.id}`} className="btn btn-secondary">
              View details
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Orders;
