package de.makibytes.aigent;

import java.lang.annotation.*;

/**
 * A concrete input→output example that the implementation must satisfy.
 * Repeatable — annotate the same method multiple times to express edge cases.
 *
 * <p>The AI must pass <em>all</em> {@code @Example} cases, not just the first.
 * Edge cases (empty input, already-valid input, boundary values) are especially important.
 *
 * <p>Example:
 * <pre>{@code
 * @Example(label = "typical",       input = "['555-0199']",  output = "['+15550199']")
 * @Example(label = "already E.164", input = "['+15550199']", output = "['+15550199']")
 * @Example(label = "empty list",    input = "[]",            output = "[]")
 * }</pre>
 */
@Repeatable(Examples.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Example {
    String input();

    /**
     * Expected return value as a SpEL literal (e.g. {@code "5.0"}, {@code "{'a','b'}"}, {@code "true"}).
     * Leave empty when {@link #throws_()} is set.
     */
    String output() default "";

    /**
     * Expected exception type when the input should cause the method to throw.
     * When set, {@link #output()} is ignored and the runner verifies an exception of this type is thrown.
     * Default {@code NoException.class} means no exception is expected.
     */
    Class<? extends Throwable> throws_() default Example.NoException.class;

    /** Sentinel: no exception expected. Do not use directly — use the default. */
    final class NoException extends RuntimeException {
        private NoException() {}
    }

    /** Optional human-readable name for this case, e.g. "empty list" or "already normalized". */
    String label() default "";
}
