package com.hmws.global.authentication.repository;

import com.hmws.global.authentication.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUsername(String username);

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByUsername(String userId);
}
