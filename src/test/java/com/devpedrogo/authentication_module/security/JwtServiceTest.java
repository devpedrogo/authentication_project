package com.devpedrogo.authentication_module.security;

import com.devpedrogo.authentication_module.domain.entity.User;
import com.devpedrogo.authentication_module.domain.enums.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes do serviço de tokens JWT")
class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                "VGhpc0lzQVN0YWJsZVRlc3RTZWNyZXRLZXlGb3JKV1RTaWduaW5n"
        );
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    @Test
    @DisplayName("Gera token com identidade e papel do usuário")
    void generateTokenIncludesUserIdentityAndRole() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("ana@example.com")
                .role(UserRole.USER)
                .build();

        String token = jwtService.generateToken(user);

        assertEquals("ana@example.com", jwtService.extractUsername(token));
        assertEquals(userId.toString(), jwtService.extractClaim(token, claims -> claims.get("id", String.class)));
        assertEquals("USER", jwtService.extractClaim(token, claims -> claims.get("role", String.class)));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("Considera inválido um token de outro usuário")
    void tokenIsInvalidForAnotherUser() {
        User tokenOwner = User.builder()
                .id(UUID.randomUUID())
                .email("ana@example.com")
                .role(UserRole.USER)
                .build();
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .email("bia@example.com")
                .role(UserRole.USER)
                .build();

        String token = jwtService.generateToken(tokenOwner);

        assertFalse(jwtService.isTokenValid(token, anotherUser));
    }

    @Test
    @DisplayName("Lança exceção ao validar um token expirado")
    void tokenIsInvalidAfterExpiration() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ana@example.com")
                .role(UserRole.USER)
                .build();

        String token = jwtService.generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(token, user));
    }
}
