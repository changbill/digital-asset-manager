package digital.asset.manager.application.common.config.security;

import digital.asset.manager.application.global.auth.util.AuthTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret-key}")
    private String key;

    @Bean
    public AuthTokenProvider jwtProvider() {
        return new AuthTokenProvider(key);
    }
}
