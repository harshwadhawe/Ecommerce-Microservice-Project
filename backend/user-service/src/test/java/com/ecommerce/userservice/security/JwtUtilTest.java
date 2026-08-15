package com.ecommerce.userservice.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256!!";

    private JwtUtil jwtUtil;
    private UserDetails user;

    private JwtUtil jwtUtilWithExpiration(long expirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", SECRET);
        ReflectionTestUtils.setField(util, "expiration", expirationMs);
        return util;
    }

    @BeforeEach
    void setUp() {
        jwtUtil = jwtUtilWithExpiration(86400000L);
        user = new User("a@b.com", "hashed", Collections.emptyList());
    }

    @Test
    void tokenRoundTripsTheSubject() {
        String token = jwtUtil.generateToken(user);

        assertEquals("a@b.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token, user));
    }

    @Test
    void tokenIsRejectedForADifferentUser() {
        String token = jwtUtil.generateToken(user);

        UserDetails other = new User("other@b.com", "hashed", Collections.emptyList());

        assertFalse(jwtUtil.validateToken(token, other));
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = jwtUtilWithExpiration(-1000L).generateToken(user);

        assertThrows(JwtException.class, () -> jwtUtil.validateToken(expired, user));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtil attacker = new JwtUtil();
        ReflectionTestUtils.setField(attacker, "secret", "a-completely-different-secret-key-of-length!!");
        ReflectionTestUtils.setField(attacker, "expiration", 86400000L);
        String forged = attacker.generateToken(user);

        assertThrows(JwtException.class, () -> jwtUtil.extractUsername(forged));
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = jwtUtil.generateToken(user);
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".bm90LWEtc2lnbmF0dXJl";

        assertThrows(JwtException.class, () -> jwtUtil.extractUsername(tampered));
    }

    @Test
    void expirationIsInTheFuture() {
        String token = jwtUtil.generateToken(user);

        assertTrue(jwtUtil.extractExpiration(token).getTime() > System.currentTimeMillis());
    }
}
