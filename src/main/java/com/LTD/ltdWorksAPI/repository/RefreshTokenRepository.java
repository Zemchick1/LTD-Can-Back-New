package com.LTD.ltdWorksAPI.repository;

import com.LTD.ltdWorksAPI.model.entity.RefreshToken;
import com.LTD.ltdWorksAPI.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String token);

    Optional<List<RefreshToken>> findAllByUser(User user);
}
