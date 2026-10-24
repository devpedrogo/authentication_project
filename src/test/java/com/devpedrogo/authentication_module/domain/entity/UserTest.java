package com.devpedrogo.authentication_module.domain.entity;

import com.devpedrogo.authentication_module.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes da entidade de usuário")
class UserTest {

    @Test
    @DisplayName("Concede a autoridade de usuário a um usuário comum")
    void regularUserHasUserAuthority() {
        User user = User.builder().role(UserRole.USER).build();

        assertEquals(
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")),
                user.getAuthorities()
        );
    }

    @Test
    @DisplayName("Concede autoridades de administrador e usuário a um administrador")
    void administratorHasAdminAndUserAuthorities() {
        User user = User.builder().role(UserRole.ADMIN).build();

        assertEquals(
                java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER")
                ),
                user.getAuthorities()
        );
    }

    @Test
    @DisplayName("Retorna corretamente os estados da conta")
    void accountFlagsAreReturnedByUserDetailsContract() {
        User user = User.builder().enabled(false).accountNonLocked(false).build();

        assertFalse(user.isEnabled());
        assertFalse(user.isAccountNonLocked());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isCredentialsNonExpired());
    }
}
