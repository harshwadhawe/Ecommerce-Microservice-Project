import axios from 'axios';
import { getToken, clearSession, cartChanged } from './auth';

// Each service is addressed directly -- there is no gateway. CRA inlines these at build time, so
// they must be referenced literally; a computed process.env[...] lookup resolves to undefined.
const USER_URL = process.env.REACT_APP_USER_SERVICE_URL || 'http://localhost:8081';
const PRODUCT_URL = process.env.REACT_APP_PRODUCT_SERVICE_URL || 'http://localhost:8082';
const CART_URL = process.env.REACT_APP_CART_SERVICE_URL || 'http://localhost:8083';
const PAYMENT_URL = process.env.REACT_APP_PAYMENT_SERVICE_URL || 'http://localhost:8085';

const client = (baseURL) => {
  const instance = axios.create({ baseURL });

  instance.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      // An expired or rejected token should not leave a half-logged-in UI behind.
      if (error.response?.status === 401) {
        clearSession();
      }
      return Promise.reject(error);
    }
  );

  return instance;
};

const users = client(USER_URL);
const products = client(PRODUCT_URL);
const carts = client(CART_URL);
const payments = client(PAYMENT_URL);

/**
 * Services answer with {error: "..."} for mapped failures and {field: "message"} for validation
 * errors, so pull whichever is present rather than showing "Request failed with status code 400".
 */
export const errorMessage = (error, fallback = 'Something went wrong. Please try again.') => {
  const data = error.response?.data;
  if (!data) return error.request ? 'Cannot reach the service. Is it running?' : fallback;
  if (typeof data === 'string') return data;
  if (data.error) return data.error;
  const firstField = Object.values(data)[0];
  return typeof firstField === 'string' ? firstField : fallback;
};

export const registerUser = (payload) =>
  users.post('/api/users/register', payload).then((r) => r.data);

export const loginUser = (email, password) =>
  users.post('/api/users/login', { email, password }).then((r) => r.data);

export const fetchProducts = (page = 0, size = 12) =>
  products.get('/api/products/active', { params: { page, size } }).then((r) => r.data);

export const searchProducts = (q, page = 0, size = 12) =>
  products.get('/api/products/search', { params: { q, page, size } }).then((r) => r.data);

export const fetchCart = (userId) =>
  carts.get(`/api/cart/${userId}`).then((r) => r.data);

const mutateCart = (request) =>
  request.then((r) => {
    cartChanged();
    return r.data;
  });

export const addToCart = (userId, productId, quantity = 1) =>
  mutateCart(carts.post(`/api/cart/${userId}/items`, { productId, quantity }));

export const updateCartItem = (userId, productId, quantity) =>
  mutateCart(carts.put(`/api/cart/${userId}/items/${productId}`, { quantity }));

export const removeCartItem = (userId, productId) =>
  mutateCart(carts.delete(`/api/cart/${userId}/items/${productId}`));

export const clearCart = (userId) =>
  mutateCart(carts.delete(`/api/cart/${userId}`));

export const processPayment = (payload) =>
  payments.post('/api/payment/process', payload).then((r) => r.data);
