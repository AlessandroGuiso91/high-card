package it.sara.demo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Layer-neutral projection of a user, carried inside service results and web responses.
 */
@Getter
@Setter
public class UserDTO {
    private String guid;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
