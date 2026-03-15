package de.makibytes.aigent;

import java.lang.annotation.*;

/**
 * Declares that a method has no observable side effects.
 *
 * <p>The AI must enforce this constraint when implementing: no mutation of parameters,
 * no IO, no randomness, no access to mutable external state.
 *
 * <p><strong>Only use on utility / static-like methods.</strong>
 * Do NOT annotate Spring {@code @Service} or {@code @Component} methods — Spring beans
 * have injected dependencies and are inherently stateful.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Pure {}
