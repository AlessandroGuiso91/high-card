package it.sara.demo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Mints RSA-signed JWTs for authenticated users. Claims emitted: {@code iss}, {@code aud},
 * {@code sub}, {@code iat}, {@code exp} and a custom {@code roles} array consumed by the
 * resource-server side for authorization decisions.
 */
@Service
public class JwtIssuer {

    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final long expirationMinutes;

    public JwtIssuer(
            JwtEncoder encoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Builds a signed JWT for the given subject and roles. Returns both the token
     * string and the absolute expiry instant so the caller can echo it to the client
     * without re-decoding the token.
     */
    public IssuedToken issue(String subject, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(subject)
                .issuedAt(now)
                .expiresAt(exp)
                .claim("roles", roles)
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedToken(token, exp);
    }

    /** Result of {@link #issue(String, List)}: the signed token and its expiry. */
    public record IssuedToken(String token, Instant expiresAt) {}
}
