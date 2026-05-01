package it.sara.demo.service.user.result;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.result.GenericPagedResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Service-layer result for user search: paginated user list plus the inherited
 * {@code total/offset/limit} metadata.
 */
@Getter
@Setter
public class GetUsersResult extends GenericPagedResult {
    private List<UserDTO> users;
}
