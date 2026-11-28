package org.dpp.backend.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handleException_includesDetails_whenShowDetailsIsTrue() {
        // given
        GlobalExceptionHandler handler = new GlobalExceptionHandler(true);

        // when
        ProblemDetail problem = handler.handleException(new RuntimeException("Testfehler"));

        // then
        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Es ist ein Fehler aufgetreten. Bitte versuche es später erneut.");
        assertThat(problem.getProperties())
                .containsKey("timestamp")
                .containsEntry("exception", "java.lang.RuntimeException")
                .containsEntry("message", "Testfehler");
    }

    @Test
    void handleException_hidesDetails_whenShowDetailsIsFalse() {
        // given
        GlobalExceptionHandler handler = new GlobalExceptionHandler(false);

        // when
        ProblemDetail problem = handler.handleException(new RuntimeException("Testfehler"));

        // then
        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getProperties())
                .containsKey("timestamp")
                .doesNotContainKeys("exception", "message");
    }
}