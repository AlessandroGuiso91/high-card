package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserRepository}, focused on the case-insensitive
 * contains-filter applied to first name, last name and email.
 */
class UserRepositoryTest {

    private final UserRepository repository = new UserRepository();

    @BeforeEach
    void resetTable() {
        FakeDatabase.TABLE_USER.clear();
        save("Mario", "Rossi", "mario@example.com");
        save("Anna", "Bianchi", "anna.bianchi@example.com");
        save("Luca", "Verdi", "luca@example.com");
    }

    @Test
    void findMatching_returnsAllWhenQueryIsNull() {
        assertThat(repository.findMatching(null)).hasSize(3);
    }

    @Test
    void findMatching_returnsAllWhenQueryIsBlank() {
        assertThat(repository.findMatching("   ")).hasSize(3);
    }

    @Test
    void findMatching_isCaseInsensitiveOnFirstName() {
        assertThat(repository.findMatching("MARIO"))
                .extracting(User::getFirstName)
                .containsExactly("Mario");
    }

    @Test
    void findMatching_matchesPartialLastName() {
        assertThat(repository.findMatching("ross"))
                .extracting(User::getLastName)
                .containsExactly("Rossi");
    }

    @Test
    void findMatching_matchesEmailSubstring() {
        assertThat(repository.findMatching("example.com"))
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder(
                        "mario@example.com",
                        "anna.bianchi@example.com",
                        "luca@example.com"
                );
    }

    @Test
    void findMatching_returnsEmptyWhenNoMatch() {
        assertThat(repository.findMatching("ghost")).isEmpty();
    }

    @Test
    void save_assignsGuidAndAppendsToTable() {
        User user = new User();
        user.setFirstName("New");
        user.setLastName("User");
        user.setEmail("new@example.com");

        boolean ok = repository.save(user);

        assertThat(ok).isTrue();
        assertThat(user.getGuid()).isNotBlank();
        assertThat(FakeDatabase.TABLE_USER).hasSize(4);
    }

    private static void save(String firstName, String lastName, String email) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setGuid(java.util.UUID.randomUUID().toString());
        FakeDatabase.TABLE_USER.add(u);
    }
}
