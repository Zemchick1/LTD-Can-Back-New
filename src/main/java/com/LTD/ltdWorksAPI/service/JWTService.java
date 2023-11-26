package com.LTD.ltdWorksAPI.service;

import com.LTD.ltdWorksAPI.exception.BadRequestException;
import com.LTD.ltdWorksAPI.model.entity.JwtAccessToken;
import com.LTD.ltdWorksAPI.model.entity.RefreshToken;
import com.LTD.ltdWorksAPI.repository.JwtAccessTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
@Service
public class JWTService {
    @Value("${secret_key_jwt}")
    private String secret_key_jwt;
    @Value("${jwt_token_lifetime}")
    private long jwt_token_lifetime;
    private final JwtAccessTokenRepository jwtAccessTokenRepository;
    private static final Logger log = LoggerFactory.getLogger(JWTService.class);


    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails){
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwt_token_lifetime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(UserDetails userDetails, String token){
        final String username = getEmail(token);
        JwtAccessToken jwt_token = jwtAccessTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("Can't find a JWT Token related to Token in DB");
                    throw new BadRequestException("Token is not valid");
                });
        return userDetails.getUsername().equals(username) && !isExpired(token) && !jwt_token.is_revoked();
    }

    public boolean isExpired(String token) {
        return getExpirationDate(token).before(new Date());
    }

    private Date getExpirationDate(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    public String getEmail(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaims(String token){
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
    }

    private Key getSigningKey(){
        byte[] KeyBytes = Decoders.BASE64.decode(secret_key_jwt); // keys in bytes
        return Keys.hmacShaKeyFor(KeyBytes);
    }

    public void saveJwtToken(RefreshToken refreshToken, String jwtToken){
        JwtAccessToken userToken = JwtAccessToken.builder()
                .token(jwtToken)
                .refresh_token(refreshToken)
                .expiry_date(getExpirationDate(jwtToken))
                .build();
        jwtAccessTokenRepository.save(userToken);
    }
}
