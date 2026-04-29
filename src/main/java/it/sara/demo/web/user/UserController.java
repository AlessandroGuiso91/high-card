package it.sara.demo.web.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.web.assembler.AddUserAssembler;
import it.sara.demo.web.response.GenericResponse;
import it.sara.demo.web.user.request.AddUserRequest;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.GetUsersResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing users.
 * Handles incoming HTTP requests, enforces payload validation via Bean Validation,
 * and delegates business logic to the service layer.
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final AddUserAssembler addUserAssembler;

    /**
     * Creates a new user. The request payload is automatically validated by Spring before processing.
     *
     * @param request the payload containing user details to be added
     * @return a generic success response if the user is correctly processed
     * @throws GenericException if business logic validation fails at the service layer
     */
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.PUT)
    public ResponseEntity<GenericResponse> addUser(@RequestBody @Valid AddUserRequest request) throws GenericException {
        CriteriaAddUser criteria = addUserAssembler.toCriteria(request);
        userService.addUser(criteria);
        return ResponseEntity.ok(GenericResponse.success("User added."));
    }

    /**
     * Retrieves a paginated list of users based on the provided search criteria.
     *
     * @param request the payload containing pagination and filtering criteria
     * @return a response containing the matched users
     * @throws GenericException if an error occurs during the retrieval process
     */
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.POST)
    public ResponseEntity<GetUsersResponse> getUsers(@RequestBody GetUsersRequest request) throws GenericException {
        return ResponseEntity.ok().build();
    }
}
