import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorMessage, fetchCart, placeOrder } from '../api';
import { getUser, isLoggedIn } from '../auth';
import './Checkout.css';

const Checkout = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    // Shipping Info
    firstName: '',
    lastName: '',
    address: '',
    city: '',
    country: '',
    postalCode: '',
    // Payment Info
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: ''
  });
  const [processing, setProcessing] = useState(false);
  const [cart, setCart] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isLoggedIn()) {
      navigate('/login');
      return;
    }
    fetchCart(getUser().id)
      .then((loaded) => {
        if (!loaded.items || loaded.items.length === 0) {
          navigate('/cart');
          return;
        }
        setCart(loaded);
      })
      .catch((err) => setError(errorMessage(err, 'Could not load your cart')));
  }, [navigate]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setProcessing(true);
    
    try {
      // order-service reads the cart itself, charges payment-service, records the order and
      // empties the cart -- the browser is not trusted with the amount.
      const order = await placeOrder({
        recipientName: `${formData.firstName} ${formData.lastName}`.trim(),
        address: formData.address,
        city: formData.city,
        country: formData.country,
        postalCode: formData.postalCode,
        cardholderName: formData.cardholderName,
        cardNumber: formData.cardNumber.replace(/\s/g, ''),
        expiryDate: formData.expiryDate,
        cvv: formData.cvv
      });

      navigate(`/order-confirmation/${order.id}`);
    } catch (err) {
      // 402 means the card was declined: the order exists, marked PAYMENT_FAILED, and the cart is
      // still intact so the shopper can try again.
      setError(errorMessage(err, 'Payment failed. Please try again.'));
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="checkout">
      <div className="container">
        <h1>Checkout</h1>

        {error && <div className="error-message">{error}</div>}
        
        <div className="checkout-content">
          <form onSubmit={handleSubmit} className="checkout-form">
            <div className="form-section">
              <h3>Shipping Information</h3>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">First Name</label>
                  <input
                    type="text"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleInputChange}
                    className="form-control"
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Last Name</label>
                  <input
                    type="text"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleInputChange}
                    className="form-control"
                    required
                  />
                </div>
              </div>
              
              <div className="form-group">
                <label className="form-label">Address</label>
                <input
                  type="text"
                  name="address"
                  value={formData.address}
                  onChange={handleInputChange}
                  className="form-control"
                  required
                />
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">City</label>
                  <input
                    type="text"
                    name="city"
                    value={formData.city}
                    onChange={handleInputChange}
                    className="form-control"
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Country</label>
                  <input
                    type="text"
                    name="country"
                    value={formData.country}
                    onChange={handleInputChange}
                    className="form-control"
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Postal Code</label>
                  <input
                    type="text"
                    name="postalCode"
                    value={formData.postalCode}
                    onChange={handleInputChange}
                    className="form-control"
                    required
                  />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Payment Information</h3>
              <div className="form-group">
                <label className="form-label">Cardholder Name</label>
                <input
                  type="text"
                  name="cardholderName"
                  value={formData.cardholderName}
                  onChange={handleInputChange}
                  className="form-control"
                  required
                />
              </div>
              
              <div className="form-group">
                <label className="form-label">Card Number</label>
                <input
                  type="text"
                  name="cardNumber"
                  value={formData.cardNumber}
                  onChange={handleInputChange}
                  className="form-control"
                  placeholder="1234 5678 9012 3456"
                  required
                />
              </div>
              
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Expiry Date</label>
                  <input
                    type="text"
                    name="expiryDate"
                    value={formData.expiryDate}
                    onChange={handleInputChange}
                    className="form-control"
                    placeholder="MM/YY"
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">CVV</label>
                  <input
                    type="text"
                    name="cvv"
                    value={formData.cvv}
                    onChange={handleInputChange}
                    className="form-control"
                    placeholder="123"
                    required
                  />
                </div>
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-success place-order-btn"
              disabled={processing || !cart}
            >
              {processing ? 'Processing...' : 'Place Order'}
            </button>
          </form>

          <div className="order-summary">
            <h3>Order Summary</h3>
            {(cart?.items || []).map((item) => (
              <div key={item.productId} className="summary-item">
                <span>{item.productName} x{item.quantity}</span>
                <span>${(Number(item.price) * item.quantity).toFixed(2)}</span>
              </div>
            ))}
            <div className="summary-item">
              <span>Shipping</span>
              <span>Free</span>
            </div>
            <div className="summary-item total">
              <span>Total</span>
              <span>${Number(cart?.totalAmount || 0).toFixed(2)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Checkout;