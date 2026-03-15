package de.makibytes.aigent;

import de.makibytes.aigent.autoconfigure.StubDetector;

import java.lang.annotation.*;

/**
 * Work-order signal: this method needs to be implemented (or re-implemented) by the AI.
 *
 * <p>The {@code /aigent} command scans for {@code @Stub} as the authoritative work queue.
 * The optional {@code value()} is a note from the human to the AI — use it for targeted
 * re-implementation requests:
 * <pre>{@code
 * @Stub("fix: handle already-E.164 inputs — currently strips the + prefix")
 * }</pre>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>{@code @Stub} present → AI's turn: implement, remove {@code @Stub}, add {@code @AiNote}.
 *   <li>{@code @AiNote} present, no {@code @Stub} → Human's turn: review.
 *   <li>Neither present → Done: human-authored or accepted.
 * </ul>
 *
 * <p><b>At startup</b>, {@link StubDetector} will log a warning
 * (or throw, if {@code aigent.on-stub=FAIL}) for every Spring bean method still carrying
 * this annotation — catching unimplemented stubs before they reach production.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Stub {
    /** Optional note to the AI: reason, constraint, or targeted change request. */
    String value() default "";
}
