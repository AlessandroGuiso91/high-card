package it.sara.demo.web.response;

import it.sara.demo.dto.StatusDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * Base outbound web payload. Carries the project's {@link StatusDTO} envelope so
 * every response — successful or not — has the same outer shape.
 */
@Getter
@Setter
public class GenericResponse {
    private StatusDTO status;

    /**
     * Builds a successful envelope (code 200) with the given human-readable message
     * and a freshly generated traceId.
     */
    public static GenericResponse success(String message) {
        GenericResponse returnValue = new GenericResponse();
        returnValue.setStatus(new StatusDTO());
        returnValue.getStatus().setCode(200);
        returnValue.getStatus().setMessage(message != null ? message : "Success");
        returnValue.getStatus().setTraceId(java.util.UUID.randomUUID().toString());
        return returnValue;
    }
}
