package it.sara.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import it.sara.demo.dto.StatusDTO;
import it.sara.demo.web.response.GenericResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Spring Security configuration for the JWT resource-server side. Responsible for
 * building the {@link JwtDecoder} that validates incoming bearer tokens against
 * signature (RSA public key), expiration, issuer and audience.
 *
 * <p>The {@code SecurityFilterChain} (which routes are public vs. authenticated)
 * lives in a separate step; this class keeps the cryptographic and validation
 * concerns isolated.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.audience}")
    private String audience;

    @Value("${app.jwt.public-key-location}")
    private Resource publicKeyResource;

    @Value("${app.jwt.private-key-location}")
    private Resource privateKeyResource;

    /**
     * Defines public vs. authenticated routes and wires JWT-based authentication.
     * CSRF is disabled because the API is stateless and authenticated via bearer
     * tokens, not browser cookies. Sessions are stateless: every request must
     * carry its own credentials. Authentication and authorization failures are
     * routed through custom handlers that preserve the project's status-in-body
     * convention (HTTP 200 always).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
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
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(deniedHandler)
                )
                .build();
    }

    /**
     * Converts the custom {@code roles} claim emitted by {@link JwtIssuer} into Spring
     * Security {@link org.springframework.security.core.GrantedAuthority} values, so
     * {@code @PreAuthorize("hasRole('ADMIN')")} can be evaluated against the JWT.
     * The empty prefix is required because the claim already contains the {@code ROLE_}
     * prefix (Spring's default would prepend {@code SCOPE_} otherwise).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * Writes a status-in-body envelope to the HTTP response with a fixed 200 status
     * code, mirroring the contract of {@code GlobalExceptionHandler} for security
     * failures (which bypass {@code @RestControllerAdvice}).
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

    /**
     * Decoder used by Spring Security to verify and parse incoming JWTs. Combines
     * the framework's default validators (signature, expiration, issuer) with a
     * custom {@link JwtAudienceValidator}.
     */
    @Bean
    public JwtDecoder jwtDecoder() throws Exception {
        RSAPublicKey publicKey = loadRsaPublicKey(publicKeyResource);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtAudienceValidator(audience)
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }

    /**
     * Encoder used to mint signed JWTs from the {@code JwtIssuer}. Backed by the
     * RSA keypair loaded from the project's PEM files; the private key never leaves
     * this bean's scope.
     */
    @Bean
    public JwtEncoder jwtEncoder() throws Exception {
        RSAPublicKey publicKey = loadRsaPublicKey(publicKeyResource);
        RSAPrivateKey privateKey = loadRsaPrivateKey(privateKeyResource);
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwks);
    }

    /**
     * BCrypt with default strength (10). Used to hash passwords stored by the
     * {@link InMemoryUserDetailsManager} and to verify them during login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Demo user store: two users with different roles, hashed via BCrypt at startup.
     * Replace with a real {@link UserDetailsService} backed by the database for production.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails alice = User.builder()
                .username("alice")
                .password(encoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build();
        UserDetails bob = User.builder()
                .username("bob")
                .password(encoder.encode("user123"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(alice, bob);
    }

    /**
     * Authentication manager exposed so the {@code AuthController} can authenticate login requests.
     * Spring Boot automatically configures it using the existing UserDetailsService and PasswordEncoder beans.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Reads a PEM-encoded X.509 SubjectPublicKeyInfo file and returns the parsed
     * {@link RSAPublicKey}. Header, footer and whitespace are stripped before Base64 decoding.
     */
    private static RSAPublicKey loadRsaPublicKey(Resource resource) throws Exception {
        String pem = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        String body = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(body);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    /**
     * Reads a PEM-encoded PKCS#8 private key file and returns the parsed
     * {@link RSAPrivateKey}.
     */
    private static RSAPrivateKey loadRsaPrivateKey(Resource resource) throws Exception {
        String pem = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        String body = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(body);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }
}
