package it.sara.demo.web.user.request;

import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.web.request.GenericRequest;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for {@code POST /user/v1/user}. Bounds on {@code offset}, {@code limit}
 * and {@code query} length are enforced at the controller boundary via {@code @Valid}.
 */
@Getter
@Setter
public class GetUsersRequest extends GenericRequest {

    @Size(max = 50, message = "Search query cannot exceed 50 characters")
    private String query;

    @NotNull(message = "Offset is mandatory")
    @Min(value = 0, message = "Offset cannot be negative")
    private Integer offset;

    @NotNull(message = "Limit is mandatory")
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    private Integer limit;

    private OrderType order;

}
