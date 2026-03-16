package de.makibytes.aigent.testing;

import de.makibytes.aigent.Property;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PropertyVerifierTest {

    @Test
    void propertyHolds_noViolations() {
        assertThatCode(() ->
                PropertyVerifier.verify(new CorrectMath(), "abs", -5, 0, 3, -100, 42)
        ).doesNotThrowAnyException();
    }

    @Test
    void propertyViolated_throwsAssertionError() {
        assertThatThrownBy(() ->
                PropertyVerifier.verify(new BuggyMath(), "abs", -5)
        ).isInstanceOf(AssertionError.class)
                .hasMessageContaining("$result >= 0");
    }

    @Test
    void checkReturnsViolationsWithoutThrowing() {
        var violations = PropertyVerifier.check(new BuggyMath(), "abs", -5, 3);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).input()).isEqualTo(-5);
    }

    @Test
    void multipleProperties_allChecked() {
        assertThatCode(() ->
                PropertyVerifier.verify(new LengthPreserving(), "toUpper", "hello", "WORLD", "")
        ).doesNotThrowAnyException();
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    static class CorrectMath {
        @Property("$result >= 0")
        public int abs(int x) {
            return Math.abs(x);
        }
    }

    @SuppressWarnings("unused")
    static class BuggyMath {
        @Property("$result >= 0")
        public int abs(int x) {
            return x; // bug: doesn't abs
        }
    }

    @SuppressWarnings("unused")
    static class LengthPreserving {
        @Property("$result.length() == $input.length()")
        @Property("$result.equals($result.toUpperCase())")
        public String toUpper(String s) {
            return s.toUpperCase();
        }
    }
}
