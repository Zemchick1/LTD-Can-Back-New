package com.LTD.ltdWorksAPI.repository;

import com.LTD.ltdWorksAPI.model.entity.JwtAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JwtAccessTokenRepository extends JpaRepository<JwtAccessToken, Long> {
    // @Query(value = "SELECT t FROM writings.token t JOIN writings.user_cred u ON
    // u.Id = :userId AND NOT t.is_revoked AND NOT t.is_expired", nativeQuery =
    // true)
    // List<JwtAccessToken> findAllValidTokensForUser(@Param("userId") Integer
    // userId);

    Optional<JwtAccessToken> findByToken(String token);
}
