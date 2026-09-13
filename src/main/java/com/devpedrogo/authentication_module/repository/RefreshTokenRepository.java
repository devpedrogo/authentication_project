package com.devpedrogo.authentication_module.repository;


import com.devpedrogo.authentication_module.domain.entity.RefreshToken;
import com.devpedrogo.authentication_module.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUserAndRevokedFalse(User user);

    @Modifying
    int deleteByUser(User user);
}