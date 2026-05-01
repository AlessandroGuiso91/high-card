package it.sara.demo.web.auth.request;

import it.sara.demo.web.request.GenericRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for {@code POST /auth/login}. Both fields are required; bounds are
 * enforced at the controller boundary via {@code @Valid}.
 */
@Getter
@Setter
public class LoginRequest extends GenericRequest {

    @NotBlank(message = "Username is mandatory")
    @Size(max = 50, message = "Username must be at most 50 characters")
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Size(max = 100, message = "Password must be at most 100 characters")
    private String password;
}
