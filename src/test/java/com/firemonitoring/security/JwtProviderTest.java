package com.firemonitoring.security;

import com.firemonitoring.entity.User;
import com.firemonitoring.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {
    @Test
    void createsTokenFromUserWithExpectedClaims() {
        JwtProvider provider = new JwtProvider("test-secret-with-at-least-32-bytes", "test-issuer", 60);
        User user = new User();
        user.setId(42L);
        user.setUsername("test-user");
        user.setRole(UserRole.MEMBER);
        user.setPassword("private-password-hash");

        String token = provider.generateToken(user);
        var jwt = provider.getJwtDecoder().decode(token);

        assertEquals("test-user", jwt.getSubject());
        assertEquals(42L, ((Number) jwt.getClaim("userId")).longValue());
        assertEquals(List.of("ROLE_MEMBER"), jwt.getClaimAsStringList("roles"));
        assertEquals("test-issuer", jwt.getClaimAsString("iss"));
        assertEquals(jwt.getIssuedAt().plusSeconds(3600), jwt.getExpiresAt());
        assertFalse(jwt.getClaims().containsKey("password"));
        assertTrue(provider.isTokenValid(token));
    }
}
