package it.sara.demo.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JwtIssuer}: tokens are signed with the private key, contain
 * the configured issuer / audience / expiration / roles, and can be verified by
 * a decoder built on the matching public key.
 */
class JwtIssuerTest {

    private JwtIssuer issuer;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));

        issuer = new JwtIssuer(encoder, "high-card", "high-card-api", 60);
        decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Test
    void issue_emitsTokenWithExpectedClaims() {
        JwtIssuer.IssuedToken issued = issuer.issue("alice", List.of("ROLE_ADMIN", "ROLE_USER"));

        var jwt = decoder.decode(issued.token());
        // The issuer is a free-form string (not a URL), so read it via getClaimAsString
        // rather than getIssuer() which expects a URL.
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("high-card");
        assertThat(jwt.getAudience()).containsExactly("high-card-api");
        assertThat(jwt.getSubject()).isEqualTo("alice");
        assertThat(jwt.<List<String>>getClaim("roles")).containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void issue_setsExpirationInTheFuture() {
        JwtIssuer.IssuedToken issued = issuer.issue("bob", List.of("ROLE_USER"));

        assertThat(issued.expiresAt()).isAfter(Instant.now());
        // JWT exp is encoded in epoch seconds, so the decoded value is truncated.
        // Compare both at second-level granularity.
        assertThat(decoder.decode(issued.token()).getExpiresAt())
                .isEqualTo(issued.expiresAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
