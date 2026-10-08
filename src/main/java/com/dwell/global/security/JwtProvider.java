package com.dwell.global.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE = "ACCESS_TOKEN";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {

        // Build the SecretKey used to sign JWTs
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Access token expiration time
        this.accessTokenExpiration = accessTokenExpiration;
    }


    public String createAccessToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + accessTokenExpiration);

        // Collect the user's authorities
        List<String> roles =
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        // Build the access token
        return Jwts.builder()
                .subject(String.valueOf(userDetails.getUser().getId())) // Token owner identifier
                .claim("type", TOKEN_TYPE) // Token type
                .claim("email", userDetails.getUsername()) // User email
                .claim("roles", roles) // User roles
                .issuedAt(now) // Issued at
                .expiration(expiredAt) // Expiration
                .signWith(secretKey) // Signature
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            // Verify the token signature, expiration, and type
            Claims claims = extractClaims(token);
            String type = claims.get("type", String.class);

            return TOKEN_TYPE.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        // Extract userId from the token
        return Long.valueOf(extractClaims(token).getSubject());
    }

    private Claims extractClaims(String token) {
        // Parse the token and extract the payload
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
