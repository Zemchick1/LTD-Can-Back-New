package com.LTD.ltdWorksAPI.config;

import com.LTD.ltdWorksAPI.service.HelperFunctionsService;
import com.LTD.ltdWorksAPI.service.JWTService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
        private final JWTService jwtService;

        private final UserDetailsService UserdetailsService;

        private final HelperFunctionsService helperFunctionsService;


        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
                        @NonNull HttpServletResponse response,
                        @NonNull FilterChain filterChain) throws ServletException, IOException {
                if ((request.getRequestURI().startsWith("/v1/auth"))){
                        filterChain.doFilter(request, response);
                        return;
                }
                Cookie[] cookies = request.getCookies();

                String jwt_token = helperFunctionsService.getCookieString(cookies,"jwt_token");

                if (cookies == null || (jwt_token == null)
                        || (helperFunctionsService.getCookieString(cookies,"refresh_token") == null)){
                        helperFunctionsService.fullLogout(response);
                        filterChain.doFilter(request, response);
                        return;
                }

                String userEmail;
                try {
                        userEmail = jwtService.getEmail(jwt_token); // TODO How to clean Jwt in DB?
                }
                catch (ExpiredJwtException ex) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Token is expired");
                        return;
                }
                if ((userEmail != null) && (SecurityContextHolder.getContext().getAuthentication() == null)) {
                        UserDetails userDetails = this.UserdetailsService.loadUserByUsername(userEmail);
                        if (jwtService.isTokenValid(userDetails, jwt_token)) {
                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                                authToken.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request)
                                );
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                        else {
                                helperFunctionsService.fullLogout(response);
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write("Token is not valid");
                        }
                }
                filterChain.doFilter(request, response);
        }
}