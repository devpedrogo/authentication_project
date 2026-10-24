package com.devpedrogo.authentication_module.service;

import com.devpedrogo.authentication_module.domain.entity.RefreshToken;
import com.devpedrogo.authentication_module.domain.entity.User;
import com.devpedrogo.authentication_module.repository.RefreshTokenRepository;
import com.devpedrogo.authentication_module.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de refresh token")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 3_600_000L);
        user = User.builder().id(UUID.randomUUID()).email("ana@example.com").build();
    }

    @Test
    @DisplayName("Revoga o token anterior e persiste um novo token")
    void createRefreshTokenRevokesPreviousTokenAndPersistsNewOne() {
        RefreshToken previousToken = token("previous-token", user, Instant.now().plusSeconds(3600), false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserAndRevokedFalse(user)).thenReturn(Optional.of(previousToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforeCreation = Instant.now();
        RefreshToken created = refreshTokenService.createRefreshToken(user.getId());
        Instant afterCreation = Instant.now();

        assertTrue(previousToken.isRevoked());
        assertNotNull(created.getToken());
        assertEquals(user, created.getUser());
        assertFalse(created.isRevoked());
        assertTrue(created.getExpiryDate().isAfter(beforeCreation.plusSeconds(3599)));
        assertTrue(created.getExpiryDate().isBefore(afterCreation.plusSeconds(3601)));
        ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(savedTokens.capture());
        assertSame(previousToken, savedTokens.getAllValues().get(0));
        assertSame(created, savedTokens.getAllValues().get(1));
    }

    @Test
    @DisplayName("Não cria token quando o usuário não existe")
    void createRefreshTokenFailsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.createRefreshToken(userId)
        );

        assertEquals("Usuário não encontrado com id: " + userId, exception.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Retorna um token válido e não revogado")
    void verifyExpirationReturnsAValidToken() {
        RefreshToken token = token("valid-token", user, Instant.now().plusSeconds(60), false);

        assertSame(token, refreshTokenService.verifyExpiration(token));
    }

    @Test
    @DisplayName("Remove e rejeita um token expirado")
    void verifyExpirationDeletesAndRejectsAnExpiredToken() {
        RefreshToken token = token("expired-token", user, Instant.now().minusSeconds(1), false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.verifyExpiration(token)
        );

        assertEquals("Refresh token expirado. Por favor, faça login novamente.", exception.getMessage());
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("Rejeita um token revogado")
    void verifyExpirationRejectsARevokedToken() {
        RefreshToken token = token("revoked-token", user, Instant.now().plusSeconds(60), true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.verifyExpiration(token)
        );

        assertEquals("Refresh token revogado. Por favor, faça login novamente.", exception.getMessage());
        verify(refreshTokenRepository, never()).delete(token);
    }

    @Test
    @DisplayName("Revoga um token existente")
    void revokeTokenMarksExistingTokenAsRevoked() {
        RefreshToken token = token("refresh-token", user, Instant.now().plusSeconds(60), false);
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken(token.getToken());

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("Não altera nada quando o token não existe")
    void revokeTokenDoesNothingWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        refreshTokenService.revokeToken("unknown");

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    private RefreshToken token(String value, User owner, Instant expiration, boolean revoked) {
        return RefreshToken.builder()
                .token(value)
                .user(owner)
                .expiryDate(expiration)
                .revoked(revoked)
                .build();
    }
}
