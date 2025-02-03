package digital.asset.manager.application.chart.alert;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.nio.charset.StandardCharsets.*;

@Component
@Slf4j
public class DiscordNotifier {

    private static final String WEBHOOK_URL = System.getenv("WEBHOOK_URL");

    public void sendNotification(String message) {
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            JSONObject jo = new JSONObject();
            jo.put("content", message);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jo.toString().getBytes(UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("Discord 알림 전송 성공");
            } else {
                System.err.println("Discord 알림 전송 실패. 응답 코드: " + responseCode);
            }
        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage(), e);
        }
    }
}
