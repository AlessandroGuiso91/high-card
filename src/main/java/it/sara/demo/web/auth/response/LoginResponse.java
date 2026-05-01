package it.sara.demo.web.auth.response;

import it.sara.demo.web.response.GenericResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Successful login response: the project's standard {@code status} envelope plus
 * the issued bearer token and its absolute expiry instant.
 */
@Getter
@Setter
public class LoginResponse extends GenericResponse {
    private String token;
    private Instant expiresAt;
}
