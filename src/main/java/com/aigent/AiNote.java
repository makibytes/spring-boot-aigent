package com.aigent;

import java.lang.annotation.*;

/**
 * Written by the AI after it implements a {@link Stub}-annotated method.
 * Creates a structured feedback channel — human-readable, machine-readable, and living
 * in the same source file as the spec and implementation.
 *
 * <p>The AI must:
 * <ol>
 *   <li>Remove {@code @Stub} from the method.
 *   <li>Write the implementation body.
 *   <li>Add (or update) {@code @AiNote} immediately before the method signature.
 * </ol>
 *
 * <p>The human responds by either:
 * <ul>
 *   <li>Removing {@code @AiNote} — accepts the implementation. Done.
 *   <li>Refining spec annotations and re-adding {@code @Stub} — triggers the next iteration.
 * </ul>
 *
 * <p>The AI must NOT modify {@code @Intent}, {@code @Contract}, {@code @Example},
 * or {@code @Property} — those belong to the human.
 *
 * <p>Example:
 * <pre>{@code
 * @AiNote(
 *     confidence = Confidence.HIGH,
 *     assumed    = "All inputs are North American numbers requiring a +1 country code.",
 *     open       = "Should international numbers be supported? Not mentioned in @Intent."
 * )
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiNote {

    /** How confident the AI is in its implementation. */
    Confidence confidence() default Confidence.MEDIUM;

    /** What the AI assumed beyond what's stated in the spec. Empty string if nothing was assumed. */
    String assumed() default "";

    /** Open questions for the human to answer before the next iteration. Empty string if none. */
    String open() default "";
}
