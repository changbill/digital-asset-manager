package digital.asset.manager.application.global.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class AuthToken {

    @Getter
    private final String token;
    private final Key key;

    private static final String AUTHORITIES_KEY = "role";

    AuthToken(final String token, final String key) {
        this.token = token;
        this.key = getKey(key);
    }

    AuthToken(String id, String key, long expiry) {
        this.token = generateToken(id, expiry);
        this.key = getKey(key);
    }

    AuthToken(String id ,String role, String key, long expiry) {
        this.token = generateToken(id, role, expiry);
        this.key = getKey(key);
    }


    public String getUserEmail() {
        return Objects.requireNonNull(extractClaims()).get("email", String.class);
    }

    public boolean isExpired() {
        Date expiredDate = Objects.requireNonNull(extractClaims()).getExpiration();
        return expiredDate.before(new Date());
    }

    public boolean validate() {
        return extractClaims() != null;
    }

    public Claims extractClaims() {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SecurityException e) {
            log.info("Invalid JWT signature.");
        } catch (MalformedJwtException e) {
            log.info("Invalid JWT token.");
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token.");
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
        }
        return null;
    }

    public String generateToken(String id, long exp) {
        Date now = new Date();

        return Jwts.builder()
                .setSubject(id)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String id, String role, long exp) {
        Claims claims = Jwts.claims();
        claims.put("email", id);
        claims.put(AUTHORITIES_KEY, role);
        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getExpiredClaims() {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token");
            return e.getClaims();
        }
        return null;
    }

    public Long getId() {
        return extractClaims()
                .get("id", Long.class);
    }

    private Key getKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
