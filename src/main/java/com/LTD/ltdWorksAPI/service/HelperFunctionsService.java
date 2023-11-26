package com.LTD.ltdWorksAPI.service;

import com.LTD.ltdWorksAPI.exception.BadRequestException;
import com.LTD.ltdWorksAPI.model.entity.*;
import com.LTD.ltdWorksAPI.repository.JwtAccessTokenRepository;
import com.LTD.ltdWorksAPI.repository.RefreshTokenRepository;
import com.LTD.ltdWorksAPI.utils.enums.Roles;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@RequiredArgsConstructor
@Service
public class HelperFunctionsService {
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    @Value("${jwt_token_lifetime}")
    private long jwt_token_lifetime;
    @Value("${forgot_password_token_lifetime}")
    private long forgot_password_token_lifetime;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtAccessTokenRepository jwtAccessTokenRepository;
    private static final Logger log = LoggerFactory.getLogger(HelperFunctionsService.class);

    public RefreshToken createRefreshToken(User user){
        return RefreshToken
                .builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .build();
    }

    public ConfirmationToken createConfirmationToken(User user){
        return ConfirmationToken
                .builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .build();
    }

    public boolean isRefreshTokenValid(RefreshToken token){
        return !token.is_revoked() && token.getExpiry_date().after(new Date(System.currentTimeMillis() + jwt_token_lifetime));
    }

    public ResponseCookie createCookie(String value, String name, long time) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
//                .secure(true)
                .domain("localhost")
                .path("/")
                .maxAge(time)
                .sameSite("Strict")
                .build();
    }

    public String getCookieString(Cookie[] cookies, String name){
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    public void sendVerificationEmail(User user, ConfirmationToken token) {
        String recipient = user.getUsername();
        String subject = "Complete Registration!";
        String content = "To confirm your account, please click here : "
                + "http://localhost:8080/v1/auth/verification?token="
                + token.getToken();
        emailService.sendEmail(recipient, subject, content);
    }

    public void fullLogout(HttpServletResponse response){
        ResponseCookie deleteCookieJwt
                = createCookie("0", "jwt_token", 0);
        ResponseCookie deleteCookieRefreshToken
                = createCookie("0", "refresh_token", 0);

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookieRefreshToken.toString());

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookieJwt.toString());
        SecurityContextHolder.clearContext();
    }

    public void sendEmailForgotPassword(String email, String token) {
        String subject = "Forgot Password";
        String content = "Trouble signing in?\n" +
                "Resetting your password is easy.\n" +
                "\n" +
                "Just press the url below and follow the instructions. We’ll have you up and running in no time.\n" +
                "http://localhost:8080/v1/auth/forgot_password/process?token=" + token +
                "\n\n" +
                "If you did not make this request then please ignore this email.";
        emailService.sendEmail(email, subject, content);
    }

    public ForgotPasswordToken createForgotPasswordToken(String email) {
        return ForgotPasswordToken.builder()
                .token(UUID.randomUUID().toString())
                .email(email)
                .expiry_date(new Date(System.currentTimeMillis() + forgot_password_token_lifetime))
                .build();
    }

    public void setAllTokensByUserRevoked(User user) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(jdbcTemplate.getDataSource()));
        String sql = """
                BEGIN;
                                
                -- Update in the first database
                UPDATE users.jwt_access_token AS jwt
                SET is_revoked = true
                FROM users.refresh_token AS refresh
                         JOIN users.user_cred AS cred ON cred.id = :user.id
                WHERE refresh.user_id = cred.id
                AND jwt.refresh_token_id = refresh.id;
                                
                -- Update in the second database
                UPDATE users.refresh_token
                SET is_revoked = true
                WHERE users.refresh_token.user_id = :user.id;
                                
                                
                COMMIT;
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("user.id", user.getId());
        namedParameterJdbcTemplate.update(sql, parameters); // TODO TESTS
    }

    public Roles getRole(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Roles role = Roles.valueOf(authentication.getAuthorities().stream().findFirst().orElseThrow().getAuthority());
        return role;
    }
}
