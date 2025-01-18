package digital.asset.manager.application.service;

import digital.asset.manager.application.dto.PriceAlertRequest;
import digital.asset.manager.application.alert.PriceAlertScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceAlertServiceImpl implements PriceAlertService {

    private final RedisTemplate<String, PriceAlertRequest> redisTemplate;
    private final PriceAlertScheduler scheduler;

    @Override
    public void addAlert(PriceAlertRequest request) {
        redisTemplate.opsForValue().set(request.getName(), request);
        scheduler.scheduleAlert(request);
    }

    @Override
    public void removeAlert(String name) {
        redisTemplate.delete(name);
        scheduler.cancelAlert(name);
    }
}
