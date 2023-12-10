package com.LTD.ltdWorksAPI.service;

import com.LTD.ltdWorksAPI.exception.BadRequestException;
import com.LTD.ltdWorksAPI.model.dto.*;
import com.LTD.ltdWorksAPI.model.entity.*;
import com.LTD.ltdWorksAPI.repository.*;
import com.LTD.ltdWorksAPI.utils.enums.Roles;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTService jwtService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final AuthenticationManager authenticationManager;

    private final HelperFunctionsService helperFunctionsService;

    private final JwtAccessTokenRepository jwtAccessTokenRepository;

    private final ForgotPasswordTokenRepository forgotPasswordTokenRepository;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthResponseDTO register(@NotNull RegisterDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()
                || userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("User with this email or username is already exists");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Roles.User)
                .build();
        ConfirmationToken confirmationToken = helperFunctionsService.createConfirmationToken(user);
        confirmationTokenRepository.save(confirmationToken);
        helperFunctionsService.sendVerificationEmail(user, confirmationToken);
        RefreshToken refreshToken = helperFunctionsService.createRefreshToken(user);
        String jwtToken = jwtService.generateToken(user);
        jwtService.saveJwtToken(refreshToken, jwtToken);
        refreshTokenRepository.save(refreshToken);
        return AuthResponseDTO.builder()
                .jwt_token(jwtToken)
                .refresh_token(refreshToken.getToken())
                .build();
    }

    public AuthResponseDTO login(@NotNull LoginDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Can't find User with this email");
                    throw new BadRequestException("Bad Request");
                });
        String jwtToken = jwtService.generateToken(user);
        RefreshToken refreshToken = helperFunctionsService.createRefreshToken(user);
        jwtService.saveJwtToken(refreshToken, jwtToken);
        refreshTokenRepository.save(refreshToken);
        return AuthResponseDTO
                .builder()
                .jwt_token(jwtToken)
                .refresh_token(refreshToken.getToken())
                .build();
    }

    public ResponseEntity<String> verification(@NotNull String request) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(request).orElseThrow(
                () -> {
                    log.error("Can't find Confirmation Token");
                    throw new BadRequestException("Bad Request");
                }
        );
        User user = confirmationToken.getUser();
        if (user == null) {
            log.error("Can't find User related to Confirmation Token");
            throw new BadRequestException("Bad Request");
        }
        confirmationTokenRepository.delete(confirmationToken);
        user.setEmail_verified(true);
        userRepository.save(user);
        return ResponseEntity.ok("Successfully Verified");
    }

    public AuthResponseDTO refresh_token(@NotNull HttpServletRequest request,
                                         @NotNull HttpServletResponse response){
        Cookie[] cookies = request.getCookies();
        if (cookies == null){
            log.error("User has no Cookies");
            throw new BadRequestException("Bad Request");
        }
        String refresh_tokenString = helperFunctionsService.getCookieString(cookies,"refresh_token");
        String jwt_tokenString = helperFunctionsService.getCookieString(cookies,"jwt_token");
        if (refresh_tokenString == null || jwt_tokenString == null){
            log.error("User has no Refresh Token Cookies or Jwt Token Cookies");
            throw new BadRequestException("An unexpected Error occurred.");
        }
        RefreshToken old_refresh_token = refreshTokenRepository.findByToken(refresh_tokenString)
                .orElseThrow(() -> {
                    helperFunctionsService.fullLogout(response);
                    log.error("Can't find Refresh Token in DB");
                    throw new BadRequestException("Invalid refresh token");
                });
        JwtAccessToken old_jwt_token = jwtAccessTokenRepository.findByToken(jwt_tokenString)
                .orElseThrow(() -> {
                    helperFunctionsService.fullLogout(response);
                    log.error("Can't find JWT Token in DB");
                    throw new BadRequestException("Invalid access token");
                });

        if (helperFunctionsService.isRefreshTokenValid(old_refresh_token)
                && old_jwt_token.getRefresh_token().equals(old_refresh_token)){
            old_jwt_token.set_revoked(true);
            jwtAccessTokenRepository.save(old_jwt_token);
            String new_refresh_token = UUID.randomUUID().toString();
            old_refresh_token.setToken(new_refresh_token);
            refreshTokenRepository.save(old_refresh_token);
            String new_jwt_token = jwtService.generateToken(old_refresh_token.getUser());
            jwtService.saveJwtToken(old_refresh_token, new_jwt_token);
            return AuthResponseDTO
                    .builder()
                    .refresh_token(new_refresh_token)
                    .jwt_token(new_jwt_token)
                    .build();
        }
        else {
            helperFunctionsService.fullLogout(response);
            throw new BadRequestException("Refresh token is not valid");
        }
    }


    public ResponseEntity<Boolean> change_password(ChangePasswordDTO changePasswordDTO, HttpServletRequest request
            , HttpServletResponse response) {
        if (Objects.equals(changePasswordDTO.getNew_password(), changePasswordDTO.getNew_password_duplicate())){
            String token = helperFunctionsService.getCookieString(request.getCookies(), "jwt_token");
            User user = userRepository.findByEmail(jwtService.getEmail(token)).orElseThrow(
                    () -> {
                        log.error("Can't find User related to JWTToken in DB");
                        throw new BadRequestException("Bad Request");
                    }
            );
            if (passwordEncoder.matches(changePasswordDTO.getOld_password(), user.getPassword())){
                user.setPassword(passwordEncoder.encode(changePasswordDTO.getNew_password()));
                helperFunctionsService.setAllTokensByUserRevoked(user);
                helperFunctionsService.fullLogout(response);
                return ResponseEntity.ok(true);
            }
            else {
                throw new BadRequestException("Old Password is incorrect");
            }
        }
        else {
            throw new BadRequestException("New Passwords don't match");
        }
    }
    public ResponseEntity<String> send_email_forgot_password(String email){
        ForgotPasswordToken token = helperFunctionsService.createForgotPasswordToken(email);
        helperFunctionsService.sendEmailForgotPassword(email, token.getToken());
        forgotPasswordTokenRepository.save(token);
        return ResponseEntity.ok().body("Successfully sent an Email");
    }

    public ResponseEntity<Boolean> check_forgot_password_token(String token){
        forgotPasswordTokenRepository.findByToken(token).orElseThrow(() -> {
            log.error("Can't find Forgot Password Token in DB");
            throw new BadRequestException("Bad Request");
        });
        return ResponseEntity.ok(true);
    }

    public ResponseEntity<Boolean> handle_forgot_password(ForgotPasswordDTO forgotPasswordDTO) {
        ForgotPasswordToken token = forgotPasswordTokenRepository.findByToken(forgotPasswordDTO.getToken()).orElseThrow(
                () -> {
                    log.error("Can't find Forgot Password Token in DB");
                    throw new BadRequestException("Bad Request");
                }
        );
        if (forgotPasswordDTO.getNew_password().equals(forgotPasswordDTO.getNew_password_duplicate())
                && !token.getExpiry_date().before(new Date(System.currentTimeMillis()))){
            User user = userRepository.findByEmail(token.getEmail()).orElseThrow(
                    () -> {
                        log.error("Can't find User related to Forgot Password Token in DB");
                        throw new BadRequestException("Bad Request");
                    }
            );
            user.setPassword(passwordEncoder.encode(forgotPasswordDTO.getNew_password()));
            helperFunctionsService.setAllTokensByUserRevoked(user);
            forgotPasswordTokenRepository.delete(token);
            userRepository.save(user);
            return ResponseEntity.ok(true);
        }
        else {
            throw new BadRequestException("Password don't match");
        }
    }

//     private void revokeAllUserTokens(User user){
//     List<JwtAccessToken> validTokens =
//     tokenRepository.findAllValidTokensForUser(user.getId());
//     if (validTokens.isEmpty())
//     return;
//     validTokens.forEach(Token ->
//     {
//     Token.set_revoked(true);
//     });
//     tokenRepository.saveAll(validTokens);
//     }

}
