package com.devpedrogo.authentication_module.service;

import com.devpedrogo.authentication_module.domain.entity.RefreshToken;
import com.devpedrogo.authentication_module.domain.entity.User;
import com.devpedrogo.authentication_module.domain.enums.UserRole;
import com.devpedrogo.authentication_module.dto.AuthenticationResponse;
import com.devpedrogo.authentication_module.dto.LoginRequest;
import com.devpedrogo.authentication_module.dto.RefreshTokenRequest;
import com.devpedrogo.authentication_module.dto.RegisterRequest;
import com.devpedrogo.authentication_module.repository.RefreshTokenRepository;
import com.devpedrogo.authentication_module.repository.UserRepository;
import com.devpedrogo.authentication_module.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de autenticação")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Ana")
                .email("ana@example.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .enabled(true)
                .accountNonLocked(true)
                .build();
    }

    @Test
    @DisplayName("Cadastra usuário e retorna tokens")
    void registerCreatesUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "password123");
        RefreshToken refreshToken = refreshToken("refresh-token", user);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(user.getId());
            return savedUser;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

        AuthenticationResponse response = authService.register(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals(request.name(), savedUser.getValue().getName());
        assertEquals(request.email(), savedUser.getValue().getEmail());
        assertEquals("encoded-password", savedUser.getValue().getPassword());
        assertEquals(UserRole.USER, savedUser.getValue().getRole());
        verify(jwtService).generateToken(savedUser.getValue());
        verify(refreshTokenService).createRefreshToken(user.getId());
    }

    @Test
    @DisplayName("Rejeita cadastro com e-mail já existente")
    void registerRejectsAnAlreadyRegisteredEmail() {
        RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));

        assertEquals("E-mail já cadastrado no sistema.", exception.getMessage());
    }

    @Test
    @DisplayName("Autentica usuário e retorna tokens")
    void loginAuthenticatesUserAndReturnsTokens() {
        LoginRequest request = new LoginRequest("ana@example.com", "password123");
        RefreshToken refreshToken = refreshToken("refresh-token", user);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

        AuthenticationResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
    }

    @Test
    @DisplayName("Renova os tokens do usuário associado ao refresh token")
    void refreshTokenRotatesTokensForTheTokenOwner() {
        RefreshToken currentToken = refreshToken("old-refresh-token", user);
        RefreshToken replacementToken = refreshToken("new-refresh-token", user);
        when(refreshTokenRepository.findByToken(currentToken.getToken())).thenReturn(Optional.of(currentToken));
        when(refreshTokenService.verifyExpiration(currentToken)).thenReturn(currentToken);
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(replacementToken);

        AuthenticationResponse response = authService.refreshToken(
                new RefreshTokenRequest(currentToken.getToken())
        );

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        verify(refreshTokenService).verifyExpiration(currentToken);
    }

    @Test
    @DisplayName("Rejeita refresh token inexistente")
    void refreshTokenRejectsUnknownToken() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.refreshToken(new RefreshTokenRequest("unknown"))
        );

        assertEquals("Refresh token não encontrado no banco de dados.", exception.getMessage());
    }

    private RefreshToken refreshToken(String token, User owner) {
        return RefreshToken.builder()
                .token(token)
                .user(owner)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
    }
}
