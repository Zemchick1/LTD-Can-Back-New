package com.LTD.ltdWorksAPI.controller;

import com.LTD.ltdWorksAPI.model.dto.*;
import com.LTD.ltdWorksAPI.service.AuthService;
import com.LTD.ltdWorksAPI.service.HelperFunctionsService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/auth/")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;

    private final HelperFunctionsService helperFunctionsService;

    @Value("${cookie_lifetime}")
    private long cookie_lifetime;

    @PostMapping("/register")
    public ResponseEntity<Boolean> register(
            @Valid @RequestBody RegisterDTO requestBody,
            @NotNull HttpServletResponse response
    ) {
        AuthResponseDTO result = authService.register(requestBody);

        ResponseCookie cookieRefreshToken = helperFunctionsService.createCookie
                (result.getRefresh_token(), "refresh_token", cookie_lifetime);
        ResponseCookie cookieAccessToken = helperFunctionsService.createCookie
                (result.getJwt_token(), "jwt_token", cookie_lifetime);

        response.addHeader("Set-Cookie", cookieRefreshToken.toString());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookieAccessToken.toString())
                .body(true);
    }

    @PostMapping("/login")
    public ResponseEntity<Boolean> login(
            @RequestBody LoginDTO requestBody,
            @NotNull HttpServletResponse response
    ) {
        AuthResponseDTO result = authService.login(requestBody);

        ResponseCookie cookieRefreshToken = helperFunctionsService.createCookie
                (result.getRefresh_token(), "refresh_token", cookie_lifetime);
        ResponseCookie cookieAccessToken = helperFunctionsService.createCookie
                (result.getJwt_token(), "jwt_token", cookie_lifetime);

        response.addHeader("Set-Cookie", cookieRefreshToken.toString());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookieAccessToken.toString())
                .body(true);
    }


    @RateLimiter(name = "refresh_token_rate_limiter")
    @PostMapping("/renew_token")
    public ResponseEntity<Boolean> refresh_token(@NotNull HttpServletRequest request,
                                                 @NotNull HttpServletResponse response) {
        AuthResponseDTO result = authService.refresh_token(request, response);

        ResponseCookie cookieRefreshToken = helperFunctionsService.createCookie
                (result.getRefresh_token(), "refresh_token", cookie_lifetime);
        ResponseCookie cookieAccessToken = helperFunctionsService.createCookie
                (result.getJwt_token(), "jwt_token", cookie_lifetime);

        response.addHeader("Set-Cookie", cookieRefreshToken.toString());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookieAccessToken.toString())
                .body(true);
    }

    @PostMapping("/change_password") // TODO change endpoint later
    public ResponseEntity<Boolean> change_password(
            @RequestBody ChangePasswordDTO forgotPasswordDTO,
            HttpServletRequest request, HttpServletResponse response
    ) {
        return authService.change_password(forgotPasswordDTO, request, response);
    }

    @GetMapping("/verification")
    public ResponseEntity<String> verification(
            @RequestParam(value = "token") String requestBody
    ) {
        return authService.verification(requestBody);

    }

    @GetMapping("/forgot_password")
    public ResponseEntity<String> send_email_forgot_password(
            @RequestParam @NotNull String email){
        return authService.send_email_forgot_password(email);
    }

    @PostMapping("/forgot_password/check")
    public ResponseEntity<Boolean> check_forgot_password_token(
            @RequestParam String token
    ){
        return authService.check_forgot_password_token(token);
    }

    @PostMapping("/forgot_password/handle")
    public ResponseEntity<Boolean> handle_forgot_password(
            @RequestBody ForgotPasswordDTO forgotPasswordDTO
    ){
        return authService.handle_forgot_password(forgotPasswordDTO);
    }

}