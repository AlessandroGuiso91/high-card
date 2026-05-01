package it.sara.demo.service.user.impl;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.service.assembler.UserAssembler;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.service.user.result.GetUsersResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserServiceImpl}. The repository and assembler are mocked
 * so that filter / sort / paginate logic and error mapping can be exercised in
 * isolation from the in-memory store and the entity-to-DTO conversion.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAssembler userAssembler;

    @InjectMocks
    private UserServiceImpl service;

    private User mario;
    private User anna;
    private User luca;

    @BeforeEach
    void setUp() {
        mario = user("Mario", "Rossi", "mario@example.com");
        anna = user("Anna", "Bianchi", "anna@example.com");
        luca = user("Luca", "Verdi", "luca@example.com");
    }

    /**
     * Configures the assembler mock to project a {@link User} into a {@link UserDTO}
     * one-to-one. Called only by tests that exercise the read path so that Mockito's
     * strict-stubbing detector does not flag it as unused on the write-path tests.
     */
    private void stubAssembler() {
        when(userAssembler.toDTO(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            UserDTO dto = new UserDTO();
            dto.setFirstName(u.getFirstName());
            dto.setLastName(u.getLastName());
            dto.setEmail(u.getEmail());
            return dto;
        });
    }

    @Test
    void addUser_persistsAndReturnsResult() throws GenericException {
        CriteriaAddUser criteria = new CriteriaAddUser();
        criteria.setFirstName("Mario");
        criteria.setLastName("Rossi");
        criteria.setEmail("mario@example.com");
        criteria.setPhoneNumber("3331234567");
        when(userRepository.save(any(User.class))).thenReturn(true);

        assertThat(service.addUser(criteria)).isNotNull();
    }

    @Test
    void addUser_throwsWhenRepositoryFails() {
        CriteriaAddUser criteria = new CriteriaAddUser();
        criteria.setFirstName("Mario");
        criteria.setLastName("Rossi");
        criteria.setEmail("mario@example.com");
        criteria.setPhoneNumber("3331234567");
        when(userRepository.save(any(User.class))).thenReturn(false);

        assertThatThrownBy(() -> service.addUser(criteria))
                .isInstanceOf(GenericException.class)
                .extracting("status.code").isEqualTo(500);
    }

    @Test
    void getUsers_sortsByLastNameAscending() {
        stubAssembler();
        when(userRepository.findMatching(null)).thenReturn(List.of(luca, mario, anna));
        CriteriaGetUsers criteria = criteria(null, 0, 10, OrderType.BY_LASTNAME);

        GetUsersResult result = service.getUsers(criteria);

        assertThat(result.getUsers()).extracting(UserDTO::getLastName)
                .containsExactly("Bianchi", "Rossi", "Verdi");
        assertThat(result.getTotal()).isEqualTo(3);
    }

    @Test
    void getUsers_sortsByLastNameDescending() {
        stubAssembler();
        when(userRepository.findMatching(null)).thenReturn(List.of(luca, mario, anna));
        CriteriaGetUsers criteria = criteria(null, 0, 10, OrderType.BY_LASTNAME_DESC);

        GetUsersResult result = service.getUsers(criteria);

        assertThat(result.getUsers()).extracting(UserDTO::getLastName)
                .containsExactly("Verdi", "Rossi", "Bianchi");
    }

    @Test
    void getUsers_defaultsOrderToByLastNameWhenNull() {
        stubAssembler();
        when(userRepository.findMatching(null)).thenReturn(List.of(luca, mario, anna));
        CriteriaGetUsers criteria = criteria(null, 0, 10, null);

        GetUsersResult result = service.getUsers(criteria);

        assertThat(result.getUsers()).extracting(UserDTO::getLastName)
                .containsExactly("Bianchi", "Rossi", "Verdi");
    }

    @Test
    void getUsers_paginatesAfterSorting() {
        stubAssembler();
        when(userRepository.findMatching(null)).thenReturn(List.of(luca, mario, anna));
        CriteriaGetUsers criteria = criteria(null, 1, 1, OrderType.BY_LASTNAME);

        GetUsersResult result = service.getUsers(criteria);

        assertThat(result.getUsers()).extracting(UserDTO::getLastName).containsExactly("Rossi");
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getLimit()).isEqualTo(1);
    }

    @Test
    void getUsers_offsetBeyondTotalReturnsEmptyButKeepsTotal() {
        when(userRepository.findMatching(null)).thenReturn(List.of(luca, mario, anna));
        CriteriaGetUsers criteria = criteria(null, 99, 10, OrderType.BY_LASTNAME);

        GetUsersResult result = service.getUsers(criteria);

        assertThat(result.getUsers()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(3);
    }

    @Test
    void getUsers_totalReflectsFilteredSetNotPage() {
        stubAssembler();
        when(userRepository.findMatching("ross")).thenReturn(List.of(mario));
        CriteriaGetUsers criteria = criteria("ross", 0, 10, OrderType.BY_LASTNAME);

        GetUsersResult result = service.getUsers(criteria);

        assertThat(result.getUsers()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    private static User user(String firstName, String lastName, String email) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        return u;
    }

    private static CriteriaGetUsers criteria(String query, int offset, int limit, OrderType order) {
        CriteriaGetUsers c = new CriteriaGetUsers();
        c.setQuery(query);
        c.setOffset(offset);
        c.setLimit(limit);
        c.setOrder(order);
        return c;
    }
}
