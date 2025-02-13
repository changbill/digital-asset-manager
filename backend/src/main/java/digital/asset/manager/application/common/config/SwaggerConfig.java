package digital.asset.manager.application.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final String API_VERSION = "1.0.0";
    private static final String API_TITLE = "가상자산 매니저 API";
    private static final String API_DESCRIPTION = "가상자산 매니저 API 목록입니다.";


    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .version(API_VERSION)
                .title(API_TITLE)
                .description(API_DESCRIPTION);

        Components components = new Components()
                .addSecuritySchemes("accessToken",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));

        return new OpenAPI()
                .components(components)
                .info(info);
    }
}
