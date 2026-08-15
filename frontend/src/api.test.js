// axios v1 ships ESM that CRA's jest transform will not parse. Nothing here makes a request, so a
// stub of the only surface api.js touches at import time is enough.
jest.mock('axios', () => ({
  create: () => ({
    interceptors: { request: { use: () => {} }, response: { use: () => {} } },
  }),
}));

import { errorMessage } from './api';
import { clearSession, getToken, getUser, isLoggedIn, saveSession } from './auth';

// Services answer failures in three different shapes; showing "Request failed with status code 400"
// instead of the real reason is the failure mode this guards against.
describe('errorMessage', () => {
  test('uses the mapped {error} body', () => {
    const err = { response: { data: { error: 'Insufficient stock available' } } };
    expect(errorMessage(err)).toBe('Insufficient stock available');
  });

  test('uses the first field of a validation body', () => {
    const err = { response: { data: { password: 'Password must be at least 6 characters' } } };
    expect(errorMessage(err)).toBe('Password must be at least 6 characters');
  });

  test('reports an unreachable service distinctly from an unknown failure', () => {
    expect(errorMessage({ request: {} })).toMatch(/Cannot reach the service/);
    expect(errorMessage({}, 'fallback text')).toBe('fallback text');
  });
});

describe('session', () => {
  afterEach(() => clearSession());

  test('round-trips the token and user', () => {
    saveSession('a.b.c', { id: 7, firstName: 'Ada' });

    expect(getToken()).toBe('a.b.c');
    expect(getUser().id).toBe(7);
    expect(isLoggedIn()).toBe(true);
  });

  test('clearing leaves no logged-in remnants', () => {
    saveSession('a.b.c', { id: 7 });
    clearSession();

    expect(getToken()).toBeNull();
    expect(getUser()).toBeNull();
    expect(isLoggedIn()).toBe(false);
  });

  test('a corrupted user record does not throw', () => {
    localStorage.setItem('authUser', 'not json');
    expect(getUser()).toBeNull();
  });
});
