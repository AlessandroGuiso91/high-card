package it.sara.demo.web.user.request;

import it.sara.demo.web.request.GenericRequest;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddUserRequest extends GenericRequest {

    public static final String ITALIAN_PHONE_REGEX = "^(\\+39|0039|39)?3\\d{8,9}$";

    @NotBlank(message = "First Name is mandatory")
    private String firstName;

    @NotBlank(message = "Last Name is mandatory")
    private String lastName;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
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
