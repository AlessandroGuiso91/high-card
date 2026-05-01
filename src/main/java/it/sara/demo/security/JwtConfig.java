package it.sara.demo.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
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
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT cryptography and validation configuration. Owns the RSA keypair loading
 * and the encoder/decoder/converter beans. Kept separate from
 * {@link SecurityConfig} (which deals with HTTP routing) and {@link UsersConfig}
 * (which deals with identity), so each class has a single responsibility.
 */
@Configuration
public class JwtConfig {

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.audience}")
    private String audience;

    @Value("${app.jwt.public-key-location}")
    private Resource publicKeyResource;

    @Value("${app.jwt.private-key-location}")
    private Resource privateKeyResource;

    /**
     * Decoder used by Spring Security to verify and parse incoming JWTs. Combines
     * the default validators (signature, expiration, issuer) with the custom
     * {@link JwtAudienceValidator}.
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
     * Encoder used by {@link JwtIssuer} to mint signed JWTs. The private key never
     * leaves this bean's scope.
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
     * Converts the custom {@code roles} claim into Spring Security authorities so
     * {@code @PreAuthorize("hasRole('ADMIN')")} can be evaluated against the JWT.
     * The empty prefix is required because the claim already contains {@code ROLE_}.
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
     * Reads a PEM-encoded X.509 SubjectPublicKeyInfo file into an {@link RSAPublicKey}.
     */
    private static RSAPublicKey loadRsaPublicKey(Resource resource) throws Exception {
        String pem = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        String body = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(body);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    /**
     * Reads a PEM-encoded PKCS#8 private key file into an {@link RSAPrivateKey}.
     */
    private static RSAPrivateKey loadRsaPrivateKey(Resource resource) throws Exception {
        String pem = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        String body = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", "");
        byte[] bytes = Base64.getDecoder().decode(body);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }
}
