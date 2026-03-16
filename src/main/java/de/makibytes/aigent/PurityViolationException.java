package de.makibytes.aigent;

/**
 * Thrown when a {@link Pure}-annotated method is detected to have mutated its input parameters.
 */
public class PurityViolationException extends RuntimeException {

    public PurityViolationException(String methodName, String detail) {
        super("Purity violated on %s: %s".formatted(methodName, detail));
    }
}
