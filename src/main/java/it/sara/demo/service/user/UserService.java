package it.sara.demo.service.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;

public interface UserService {

    AddUserResult addUser(CriteriaAddUser addUserRequest) throws GenericException;

    GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers);
}
