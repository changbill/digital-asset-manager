package digital.asset.manager.application.global.auth.util;

import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.global.auth.dto.UserPrincipal;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;

import static digital.asset.manager.application.common.exception.ErrorCode.INVALID_TOKEN;

/**
 * AuthToken(JWT 토큰) 생성, 검증, 변환 등을 한 곳에서 관리
 */
@Slf4j
@RequiredArgsConstructor
public class AuthTokenProvider {
    private final String key;
    private static final String AUTHORITIES_KEY = "role";

    // 리프레시 토큰
    public AuthToken createAuthToken(String id, long expiry) {
        return new AuthToken(id, key, expiry);
    }

    // 액세스 토큰
    public AuthToken createAuthToken(String id, String role, long expiry) {
        return new AuthToken(id, role, key, expiry);
    }

    public AuthToken convertAuthToken(String token) {
        return new AuthToken(token, key);
    }

    public Authentication getAuthentication(AuthToken authToken) {
        if (authToken.validate()) {
            Claims claims = authToken.extractClaims();
            Collection<? extends GrantedAuthority> authorities =
                    Arrays.stream(new String[]{
                                    claims.get(AUTHORITIES_KEY).toString()
                            })
                            .map(SimpleGrantedAuthority::new)
                            .toList();
            UserPrincipal principal = UserPrincipal.of(authToken.getUserEmail(), null, authorities);
            return new UsernamePasswordAuthenticationToken(principal, authToken, authorities);
        } else {
            throw new ApplicationException(INVALID_TOKEN);
        }
    }

    public Long getId(String token) {
        return convertAuthToken(token)
                .getId();
    }

    public void validToken(String token) {
        if(!convertAuthToken(token).validate()) {
            throw new ApplicationException(INVALID_TOKEN);
        }
    }

    public long getExpiryFromToken(AuthToken authToken) {
        Claims claims = authToken.extractClaims(); // 토큰에서 Claims 추출
        return claims.getExpiration().getTime(); // 만료 시간 (밀리초)
    }
}
