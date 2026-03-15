package com.aigent.benchmark.expression;

/**
 * Evaluates a mathematical expression string.
 * Implementations must be in the same package as ExpressionEvaluatorImpl.
 */
public interface ExpressionEvaluator {
    /**
     * Evaluates a mathematical expression string and returns the result.
     *
     * @param expression the expression to evaluate (never null)
     * @return the numeric result as a double
     * @throws IllegalArgumentException if the expression is syntactically invalid
     * @throws ArithmeticException      if the expression causes division by zero
     */
    double evaluate(String expression);
}
