package it.sara.demo.service.assembler;

import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.GetUsersResponse;
import org.springframework.stereotype.Component;

/**
 * Bidirectional assembler for the user search flow. Translates the web request
 * into service-layer criteria, and the service result into the web response.
 */
@Component
public class GetUsersAssembler {

    /**
     * Maps a validated {@link GetUsersRequest} into the service-layer criteria.
     */
    public CriteriaGetUsers toCriteria(GetUsersRequest request) {
        CriteriaGetUsers c = new CriteriaGetUsers();
        c.setQuery(request.getQuery());
        c.setOffset(request.getOffset());
        c.setLimit(request.getLimit());
        c.setOrder(request.getOrder());
        return c;
    }

    /**
     * Maps a {@link GetUsersResult} into the outbound web response.
     */
    public GetUsersResponse toResponse(GetUsersResult result) {
        GetUsersResponse response = new GetUsersResponse();
        response.setUsers(result.getUsers());
        response.setTotal(result.getTotal());
        response.setOffset(result.getOffset());
        response.setLimit(result.getLimit());
        return response;
    }
}
