package it.sara.demo.web.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Base web-layer response for paginated endpoints. Carries pagination metadata
 * (echo of input plus the filtered total) so the client can compute page boundaries.
 */
@Getter
@Setter
public class GenericPagedResponse extends GenericResponse {
    private int total;
    private int offset;
    private int limit;
}
