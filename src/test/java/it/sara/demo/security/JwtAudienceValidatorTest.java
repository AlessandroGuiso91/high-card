package it.sara.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JwtAudienceValidator}: the {@code aud} claim must contain the
 * expected value, otherwise the token is rejected.
 */
class JwtAudienceValidatorTest {

    private final JwtAudienceValidator validator = new JwtAudienceValidator("high-card-api");

    @Test
    void succeedsWhenAudienceMatches() {
        Jwt jwt = jwt(List.of("high-card-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void succeedsWhenAudienceListContainsExpected() {
        Jwt jwt = jwt(List.of("other-service", "high-card-api", "yet-another"));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void failsWhenAudienceDoesNotMatch() {
        Jwt jwt = jwt(List.of("some-other-api"));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void failsWhenAudienceIsEmpty() {
        Jwt jwt = jwt(List.of());

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private static Jwt jwt(List<String> audience) {
        return Jwt.withTokenValue("dummy")
                .header("alg", "RS256")
                .claim("aud", audience)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(c -> c.putAll(Map.of()))
                .build();
    }
}
