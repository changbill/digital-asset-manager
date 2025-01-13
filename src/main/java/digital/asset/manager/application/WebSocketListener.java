package digital.asset.manager.application;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
@Slf4j
public class WebSocketListener {
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@trade";
    private WebSocket webSocket;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostConstruct      // Spring이 빈 초기화를 완료한 후 자동으로 호출
    public void startWebSocket() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(BINANCE_WS_URL), new WebSocketListenerImpl())
                    .join();
        }
    }

    private class WebSocketListenerImpl implements WebSocket.Listener {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final AtomicLong lastProcessedTime = new AtomicLong();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
//            ZonedDateTime now = ZonedDateTime.now();
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
//            String formattedTime = now.format(formatter);

            long currentTime = System.currentTimeMillis();
            if(currentTime - lastProcessedTime.get() >= 1000) {  // 1초 경과 체크
                try {
                    Map message = objectMapper.readValue(data.toString(), Map.class);
                    String price = (String) message.get("p");

                    // Redis에 데이터 저장
                    redisTemplate.opsForValue().set("BTCUSDT_LATEST_PRICE", price);

                    System.out.println("현재 가격 저장: " + price);
                    lastProcessedTime.set(currentTime);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            webSocket.request(1);   // 추가 데이터 요청
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("웹소켓 에러: " + error.getMessage());
            webSocket.abort();
        }
    }
}