package it.sara.demo.service.assembler;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.database.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests on {@link UserAssembler#toDTO(User)}. The original implementation
 * truncated the email to its domain via {@code substring(lastIndexOf("@") + 1)} and
 * never mapped {@code phoneNumber}. Both fixes are locked in here.
 */
class UserAssemblerTest {

    private final UserAssembler assembler = new UserAssembler();

    @Test
    void toDTO_preservesFullEmail() {
        User user = new User();
        user.setGuid("g-1");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");
        user.setPhoneNumber("3331234567");

        UserDTO dto = assembler.toDTO(user);

        assertThat(dto.getEmail()).isEqualTo("mario.rossi@example.com");
    }

    @Test
    void toDTO_mapsPhoneNumber() {
        User user = new User();
        user.setEmail("anyone@example.com");
        user.setPhoneNumber("+39 3331234567");

        assertThat(assembler.toDTO(user).getPhoneNumber()).isEqualTo("+39 3331234567");
    }

    @Test
    void toDTO_mapsAllScalarFields() {
        User user = new User();
        user.setGuid("g-1");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario@example.com");
        user.setPhoneNumber("3331234567");

        UserDTO dto = assembler.toDTO(user);

        assertThat(dto.getGuid()).isEqualTo("g-1");
        assertThat(dto.getFirstName()).isEqualTo("Mario");
        assertThat(dto.getLastName()).isEqualTo("Rossi");
    }
}
