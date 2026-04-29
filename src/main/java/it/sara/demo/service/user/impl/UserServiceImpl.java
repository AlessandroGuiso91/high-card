package it.sara.demo.service.user.impl;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * Maps the validated criteria to a database entity and attempts to persist it.
     * * @param criteria the validated data for the new user
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
     * @param criteriaGetUsers
     * @return
     * @throws GenericException
     */
    @Override
    public GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) throws GenericException {
        return null;
    }
}
