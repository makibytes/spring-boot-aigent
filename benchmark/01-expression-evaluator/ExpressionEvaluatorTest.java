package de.makibytes.benchmark.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for ExpressionEvaluator.
 * Tests are ordered from basic (should always pass) to tricky (often fail in one-shot).
 * The implementation under test must be named ExpressionEvaluatorImpl in this package.
 */
class ExpressionEvaluatorTest {

    private ExpressionEvaluator ev;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        ev = (ExpressionEvaluator) Class
                .forName("de.makibytes.benchmark.expression.ExpressionEvaluatorImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    // ── Basic operations ──────────────────────────────────────────────────────

    @Test void addition()       { assertThat(ev.evaluate("2+3")).isCloseTo(5.0,  within(1e-9)); }
    @Test void subtraction()    { assertThat(ev.evaluate("10-3")).isCloseTo(7.0, within(1e-9)); }
    @Test void multiplication() { assertThat(ev.evaluate("2*3")).isCloseTo(6.0,  within(1e-9)); }
    @Test void division()       { assertThat(ev.evaluate("10/4")).isCloseTo(2.5, within(1e-9)); }

    // ── Operator precedence ───────────────────────────────────────────────────

    @Test void mulBeforeAdd()  { assertThat(ev.evaluate("2+3*4")).isCloseTo(14.0, within(1e-9)); }
    @Test void parentheses()   { assertThat(ev.evaluate("(2+3)*4")).isCloseTo(20.0, within(1e-9)); }
    @Test void nestedParens()  { assertThat(ev.evaluate("((2+3))*2")).isCloseTo(10.0, within(1e-9)); }

    // ── Power operator ────────────────────────────────────────────────────────

    @Test void simplePower()   { assertThat(ev.evaluate("2^3")).isCloseTo(8.0, within(1e-9)); }
    @Test void powerBeforeMul() { assertThat(ev.evaluate("2+3^2")).isCloseTo(11.0, within(1e-9)); }

    /**
     * CRITICAL: ^ is RIGHT-associative.
     * 2^3^2 = 2^(3^2) = 2^9 = 512   ← correct
     * (2^3)^2 = 8^2 = 64             ← wrong (left-associative mistake)
     */
    @Test void powerRightAssociative() {
        assertThat(ev.evaluate("2^3^2")).isCloseTo(512.0, within(1e-9));
    }

    // ── Unary minus ───────────────────────────────────────────────────────────

    @Test void unaryMinusBasic()  { assertThat(ev.evaluate("-3+5")).isCloseTo(2.0,  within(1e-9)); }
    @Test void unaryMinusParen()  { assertThat(ev.evaluate("-(3+5)")).isCloseTo(-8.0, within(1e-9)); }

    /**
     * CRITICAL: Unary minus has LOWER precedence than power.
     * -2^2 = -(2^2) = -4   ← correct (unary minus applied last)
     *  (-2)^2 = 4           ← what you'd get if unary binds first
     */
    @Test void unaryMinusPowerPrecedence() {
        assertThat(ev.evaluate("-2^2")).isCloseTo(-4.0, within(1e-9));
    }

    /** With explicit parentheses around the base, unary should apply first: (-2)^2 = 4. */
    @Test void unaryMinusParenthesizedBase() {
        assertThat(ev.evaluate("(-2)^2")).isCloseTo(4.0, within(1e-9));
    }

    // ── Decimal numbers ───────────────────────────────────────────────────────

    @Test void decimalLiteral()  { assertThat(ev.evaluate("1.5*2")).isCloseTo(3.0,   within(1e-9)); }
    @Test void decimalResult()   { assertThat(ev.evaluate("7.0/2")).isCloseTo(3.5,   within(1e-9)); }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test void divisionByZero() {
        assertThatThrownBy(() -> ev.evaluate("5/0"))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test void invalidExpression() {
        assertThatThrownBy(() -> ev.evaluate("2++3"))
                .isInstanceOf(Exception.class);  // IllegalArgumentException or similar
    }

    @Test void unmatchedParen() {
        assertThatThrownBy(() -> ev.evaluate("(2+3"))
                .isInstanceOf(Exception.class);
    }
}
