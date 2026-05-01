package it.sara.demo.service.user.result;

import it.sara.demo.service.result.GenericResult;
import lombok.Getter;
import lombok.Setter;

/**
 * Service-layer result for user creation. Empty payload — success is signalled by
 * the absence of a {@link it.sara.demo.exception.GenericException}.
 */
@Getter
@Setter
public class AddUserResult extends GenericResult {
}
