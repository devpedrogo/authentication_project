package com.devpedrogo.authentication_module.repository;

import com.devpedrogo.authentication_module.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    UserDetails findUserDetailsByEmail(String email);
    
    boolean existsByEmail(String email);
}
