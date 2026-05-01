package it.sara.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.sara.demo.dto.StatusDTO;
import it.sara.demo.web.response.GenericResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP security configuration: defines public vs. authenticated routes, wires
 * JWT-based authentication through the bean from {@link JwtConfig}, and enforces
 * the project's status-in-body convention even on security failures (which bypass
 * {@code @RestControllerAdvice}). JWT cryptography lives in {@link JwtConfig};
 * identity in {@link UsersConfig}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Builds the application's single security filter chain. CSRF is disabled
     * because the API is stateless and authenticated via bearer tokens; sessions
     * are stateless. Authentication and authorization failures route through
     * custom handlers that always return HTTP 200 with a {@link StatusDTO} body.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper mapper,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        AuthenticationEntryPoint authEntryPoint = (req, res, ex) -> writeStatus(res, mapper, 401, ex.getMessage());
        AccessDeniedHandler deniedHandler = (req, res, ex) -> writeStatus(res, mapper, 403, ex.getMessage());

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(deniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(deniedHandler)
                )
                .build();
    }

    /**
     * Writes a status-in-body envelope to the HTTP response with a fixed 200 status,
     * mirroring the contract of {@code GlobalExceptionHandler} for security failures
     * that bypass {@code @RestControllerAdvice}.
     */
    private static void writeStatus(HttpServletResponse response, ObjectMapper mapper, int code, String message) throws IOException {
        StatusDTO status = new StatusDTO();
        status.setCode(code);
        status.setMessage(message);
        status.setTraceId(UUID.randomUUID().toString());
        GenericResponse body = new GenericResponse();
        body.setStatus(status);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
