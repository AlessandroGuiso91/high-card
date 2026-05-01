package it.sara.demo.exception;

import it.sara.demo.dto.StatusDTO;
import it.sara.demo.web.response.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralized exception handler. Translates exceptions into a {@link GenericResponse}
 * carrying a {@link StatusDTO} (code, message, traceId) and always returns HTTP 200,
 * per the project's status-in-body convention. Specific handlers are picked over
 * the catch-all by Spring based on type hierarchy, regardless of declaration order.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles business-level errors thrown by the service layer. The {@link StatusDTO}
     * carried by the exception is propagated to the client unchanged.
     */
    @ExceptionHandler(GenericException.class)
    public ResponseEntity<GenericResponse> handleGeneric(GenericException ex) {
        log.warn("Business error [{}] {}", ex.getStatus().getCode(), ex.getStatus().getMessage());
        GenericResponse body = new GenericResponse();
        body.setStatus(ex.getStatus());
        return ResponseEntity.ok(body);
    }

    /**
     * Handles Bean Validation failures triggered by {@code @Valid} on controller
     * arguments. Aggregates all field errors into a single message and assigns code 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failure: {}", message);
        return ResponseEntity.ok(buildBody(400, message));
    }

    /**
     * Catch-all for unexpected errors. Logs the stack trace and returns a generic
     * 500 status to the client; internal details are not leaked into the response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.ok(buildBody(500, "Generic error"));
    }

    /**
     * Builds a {@link GenericResponse} envelope with a freshly generated traceId.
     */
    private static GenericResponse buildBody(int code, String message) {
        StatusDTO status = new StatusDTO();
        status.setCode(code);
        status.setMessage(message);
        status.setTraceId(UUID.randomUUID().toString());
        GenericResponse response = new GenericResponse();
        response.setStatus(status);
        return response;
    }
}
