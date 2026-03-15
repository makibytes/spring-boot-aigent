package de.makibytes.aigent;

import java.lang.annotation.*;

/**
 * A universal invariant expressed in pseudo-Java that must hold for any valid input.
 * Repeatable — annotate the same method multiple times for multiple invariants.
 *
 * <p>Bound variables:
 * <ul>
 *   <li>{@code $result} — the return value
 *   <li>{@code $input}  — the first parameter
 * </ul>
 *
 * <p>The AI uses these to generate property-based tests (jqwik {@code @Property}
 * or JUnit {@code @ParameterizedTest}).
 *
 * <p>Example:
 * <pre>{@code
 * @Property("$result.size() == $input.size()")
 * @Property("$result.stream().allMatch(n -> n.matches(\"\\+[0-9]+\"))")
 * }</pre>
 */
@Repeatable(Properties.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Property {
    String value();
}
