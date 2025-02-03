package digital.asset.manager.application.chart.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// 캐시 미스가 발생하면 외부 REST API를 호출해 데이터를 가져오고 Redis에 저장
@Service
@Slf4j
public class ExternalPriceService {

    private static final String BINANCE_REST_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public String fetchPriceFromApi() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // 외부 API 호출
            Map response = restTemplate.getForObject(BINANCE_REST_URL, Map.class);
            if(response == null) {
                throw new NullPointerException("바이낸스 응답 없음");
            }

            String price = (String) response.get("price");
            // Redis에 저장
            redisTemplate.opsForValue().set("BTCUSDT_LATEST_PRICE", price);

            return price;
        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage(), e);
            return null;
        }
    }
}