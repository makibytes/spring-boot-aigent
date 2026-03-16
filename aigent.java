package de.makibytes.aigent;

import java.lang.annotation.*;

// ── HUMAN → AI: Specification Annotations ────────────────────────────────────

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Intent { String value(); }

// $result binds to the return value in ensures expressions (JML/Eiffel convention).
// $input  binds to the first parameter.
// throws_() lists exception types the implementation must throw (or propagate) when requires is violated.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Contract {
    String requires() default "";
    String ensures()  default "";
    Class<? extends Throwable>[] throws_() default {};
}

@Repeatable(Examples.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Example {
    String input();
    String output();
    String label() default "";
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Examples { Example[] value(); }

// Universal invariant expressed in pseudo-Java. $result and $input are bound.
// Used by AI to generate property-based tests (jqwik @Property or JUnit @ParameterizedTest).
@Repeatable(Properties.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Property { String value(); }

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Properties { Property[] value(); }

// Work-order signal: AI must implement (or re-implement) this method.
// Human re-adds @Stub after reviewing to trigger a new iteration.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Stub {
    String value() default ""; // optional note: reason, constraint, or targeted change request
}

// No side effects, no mutation of parameters, no IO, no random, no external state.
// Use ONLY on utility/static-like methods. Do NOT use on Spring @Service/@Component methods.
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface Pure {}


// ── AI → HUMAN: Feedback Annotation ─────────────────────────────────────────

// Confidence level the AI assigns to its implementation.
enum Confidence { HIGH, MEDIUM, LOW }

// AI writes this annotation when it implements a @Stub method.
// It makes the AI's reasoning transparent so the human can review precisely what changed.
//
// Lifecycle signals:
//   @Stub present              → work order: AI must implement / re-implement
//   @AiNote present, no @Stub  → review request: human must inspect
//   neither present            → done: human-authored or accepted
//
// Human responds to @AiNote by:
//   a) removing it (accepts the implementation), or
//   b) refining the spec annotations and re-adding @Stub (triggers next iteration).
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface AiNote {
    Confidence confidence() default Confidence.MEDIUM;
    String assumed() default ""; // what the AI assumed that isn't stated in the spec
    String open()    default ""; // open questions for the human to answer before next iteration
}
