package com.dwell.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the access token from the request header
        String token = resolveToken(request);


        try {
            // If the token exists and is valid
            if (token != null && jwtProvider.validateToken(token)) {

                // Extract userId from the token
                Long userId = jwtProvider.getUserId(token);

                // Load the user by userId
                CustomUserDetails userDetails = customUserDetailsService.loadUserById(userId);

                // Build the Authentication object for Spring Security
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                // Store the authentication for the current request in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Clear the authentication if the token is invalid or the user lookup fails
            SecurityContextHolder.clearContext();
        }


        // Pass the request to the next filter
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // Read the Authorization header
        String authorization = request.getHeader("Authorization");

        // Return null if the Authorization header is missing or not a Bearer token
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        // Strip the "Bearer " prefix and return the raw token
        return authorization.substring(7);
    }
}
