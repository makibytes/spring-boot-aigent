package de.makibytes.aigent;

/**
 * Thrown when a {@link Contract} expression cannot be parsed or evaluated.
 * Wraps the underlying SpEL exception for debugging.
 */
public class ContractEvaluationException extends RuntimeException {

    public ContractEvaluationException(String expression, String methodName, Throwable cause) {
        super("Failed to evaluate contract expression on %s: \"%s\"".formatted(methodName, expression), cause);
    }
}
