package de.makibytes.aigent.testing;

import de.makibytes.aigent.Example;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleRunnerTest {

    private final ExampleRunner runner = new ExampleRunner();

    @Test
    void passingExamplesReturnPassResult() {
        var results = runner.runAll(new CorrectImpl());
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(ExampleRunner.ExampleResult::passed);
    }

    @Test
    void failingExampleReturnFailResult() {
        var results = runner.runAll(new BuggyImpl());
        assertThat(results).anyMatch(r -> !r.passed());
        var fail = results.stream().filter(r -> !r.passed()).findFirst().orElseThrow();
        assertThat(fail.describe()).contains("FAIL");
    }

    @Test
    void exceptionInMethodReturnErrorResult() {
        var results = runner.runAll(new ThrowingImpl());
        assertThat(results).anyMatch(r -> !r.passed() && r.error() != null);
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    static class CorrectImpl {
        @Example(label = "basic", input = "'hello'", output = "'HELLO'")
        @Example(label = "empty", input = "''", output = "''")
        public String toUpper(String s) {
            return s.toUpperCase();
        }
    }

    @SuppressWarnings("unused")
    static class BuggyImpl {
        @Example(label = "should fail", input = "'hello'", output = "'HELLO'")
        public String toUpper(String s) {
            return s; // bug: no uppercase
        }
    }

    @SuppressWarnings("unused")
    static class ThrowingImpl {
        @Example(label = "boom", input = "'x'", output = "'X'")
        public String toUpper(String s) {
            throw new RuntimeException("oops");
        }
    }
}
