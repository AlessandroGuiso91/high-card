package it.sara.demo.service.result;

import lombok.Getter;
import lombok.Setter;

/**
 * Base service-layer result for paginated queries. Holds offset/limit echo and the
 * total count of items <em>after filtering</em>; subclasses carry the actual page items.
 */

@Setter
@Getter
public class GenericPagedResult extends GenericResult {
    private int total;
    private int offset;
    private int limit;
}
