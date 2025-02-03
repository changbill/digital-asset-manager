package digital.asset.manager.application.global.email.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmailCacheRepository {

    private final RedisTemplate<String, String> emailRedisTemplate;

    public void setEmailCode(String email, String code, long expirationMillis) {
        String key = getKey(email);
        log.debug("Set User to Redis {}, {}", key, code);
        emailRedisTemplate.opsForValue().set(key, code, Duration.ofMillis(expirationMillis));
    }

    public Optional<String> getEmailCode(String email) {
        String key = getKey(email);
        String code = emailRedisTemplate.opsForValue().get(key);
        log.debug("Get Code from Redis {}, {}", key, code);
        return Optional.ofNullable(code);
    }

    private String getKey(String email) {
        return "EMAIL:" + email;
    }
}
