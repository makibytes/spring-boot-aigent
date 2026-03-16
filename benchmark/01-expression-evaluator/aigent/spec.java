package de.makibytes.benchmark.expression;

import de.makibytes.aigent.*;

/**
 * Aigent spec for ExpressionEvaluatorImpl.
 * Implement the @Stub method. All annotations are enforced at runtime via SpEL.
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
              4. unary - (applied after ^ is fully resolved: -2^2 = -(2^2) = -4)

            Key consequence: (-2)^2 = 4 but -2^2 = -4
            """)
    @Contract(
        requires = "$expression != null && !$expression.isBlank()",
        ensures  = "T(Double).isFinite($result)",
        throws_  = {IllegalArgumentException.class, ArithmeticException.class}
    )
    @Example(label = "addition",           input = "'2+3'",       output = "5.0")
    @Example(label = "mul before add",     input = "'2+3*4'",     output = "14.0")
    @Example(label = "parens override",    input = "'(2+3)*4'",   output = "20.0")
    @Example(label = "power precedence",   input = "'2+3^2'",     output = "11.0")
    @Example(label = "power right-assoc",  input = "'2^3^2'",     output = "512.0")
    @Example(label = "unary minus power",  input = "'-2^2'",      output = "-4.0")
    @Example(label = "unary minus parens", input = "'(-2)^2'",    output = "4.0")
    @Example(label = "unary on subexpr",   input = "'-(3+5)'",    output = "-8.0")
    @Example(label = "decimal literal",    input = "'1.5*2'",     output = "3.0")
    @Example(label = "decimal division",   input = "'7.0/2'",     output = "3.5")
    @Example(label = "nested parens",      input = "'((2+3))*2'", output = "10.0")
    @Example(label = "div by zero",        input = "'5/0'",       throws_ = ArithmeticException.class)
    @Example(label = "invalid double op",  input = "'2++3'",      throws_ = IllegalArgumentException.class)
    @Example(label = "unmatched paren",    input = "'(2+3'",      throws_ = IllegalArgumentException.class)
    @Property("T(Double).isFinite($result)")
    @Property("!T(Double).isNaN($result)")
    @Stub("Use a recursive-descent parser or Shunting-Yard algorithm. " +
          "^ must be Math.pow(), not Java's XOR operator. " +
          "Right-associativity of ^ and unary-minus-after-power are the two critical correctness points.")
    @Override
    public double evaluate(String expression) {
        throw new UnsupportedOperationException();
    }
}
