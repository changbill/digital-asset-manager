package digital.asset.manager.application.chart.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PriceService {

    private final RedisTemplate<String, String> priceRedisTemplate;

    @Autowired
    public PriceService(RedisTemplate<String, String> priceRedisTemplate) {
        this.priceRedisTemplate = priceRedisTemplate;
    }

    public String getPrice() {
        // Redis에서 최신 시세 가져오기
        return priceRedisTemplate.opsForValue().get("BTCUSDT_LATEST_PRICE");
    }
}
