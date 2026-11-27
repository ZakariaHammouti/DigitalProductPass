package org.dpp.backend.help;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxBytesValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public record TestRecord(@MaxBytes(72) String value) {
    }

    private boolean isValid(String value) {
        return validator.validate(new TestRecord(value)).isEmpty();
    }

    @Test
    void isValid_whenNull() {
        assertThat(isValid(null)).isTrue();
    }

    @Test
    void isValid_whenExactly72AsciiCharacters() {
        assertThat(isValid("a".repeat(72))).isTrue();
    }

    @Test
    void isInvalid_when73AsciiCharacters() {
        assertThat(isValid("a".repeat(73))).isFalse();
    }

    @Test
    void isInvalid_whenFewCharactersButTooManyBytes() {
        String umlauts = "ä".repeat(40);   // 40 Zeichen, aber 80 Bytes
        assertThat(umlauts.length()).isEqualTo(40);
        assertThat(isValid(umlauts)).isFalse();
    }
}