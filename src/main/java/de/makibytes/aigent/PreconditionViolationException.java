package de.makibytes.aigent;

/**
 * Thrown when a {@link Contract#requires()} precondition evaluates to {@code false}.
 */
public class PreconditionViolationException extends RuntimeException {

    private final String expression;
    private final String methodName;
    private final Object[] args;

    public PreconditionViolationException(String expression, String methodName, Object[] args) {
        super("Precondition violated on %s: \"%s\"".formatted(methodName, expression));
        this.expression = expression;
        this.methodName = methodName;
        this.args = args;
    }

    public String getExpression() { return expression; }
    public String getMethodName() { return methodName; }
    public Object[] getArgs() { return args; }
}
