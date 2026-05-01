package it.sara.demo.web.assembler;

import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.web.user.request.AddUserRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests on {@link AddUserAssembler#toCriteria(AddUserRequest)}. The
 * original implementation copied {@code firstName} into the criteria's
 * {@code lastName} field — the fix is locked in by an explicit assertion that
 * the two values stay distinct after mapping.
 */
class AddUserAssemblerTest {

    private final AddUserAssembler assembler = new AddUserAssembler();

    @Test
    void toCriteria_mapsLastNameFromRequestLastName() {
        AddUserRequest request = new AddUserRequest();
        request.setFirstName("Mario");
        request.setLastName("Rossi");
        request.setEmail("mario@example.com");
        request.setPhoneNumber("3331234567");

        CriteriaAddUser criteria = assembler.toCriteria(request);

        assertThat(criteria.getFirstName()).isEqualTo("Mario");
        assertThat(criteria.getLastName()).isEqualTo("Rossi");
        assertThat(criteria.getLastName()).isNotEqualTo(criteria.getFirstName());
    }

    @Test
    void toCriteria_mapsAllFields() {
        AddUserRequest request = new AddUserRequest();
        request.setFirstName("Anna-Maria");
        request.setLastName("Bianchi");
        request.setEmail("anna@example.com");
        request.setPhoneNumber("+39 3339876543");

        CriteriaAddUser criteria = assembler.toCriteria(request);

        assertThat(criteria.getEmail()).isEqualTo("anna@example.com");
        assertThat(criteria.getPhoneNumber()).isEqualTo("+39 3339876543");
    }
}
