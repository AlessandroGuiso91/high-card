package it.sara.demo.web.auth;

import it.sara.demo.dto.StatusDTO;
import it.sara.demo.security.JwtIssuer;
import it.sara.demo.web.auth.request.LoginRequest;
import it.sara.demo.web.auth.response.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Exposes the authentication entry point. Validates credentials against the
 * configured {@code AuthenticationManager} and returns a signed JWT on success.
 * Authentication failures bubble up as {@code AuthenticationException} and are
 * translated by {@code GlobalExceptionHandler} into a status-in-body 401 response.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtIssuer jwtIssuer;

    /**
     * Authenticates the supplied credentials and issues a JWT carrying the
     * authenticated subject and its granted roles.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtIssuer.IssuedToken issued = jwtIssuer.issue(auth.getName(), roles);

        StatusDTO status = new StatusDTO();
        status.setCode(200);
        status.setMessage("Login successful");
        status.setTraceId(UUID.randomUUID().toString());

        LoginResponse response = new LoginResponse();
        response.setStatus(status);
        response.setToken(issued.token());
        response.setExpiresAt(issued.expiresAt());
        return ResponseEntity.ok(response);
    }
}
