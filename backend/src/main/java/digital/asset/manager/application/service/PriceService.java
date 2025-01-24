package digital.asset.manager.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PriceService {

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public PriceService(@Qualifier("stringRedisTemplate") StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getPrice() {
        // Redis에서 최신 시세 가져오기
        return redisTemplate.opsForValue().get("BTCUSDT_LATEST_PRICE");
    }
}
