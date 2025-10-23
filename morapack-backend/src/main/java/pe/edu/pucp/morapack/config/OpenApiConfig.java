package pe.edu.pucp.morapack.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI morapackOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Morapack API")
                        .description("API para pedidos y simulación de rutas")
                        .version("v0.0.1"))
                .externalDocs(new ExternalDocumentation().description("Project README").url("../README.md"));
    }
}
