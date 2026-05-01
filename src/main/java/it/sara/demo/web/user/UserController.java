package it.sara.demo.web.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.assembler.GetUsersAssembler;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.web.assembler.AddUserAssembler;
import it.sara.demo.web.response.GenericResponse;
import it.sara.demo.web.user.request.AddUserRequest;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.GetUsersResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing users.
 * Handles incoming HTTP requests, enforces payload validation via Bean Validation,
 * and delegates business logic to the service layer.
 */
@Tag(name = "Users", description = "User creation and paginated search")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddUserAssembler addUserAssembler;
    private final GetUsersAssembler getUsersAssembler;

    /**
     * Creates a new user. The request payload is automatically validated by Spring before processing.
     * Requires the {@code ADMIN} role: regular users cannot add new users.
     *
     * @param request the payload containing user details to be added
     * @return a generic success response if the user is correctly processed
     * @throws GenericException if business logic validation fails at the service layer
     */
    @Operation(
            summary = "Create a new user",
            description = "Validates the payload at the boundary and persists the user. Requires ROLE_ADMIN."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.PUT)
    public ResponseEntity<GenericResponse> addUser(@RequestBody @Valid AddUserRequest request) throws GenericException {
        CriteriaAddUser criteria = addUserAssembler.toCriteria(request);
        userService.addUser(criteria);
        return ResponseEntity.ok(GenericResponse.success("User added."));
    }

    /**
     * Retrieves a paginated list of users based on the provided search criteria.
     * Open to any authenticated user ({@code USER} or {@code ADMIN}).
     *
     * @param request the payload containing pagination and filtering criteria
     * @return a response containing the matched users
     */
    @Operation(
            summary = "Search users",
            description = "Returns a paginated list filtered by case-insensitive contains over name or email and sorted by the requested OrderType. Requires ROLE_USER or ROLE_ADMIN."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.POST)
    public ResponseEntity<GetUsersResponse> getUsers(@RequestBody @Valid GetUsersRequest request) {
        CriteriaGetUsers criteria = getUsersAssembler.toCriteria(request);
        GetUsersResult result = userService.getUsers(criteria);
        return ResponseEntity.ok(getUsersAssembler.toResponse(result));
    }
}
