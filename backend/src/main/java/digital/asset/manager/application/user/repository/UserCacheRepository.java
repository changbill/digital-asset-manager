package digital.asset.manager.application.user.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis를 사용하여 User 정보를 key-value 형태로 저장
 * TTL(30분) 설정으로 캐시 만료 기능 제공
 * Redis를 활용한 빠른 조회 및 캐싱을 제공하는 레포지토리 구현체
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserCacheRepository {

    private final RedisTemplate<String, User> userRedisTemplate;
    private final static Duration USER_CACHE_TTL = Duration.ofMillis(1800000); //30분

    public void setUser(User user) {
        String key = getKey(user.email());
        userRedisTemplate.opsForValue().set(key, user, USER_CACHE_TTL); // 캐시 만료 시간 set
        log.info("set user : {}, {}", key, user);
    }

    public Optional<User> getUser(String email) {
        String key = getKey(email);
        User user = userRedisTemplate.opsForValue().get(key);
        log.info("get User : {}, {}", key, user);
        return Optional.ofNullable(user);
    }

    public void updateUser(User user) {
        String key = getKey(user.email());
        ValueOperations<String, User> valueOps = userRedisTemplate.opsForValue();
        valueOps.set(key, user);
    }

    public void deleteUser(String email) {
        String key = getKey(email);
        userRedisTemplate.delete(key);
    }


    private String getKey(String email) {
        return "USER:" + email;
    }
}
