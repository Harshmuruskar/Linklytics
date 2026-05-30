package com.linklytics.security;

import com.linklytics.service.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${jwt.secret:change-me-to-a-long-base64-secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:172800000}")
    private long jwtExpirationMs;

    // Extract token from Authorization header
    public String getJwtFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }

    // Generate a signed JWT for a user
    public String generateToken(UserDetailsImpl userDetails){
        String username = userDetails.getUsername();
        String roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiration = new Date(now + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key())
                .compact();
    }

    public String getUsernameFromToken(String token){
        return Jwts.parser()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    private Key key(){
        // jwtSecret may be base64-encoded or plain text. Try base64 first, fall back to UTF-8 bytes.
        byte[] keyBytes;
        try{
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        }catch(IllegalArgumentException e){
            keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Validates token by parsing; returns true when valid, false otherwise
    public boolean validateToken(String authToken){
        try {
            Jwts.parser()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            // token expired
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
