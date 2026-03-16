package de.makibytes.aigent;

/**
 * Thrown when a {@link Contract#ensures()} postcondition evaluates to {@code false}.
 */
public class PostconditionViolationException extends RuntimeException {

    private final String expression;
    private final String methodName;
    private final Object result;

    public PostconditionViolationException(String expression, String methodName, Object result) {
        super("Postcondition violated on %s: \"%s\" (result was: %s)".formatted(methodName, expression, result));
        this.expression = expression;
        this.methodName = methodName;
        this.result = result;
    }

    public String getExpression() { return expression; }
    public String getMethodName() { return methodName; }
    public Object getResult() { return result; }
}
