package com.aigent.benchmark.expression;

import com.aigent.*;

/**
 * Aigent spec for ExpressionEvaluatorImpl.
 * Implement the @Stub method. The annotations define the contract precisely.
 */
public class ExpressionEvaluatorImpl implements ExpressionEvaluator {

    @Intent("""
            Evaluate a mathematical expression string and return the result as a double.

            Supported syntax:
              - Integer and decimal literals (e.g. 3, 3.14)
              - Binary operators: + (add), - (subtract), * (multiply), / (divide),
                ^ (power — see precedence rules below)
              - Unary minus (e.g. -3, -(2+1))
              - Parentheses for grouping

            Operator precedence (highest to lowest):
              1. ^ (power) — RIGHT-associative: 2^3^2 = 2^(3^2) = 2^9 = 512, NOT (2^3)^2 = 64
              2. * /  (left-associative)
              3. + -  (left-associative)
              4. unary - (applied after ^ is fully resolved)

            Key consequence of unary-minus precedence:
              -2^2 = -(2^2) = -4   (unary minus is applied AFTER power)
              (-2)^2 = 4           (parentheses force unary minus to bind first)
            """)
    @Contract(
        requires = "expression != null && !expression.isBlank()",
        ensures  = "Double.isFinite($result) unless division by zero",
        throws_  = {IllegalArgumentException.class, ArithmeticException.class}
    )
    @Example(label = "addition",              input = "\"2+3\"",       output = "5.0")
    @Example(label = "mul before add",        input = "\"2+3*4\"",     output = "14.0")
    @Example(label = "parens override",       input = "\"(2+3)*4\"",   output = "20.0")
    @Example(label = "power precedence",      input = "\"2+3^2\"",     output = "11.0")
    @Example(label = "power right-assoc",     input = "\"2^3^2\"",     output = "512.0  // NOT 64.0")
    @Example(label = "unary minus power",     input = "\"-2^2\"",      output = "-4.0   // NOT 4.0")
    @Example(label = "unary minus parens",    input = "\"(-2)^2\"",    output = "4.0")
    @Example(label = "unary on subexpr",      input = "\"-(3+5)\"",    output = "-8.0")
    @Example(label = "decimal literal",       input = "\"1.5*2\"",     output = "3.0")
    @Example(label = "decimal division",      input = "\"7.0/2\"",     output = "3.5")
    @Example(label = "nested parens",         input = "\"((2+3))*2\"", output = "10.0")
    @Example(label = "div by zero",           input = "\"5/0\"",       output = "throws ArithmeticException")
    @Example(label = "invalid: double op",    input = "\"2++3\"",      output = "throws IllegalArgumentException")
    @Example(label = "invalid: unmatched (",  input = "\"(2+3\"",      output = "throws IllegalArgumentException")
    @Property("evaluate(\"$a+$b\") == evaluate(\"$b+$a\") for all numeric $a, $b")
    @Property("evaluate(\"$a*($b+$c)\") == evaluate(\"$a*$b+$a*$c\") for all numeric $a, $b, $c")
    @Stub("Use a recursive-descent parser or Shunting-Yard algorithm. " +
          "^ must be Math.pow(), not the Java XOR bitwise operator. " +
          "Right-associativity of ^ and unary-minus precedence are the two critical correctness points.")
    @Override
    public double evaluate(String expression) {
        throw new UnsupportedOperationException();
    }
}
