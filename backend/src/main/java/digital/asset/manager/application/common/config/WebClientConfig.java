package digital.asset.manager.application.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(
                        ExchangeFilterFunction.ofRequestProcessor(
                                clientRequest -> {
                                    log.debug("--------- 요청 ---------");
                                    log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                                    clientRequest.headers().forEach(
                                            (key, values) -> values.forEach(value -> log.debug("{}: {}", key, value))
                                    );
                                    return Mono.just(clientRequest);
                                }
                        )
                )
                .filter(
                        ExchangeFilterFunction.ofResponseProcessor(
                                clientResponse -> {
                                    log.debug("--------- 응답 ---------");
                                    clientResponse.headers().asHttpHeaders().forEach(
                                            (key, values) -> values.forEach(value -> log.debug("{}: {}", key, value))
                                    );
                                    return Mono.just(clientResponse);
                                }
                        )
                )
                .build();
    }
}
