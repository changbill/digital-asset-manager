package digital.asset.manager.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PriceController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ExternalPriceService externalPriceService;

    @GetMapping("/price")
    public String getCurrentPrice() {
        // Redis에서 최신 시세 가져오기
        String price = redisTemplate.opsForValue().get("BTCUSDT_LATEST_PRICE");

        if(price == null) {
            price = externalPriceService.fetchPriceFromApi();
            if(price == null) {
                return "현재 가격 데이터를 가져올 수 없습니다. 잠시 후 다시 시도하세요.";
            }
        }
        return "현재 BTC 가격: " + price;
    }
}
