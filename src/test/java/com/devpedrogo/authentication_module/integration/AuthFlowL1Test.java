package com.devpedrogo.authentication_module.integration;

import com.devpedrogo.authentication_module.domain.entity.RefreshToken;
import com.devpedrogo.authentication_module.domain.entity.User;
import com.devpedrogo.authentication_module.repository.RefreshTokenRepository;
import com.devpedrogo.authentication_module.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("Layer1")
@DisplayName("Fluxo de autenticação integrado")
class AuthFlowL1Test {

    private static final String AUTH_URL = "/api/v1/auth";
    private static final String PASSWORD = "senha-segura-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Cadastra usuário, persiste senha protegida e retorna tokens")
    void registerPersistsUserAndRefreshToken() throws Exception {
        String refreshToken = refreshToken(register("ana@example.com"));

        Optional<User> savedUser = userRepository.findByEmail("ana@example.com");
        assertTrue(savedUser.isPresent());
        assertTrue(passwordEncoder.matches(PASSWORD, savedUser.orElseThrow().getPassword()));
        assertNotEquals(PASSWORD, savedUser.orElseThrow().getPassword());

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow();
        assertEquals(savedUser.orElseThrow().getId(), savedToken.getUser().getId());
        assertFalse(savedToken.isRevoked());
    }

    @Test
    @DisplayName("Login autentica credenciais e revoga o refresh token anterior")
    void loginRotatesRefreshToken() throws Exception {
        String registrationToken = refreshToken(register("bia@example.com"));

        String loginToken = refreshToken(mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bia@example.com","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty()));

        assertNotEquals(registrationToken, loginToken);
        assertTrue(refreshTokenRepository.findByToken(registrationToken)
                .orElseThrow().isRevoked());
        assertFalse(refreshTokenRepository.findByToken(loginToken)
                .orElseThrow().isRevoked());
    }

    @Test
    @DisplayName("Renova os tokens e revoga o refresh token consumido")
    void refreshTokenRotatesTokens() throws Exception {
        String registrationToken = refreshToken(register("caio@example.com"));

        String refreshedToken = refreshToken(mockMvc.perform(
                        post(AUTH_URL + "/refresh-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken":"%s"}
                                        """.formatted(registrationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty()));

        assertNotEquals(registrationToken, refreshedToken);
        assertTrue(refreshTokenRepository.findByToken(registrationToken)
                .orElseThrow().isRevoked());
        assertFalse(refreshTokenRepository.findByToken(refreshedToken)
                .orElseThrow().isRevoked());
    }

    @Test
    @DisplayName("Rejeita requisição de cadastro com campos inválidos")
    void registerRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post(AUTH_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"email-invalido","password":"123"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    @DisplayName("Rejeita cadastro com e-mail já utilizado")
    void registerRejectsDuplicateEmail() throws Exception {
        register("duda@example.com").andExpect(status().isOk());

        ServletException exception = assertThrows(ServletException.class, () ->
                mockMvc.perform(post(AUTH_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duda","email":"duda@example.com","password":"outra-senha"}
                                """)));

        assertEquals("E-mail já cadastrado no sistema.", exception.getCause().getMessage());
        assertEquals(1, userRepository.count());
    }

    @Test
    @DisplayName("Rejeita login com senha incorreta")
    void loginRejectsIncorrectPassword() throws Exception {
        register("eva@example.com").andExpect(status().isOk());

        mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"eva@example.com","password":"senha-incorreta"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rejeita refresh token que não existe")
    void refreshTokenRejectsUnknownToken() throws Exception {
        ServletException exception = assertThrows(ServletException.class, () ->
                mockMvc.perform(post(AUTH_URL + "/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"token-inexistente"}
                                """)));

        assertEquals("Refresh token não encontrado no banco de dados.", exception.getCause().getMessage());
    }

    private ResultActions register(String email) throws Exception {
        return mockMvc.perform(post(AUTH_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Usuário de teste","email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty());
    }

    private String refreshToken(ResultActions result) throws Exception {
        return JsonPath.read(
                result.andReturn().getResponse().getContentAsString(),
                "$.refresh_token"
        );
    }
}
