package it.sara.demo.service.database.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Persistence-layer entity stored in {@code FakeDatabase}. Never crosses into
 * the web layer — assemblers project it into {@link it.sara.demo.dto.UserDTO}.
 */
@Getter
@Setter
public class User {
    private String guid;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
