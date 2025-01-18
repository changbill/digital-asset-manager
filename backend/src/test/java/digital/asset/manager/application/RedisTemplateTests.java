package digital.asset.manager.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class RedisTemplateTests {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void redisTemplateString() {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        String key = "name";
        valueOperations.set(key, "giraffe");
        String value = valueOperations.get(key);
        Assertions.assertEquals(value, "giraffe");
    }
}
