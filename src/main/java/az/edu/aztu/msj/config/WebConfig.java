package az.edu.aztu.msj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Serves uploaded CMS assets (the {@code public/} subtree of storage) at /files/**. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path publicDir = Paths.get(props.storage().localPath(), "public").toAbsolutePath().normalize();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(publicDir.toUri().toString());
    }
}
