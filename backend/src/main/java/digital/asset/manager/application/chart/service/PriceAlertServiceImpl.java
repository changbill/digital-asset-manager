package digital.asset.manager.application.chart.service;

import digital.asset.manager.application.chart.dto.PriceAlertRequest;
import digital.asset.manager.application.chart.alert.PriceAlertScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceAlertServiceImpl implements PriceAlertService {

    private final RedisTemplate<String, PriceAlertRequest> alertRedisTemplate;
    private final PriceAlertScheduler scheduler;

    @Override
    public void addAlert(PriceAlertRequest request) {
        alertRedisTemplate.opsForValue().set(request.getName(), request);
        scheduler.scheduleAlert(request);
    }

    @Override
    public void removeAlert(String name) {
        alertRedisTemplate.delete(name);
        scheduler.cancelAlert(name);
    }
}
