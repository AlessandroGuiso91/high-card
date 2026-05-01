package it.sara.demo.exception;

import it.sara.demo.dto.StatusDTO;
import it.sara.demo.web.response.GenericResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GlobalExceptionHandler}: every exception flavour must be
 * translated into HTTP 200 + a {@link GenericResponse} envelope carrying the
 * intended status code, per the project's status-in-body convention.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void genericException_propagatesCarriedStatus() {
        GenericException ex = new GenericException(418, "I am a teapot");

        StatusDTO status = statusOf(handler.handleGeneric(ex));

        assertThat(status.getCode()).isEqualTo(418);
        assertThat(status.getMessage()).isEqualTo("I am a teapot");
        assertThat(status.getTraceId()).isNotBlank();
    }

    @Test
    void validationException_aggregatesFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Invalid email format"));
        bindingResult.addError(new FieldError("request", "phoneNumber", "Invalid phone"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

        StatusDTO status = statusOf(handler.handleValidation(ex));

        assertThat(status.getCode()).isEqualTo(400);
        assertThat(status.getMessage())
                .contains("email: Invalid email format")
                .contains("phoneNumber: Invalid phone");
    }

    @Test
    void authenticationException_returnsUniformInvalidCredentialsMessage() {
        StatusDTO status = statusOf(handler.handleAuthFailure(
                new BadCredentialsException("Specific cause that must not leak")));

        assertThat(status.getCode()).isEqualTo(401);
        assertThat(status.getMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    void accessDeniedException_returnsForbidden() {
        StatusDTO status = statusOf(handler.handleAccessDenied(
                new AccessDeniedException("not allowed")));

        assertThat(status.getCode()).isEqualTo(403);
        assertThat(status.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void unexpectedException_returnsGenericError() {
        StatusDTO status = statusOf(handler.handleUnexpected(
                new RuntimeException("internal detail that must not leak")));

        assertThat(status.getCode()).isEqualTo(500);
        assertThat(status.getMessage()).isEqualTo("Generic error");
    }

    /**
     * Asserts the response is HTTP 200 with a non-null body and status, returning
     * the {@link StatusDTO} for further assertions. Centralises the null-safety
     * checks so each test reads as a sequence of behavioural expectations.
     */
    private static StatusDTO statusOf(ResponseEntity<GenericResponse> response) {
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        GenericResponse body = response.getBody();
        assertThat(body).isNotNull();
        StatusDTO status = body.getStatus();
        assertThat(status).isNotNull();
        return status;
    }

    private static MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        Method method = DefaultListableBeanFactory.class.getMethod("toString");
        return new MethodParameter(method, -1);
    }
}
