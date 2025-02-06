package digital.asset.manager.application;

import digital.asset.manager.application.common.config.properties.AppProperties;
import digital.asset.manager.application.common.config.properties.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties({
		CorsProperties.class,
		AppProperties.class
})
public class DigitalAssetManagerApplication {
	public static void main(String[] args) {
		SpringApplication.run(DigitalAssetManagerApplication.class, args);
	}
}