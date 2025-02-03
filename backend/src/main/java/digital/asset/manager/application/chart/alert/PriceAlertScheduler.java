package digital.asset.manager.application.chart.alert;

import digital.asset.manager.application.chart.dto.PriceAlertRequest;
import digital.asset.manager.application.chart.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PriceAlertScheduler {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final PriceService priceService;
    private final DiscordNotifier notifier;

    public void scheduleAlert(PriceAlertRequest request) {
        Runnable task = () -> {
            double currentPrice = fetchCurrentPrice(); // 실제 API 호출로 변경 가능
            if (currentPrice > request.getHighThreshold()) {
                notifier.sendNotification("🚨 " + request.getName() + ": 가격이 상한선 초과 (" + currentPrice + ")");
                cancelAlert(request.getName());
            } else if (currentPrice < request.getLowThreshold()) {
                notifier.sendNotification("🚨 " + request.getName() + ": 가격이 하한선 미만 (" + currentPrice + ")");
                cancelAlert(request.getName());
            }
        };

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
        scheduledTasks.put(request.getName(), future);
    }

    public void cancelAlert(String name) {
        ScheduledFuture<?> future = scheduledTasks.get(name);
        if (future != null) {
            future.cancel(true);
        }
        scheduledTasks.remove(name);
    }

    private double fetchCurrentPrice() {
        // 가격 가져오는 로직 (예: API 호출)
        return Double.parseDouble(priceService.getPrice());
    }

}
