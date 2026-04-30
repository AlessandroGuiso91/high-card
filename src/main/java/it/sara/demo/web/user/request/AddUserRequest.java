package it.sara.demo.web.user.request;

import it.sara.demo.web.request.GenericRequest;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for {@code PUT /user/v1/user}. Constraints are enforced at the controller
 * boundary via {@code @Valid} as input-validation defense-in-depth — independent of
 * the current in-memory persistence.
 */
@Getter
@Setter
public class AddUserRequest extends GenericRequest {

    public static final String ITALIAN_PHONE_REGEX = "^(\\+39|0039|39)?3\\d{8,9}$";

    public static final String NAME_REGEX = "^[a-zA-ZÀ-ÖØ-öø-ÿ\\s'\\-]+$";

    @NotBlank(message = "First Name is mandatory")
    @Pattern(regexp = NAME_REGEX, message = "First name must contain only letters, spaces, apostrophes and hyphens")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last Name is mandatory")
    @Pattern(regexp = NAME_REGEX, message = "Last name must contain only letters, spaces, apostrophes and hyphens")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    /**
     * Accepts standard Italian phone formats.
     * Optional prefixes: +39, 0039, 39.
     * Must be followed by the mobile prefix (3) and 8 or 9 digits.
     */
    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = ITALIAN_PHONE_REGEX, message = "Phone number must be a valid Italian mobile number")
    private String phoneNumber;
}
