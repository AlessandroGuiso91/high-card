package it.sara.demo.service.assembler;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.criteria.CriteriaGetUsers.OrderType;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.GetUsersResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GetUsersAssembler}: round-trip of pagination metadata and
 * order through the bidirectional mapper.
 */
class GetUsersAssemblerTest {

    private final GetUsersAssembler assembler = new GetUsersAssembler();

    @Test
    void toCriteria_propagatesAllFields() {
        GetUsersRequest request = new GetUsersRequest();
        request.setQuery("ross");
        request.setOffset(20);
        request.setLimit(50);
        request.setOrder(OrderType.BY_LASTNAME_DESC);

        CriteriaGetUsers criteria = assembler.toCriteria(request);

        assertThat(criteria.getQuery()).isEqualTo("ross");
        assertThat(criteria.getOffset()).isEqualTo(20);
        assertThat(criteria.getLimit()).isEqualTo(50);
        assertThat(criteria.getOrder()).isEqualTo(OrderType.BY_LASTNAME_DESC);
    }

    @Test
    void toResponse_propagatesPaginationMetadata() {
        UserDTO dto = new UserDTO();
        dto.setFirstName("Mario");
        GetUsersResult result = new GetUsersResult();
        result.setUsers(List.of(dto));
        result.setTotal(42);
        result.setOffset(10);
        result.setLimit(5);

        GetUsersResponse response = assembler.toResponse(result);

        assertThat(response.getUsers()).hasSize(1);
        assertThat(response.getTotal()).isEqualTo(42);
        assertThat(response.getOffset()).isEqualTo(10);
        assertThat(response.getLimit()).isEqualTo(5);
    }
}
