package com.LTD.ltdWorksAPI.repository;

import com.LTD.ltdWorksAPI.model.entity.ForgotPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForgotPasswordTokenRepository extends JpaRepository<ForgotPasswordToken, Integer> {
    Optional<ForgotPasswordToken> findByToken(String token);
}
