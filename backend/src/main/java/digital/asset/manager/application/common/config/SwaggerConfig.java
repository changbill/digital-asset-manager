package digital.asset.manager.application.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .version("v1.0.0")
                .title("가상자산 매니저 API")
                .description("가상자산 매니저 API 목록입니다.");

        return new OpenAPI()
                .info(info);
    }
}
