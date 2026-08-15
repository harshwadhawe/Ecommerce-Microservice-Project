// Session lives in localStorage: the JWT plus the user record returned by login. cart-service
// authorizes on the numeric id in the token, so the id is what every cart call is keyed on.
const TOKEN_KEY = 'authToken';
const USER_KEY = 'authUser';

export const AUTH_CHANGED = 'auth-changed';
export const CART_CHANGED = 'cart-changed';

const announce = (event) => window.dispatchEvent(new Event(event));

export const getToken = () => localStorage.getItem(TOKEN_KEY);

export const getUser = () => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || null;
  } catch {
    return null;
  }
};

export const isLoggedIn = () => Boolean(getToken());

export const saveSession = (token, user) => {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  announce(AUTH_CHANGED);
};

export const clearSession = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  announce(AUTH_CHANGED);
};

export const cartChanged = () => announce(CART_CHANGED);

// Returns an unsubscribe function, so effects can clean up.
export const subscribe = (event, handler) => {
  window.addEventListener(event, handler);
  return () => window.removeEventListener(event, handler);
};
