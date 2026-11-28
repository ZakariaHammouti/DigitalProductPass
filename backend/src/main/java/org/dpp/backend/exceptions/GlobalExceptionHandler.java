package org.dpp.backend.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Richtige Daten	200 + Cookie + {"username","role"}
    // Falscher Name oder falsches Passwort	401 „Benutzername oder Passwort falsch“
    // Leere Felder	400 + errors pro Feld
    // Datenbank weg / unerwarteter Fehler	500 allgemein, lokal mit Details
    // Geschützter Pfad ohne Login	401
    // Logout	200

    private final boolean showDetails;

    public GlobalExceptionHandler(@Value("${app.errors.show-details:false}") boolean showDetails) {

        this.showDetails = showDetails;
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleInternalAuthentication(InternalAuthenticationServiceException e) {
        return serverError(e);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleException(Exception e) {
        return serverError(e);
    }

    private ProblemDetail serverError(Exception e) {
        log.error("Unerwarteter Fehler", e);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Es ist ein Fehler aufgetreten. Bitte versuche es später erneut.");
        problem.setProperty("timestamp", Instant.now());

        if (showDetails) {
            problem.setProperty("exception", e.getClass().getName());
            problem.setProperty("message", e.getMessage());
        }
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAuthentication(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Benutzername oder Passwort falsch");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, List<String>> validationErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();

            validationErrors
                    .computeIfAbsent(fieldName, key -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        });

        ProblemDetail problem = ex.getBody();
        problem.setDetail("Eingaben sind ungültig");
        problem.setProperty("errors", validationErrors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleDisabled(DisabledException e) {
        //beim anlegen eines neuen Benutzers ist der AccountStatus auf PENDING gesetzt,
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Dein Konto wartet noch auf Freigabe");
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleLocked(LockedException e) {
        //Beim Sperren eines Benutzers ist der AccountStatus auf REJECTED oder LOCKED gesetzt
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Dein Konto ist gesperrt oder wurde abgelehnt");
    }
}
