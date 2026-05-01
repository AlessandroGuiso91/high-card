package it.sara.demo.service.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;

/**
 * Application service for user operations. The implementation orchestrates the
 * repository and the assemblers; web concerns (request/response shapes, HTTP
 * status codes) are kept out of this layer.
 */
public interface UserService {

    /**
     * Persists a new user described by the given criteria.
     *
     * @throws GenericException when the persistence layer rejects the write.
     */
    AddUserResult addUser(CriteriaAddUser addUserRequest) throws GenericException;

    /**
     * Returns the paginated, sorted, filtered slice of users matching the criteria.
     */
    GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers);
}
