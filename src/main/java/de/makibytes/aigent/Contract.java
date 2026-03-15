package de.makibytes.aigent;

import java.lang.annotation.*;

/**
 * Formal pre/postcondition contract for a method.
 *
 * <p>Expression syntax (pseudo-Java, read by AI — not evaluated at runtime):
 * <ul>
 *   <li>{@code $result} — binds to the return value in {@code ensures}
 *   <li>{@code $input}  — binds to the first parameter in {@code requires}/{@code ensures}
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * @Contract(
 *     requires = "numbers != null",
 *     ensures  = "$result.stream().allMatch(n -> n.startsWith('+'))",
 *     throws_  = {NullPointerException.class}
 * )
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Contract {

    /** Precondition: must be true when the method is called. */
    String requires() default "";

    /** Postcondition: must be true when the method returns. {@code $result} is the return value. */
    String ensures() default "";

    /** Exception types thrown (or propagated) when {@code requires} is violated. */
    Class<? extends Throwable>[] throws_() default {};
}
