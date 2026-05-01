package it.sara.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration. Declares the project's metadata and the global
 * {@code bearerAuth} security scheme, so every operation in Swagger UI exposes the
 * "Authorize" button and JWTs are sent as {@code Authorization: Bearer ...}.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI highCardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("High-Card API")
                        .description("Spring Boot 3.5 / Java 17 technical assessment for it.sara — user management with JWT authentication.")
                        .version("0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
