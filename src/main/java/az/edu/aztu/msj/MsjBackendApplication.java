package az.edu.aztu.msj;

import az.edu.aztu.msj.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
public class MsjBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsjBackendApplication.class, args);
    }
}
