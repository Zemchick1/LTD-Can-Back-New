package com.LTD.ltdWorksAPI.repository;

import com.LTD.ltdWorksAPI.model.entity.ConfirmationToken;
import com.LTD.ltdWorksAPI.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Integer> {
    Optional<ConfirmationToken> findByUser(User user);
    Optional<ConfirmationToken> findByToken(String token);
}
