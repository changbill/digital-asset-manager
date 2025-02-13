package digital.asset.manager.application.global.oauth.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Optional;

public class HeaderUtils {

    private final static String TOKEN_PREFIX = "Bearer ";

    public static Optional<String> getAccessToken(HttpServletRequest request) {
        String headerValue = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (headerValue == null) {
            return null;
        }
        if (headerValue.startsWith(TOKEN_PREFIX)) {
            return Optional.of(headerValue.substring(TOKEN_PREFIX.length()));
        }
        return null;
    }
}
