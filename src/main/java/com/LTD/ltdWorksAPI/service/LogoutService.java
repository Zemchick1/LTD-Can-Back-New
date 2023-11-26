package com.LTD.ltdWorksAPI.service;

import com.LTD.ltdWorksAPI.exception.BadRequestException;
import com.LTD.ltdWorksAPI.model.entity.JwtAccessToken;
import com.LTD.ltdWorksAPI.model.entity.RefreshToken;
import com.LTD.ltdWorksAPI.repository.JwtAccessTokenRepository;
import com.LTD.ltdWorksAPI.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {
    private final JwtAccessTokenRepository jwtAccessTokenRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final HelperFunctionsService helperFunctionsService;

    Logger log = LoggerFactory.getLogger(LogoutService.class);

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null){
            log.error("No cookies");
            throw new BadRequestException("Bad Request");
        }
        String jwt_token = helperFunctionsService.getCookieString(cookies, "jwt_token");
        String refresh_token = helperFunctionsService.getCookieString(cookies, "refresh_token");
        if ((jwt_token == null) || (refresh_token == null)){
            log.error("No valid Tokens in cookies");
            throw new BadRequestException("Bad Request");
        }
        JwtAccessToken token = jwtAccessTokenRepository.findByToken(jwt_token)
                .orElseThrow(() -> new BadRequestException("Invalid access token"));
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refresh_token)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        token.set_revoked(true);
        refreshToken.set_revoked(true);
        jwtAccessTokenRepository.save(token);
        refreshTokenRepository.save(refreshToken);
        helperFunctionsService.fullLogout(response);
    }
}
