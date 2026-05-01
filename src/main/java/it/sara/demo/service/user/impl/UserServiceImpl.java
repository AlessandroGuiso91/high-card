package it.sara.demo.service.user.impl;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.service.assembler.UserAssembler;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Core business logic implementation for User operations.
 * Acts as the middle layer between the web tier (validation already performed)
 * and the database tier.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserAssembler userAssembler;

    /**
     * Maps the validated criteria to a database entity and attempts to persist it.
     * @param criteria the validated data for the new user
     * @return an empty AddUserResult if successful
     * @throws GenericException if the persistence layer fails or an unexpected error occurs
     */
    @Override
    public AddUserResult addUser(CriteriaAddUser criteria) throws GenericException {

        AddUserResult returnValue;
        User user;

        try {

            returnValue = new AddUserResult();

            user = new User();
            user.setFirstName(criteria.getFirstName());
            user.setLastName(criteria.getLastName());
            user.setEmail(criteria.getEmail());
            user.setPhoneNumber(criteria.getPhoneNumber());

            if (!userRepository.save(user)) {
                throw new GenericException(500, "Error saving user");
            }
        } catch (GenericException ge) {
            throw ge;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw new GenericException(GenericException.GENERIC_ERROR);
        }
        return returnValue;
    }

    /**
     * Searches users by free-text query, then sorts and paginates the matches.
     * The returned {@code total} reflects the filtered set <em>before</em> pagination,
     * so the client can compute how many pages exist.
     */
    @Override
    public GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) {
        List<User> filtered = userRepository.findMatching(criteriaGetUsers.getQuery());

        List<UserDTO> page = filtered.stream()
                .sorted(comparatorFor(criteriaGetUsers.getOrder()))
                .skip(criteriaGetUsers.getOffset())
                .limit(criteriaGetUsers.getLimit())
                .map(userAssembler::toDTO)
                .toList();

        GetUsersResult result = new GetUsersResult();
        result.setUsers(page);
        result.setTotal(filtered.size());
        result.setOffset(criteriaGetUsers.getOffset());
        result.setLimit(criteriaGetUsers.getLimit());
        return result;
    }

    /**
     * Builds a case-insensitive {@link Comparator} for {@link User} from the requested
     * {@link OrderType}, defaulting to {@code BY_LASTNAME} when {@code order} is {@code null}.
     */
    private static Comparator<User> comparatorFor(OrderType order) {
        OrderType effective = (order != null) ? order : OrderType.BY_LASTNAME;

        return switch (effective) {
            case BY_FIRSTNAME      -> Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER);
            case BY_FIRSTNAME_DESC -> Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER).reversed();
            case BY_LASTNAME       -> Comparator.comparing(User::getLastName,  String.CASE_INSENSITIVE_ORDER);
            case BY_LASTNAME_DESC  -> Comparator.comparing(User::getLastName,  String.CASE_INSENSITIVE_ORDER).reversed();
        };
    }
}
