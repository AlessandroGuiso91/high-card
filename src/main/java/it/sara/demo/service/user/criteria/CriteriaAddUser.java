package it.sara.demo.service.user.criteria;

import it.sara.demo.service.criteria.GenericCriteria;
import lombok.Getter;
import lombok.Setter;

/**
 * Service-layer criteria for user creation. Validation has already been enforced
 * at the web boundary, so the service can treat these fields as well-formed.
 */
@Getter
@Setter
public class CriteriaAddUser extends GenericCriteria {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
