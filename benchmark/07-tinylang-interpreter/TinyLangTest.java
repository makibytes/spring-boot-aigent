package de.makibytes.benchmark.tinylang;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Authoritative tests for TinyLang.
 * The implementation under test must be named TinyLangImpl in this package.
 */
class TinyLangTest {

    private TinyLang lang;

    @BeforeEach
    void setUp() throws Exception {
        lang = (TinyLang) Class
                .forName("de.makibytes.benchmark.tinylang.TinyLangImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    // ── Integer arithmetic ────────────────────────────────────────────────────

    @Test void intAddition()       { assertThat(lang.eval("1 + 2")).isEqualTo(3L); }
    @Test void intSubtraction()    { assertThat(lang.eval("5 - 3")).isEqualTo(2L); }
    @Test void intMultiplication() { assertThat(lang.eval("2 * 3")).isEqualTo(6L); }

    /** 7 / 2 must truncate toward zero and return Long, not 3.5. */
    @Test void intDivisionTruncates() { assertThat(lang.eval("7 / 2")).isEqualTo(3L); }

    @Test void intModulo() { assertThat(lang.eval("10 % 3")).isEqualTo(1L); }

    // ── Operator precedence and grouping ──────────────────────────────────────

    @Test void precedenceMulBeforeAdd() { assertThat(lang.eval("1 + 2 * 3")).isEqualTo(7L); }
    @Test void precedenceParentheses()  { assertThat(lang.eval("(1 + 2) * 3")).isEqualTo(9L); }

    // ── Float arithmetic ──────────────────────────────────────────────────────

    @Test void floatAddition()  { assertThat(lang.eval("1.5 + 2.5")).isEqualTo(4.0); }
    @Test void floatDivision()  { assertThat(lang.eval("7.0 / 2.0")).isEqualTo(3.5); }

    /**
     * int + float must promote int to Double and return Double.
     * A model that keeps both operands as Long will return 1L instead of 1.5.
     */
    @Test void intFloatPromotion() { assertThat(lang.eval("1 + 0.5")).isEqualTo(1.5); }

    // ── Power operator ────────────────────────────────────────────────────────

    @Test void intPower() { assertThat(lang.eval("2 ^ 10")).isEqualTo(1024L); }

    /**
     * CRITICAL: ^ is RIGHT-associative.
     * 2^3^2 = 2^(3^2) = 2^9 = 512   ← correct (right-assoc)
     * (2^3)^2 = 8^2 = 64             ← wrong (left-assoc)
     */
    @Test void powerRightAssociative() { assertThat(lang.eval("2 ^ 3 ^ 2")).isEqualTo(512L); }

    /** int ^ negative exponent is undefined in integer arithmetic — must throw. */
    @Test void negativePowerThrows() {
        assertThatThrownBy(() -> lang.eval("2 ^ -1"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }

    // ── Unary operators ───────────────────────────────────────────────────────

    @Test void unaryMinus() { assertThat(lang.eval("-(3 + 4)")).isEqualTo(-7L); }

    // ── String operations ─────────────────────────────────────────────────────

    @Test void stringConcatenation() {
        assertThat(lang.eval("\"hello\" + \" \" + \"world\"")).isEqualTo("hello world");
    }

    /**
     * The TinyLang source string "line1\nline2" contains a two-character escape sequence.
     * The lexer must convert \n to an actual newline character (U+000A).
     * The Java assertion uses "line1\nline2" — a String with a real newline.
     */
    @Test void stringEscapeNewline() {
        assertThat(lang.eval("\"line1\\nline2\"")).isEqualTo("line1\nline2");
    }

    // ── Boolean and logical operators ─────────────────────────────────────────

    @Test void booleanAnd() { assertThat(lang.eval("true && false")).isEqualTo(false); }
    @Test void booleanOr()  { assertThat(lang.eval("false || true")).isEqualTo(true);  }
    @Test void booleanNot() { assertThat(lang.eval("!true")).isEqualTo(false); }

    /**
     * CRITICAL: && must short-circuit. The right-hand side "1/0 > 0" must
     * NEVER be evaluated when the left side is false. A model that eagerly
     * evaluates both sides will throw TinyLangException here instead of
     * returning false.
     */
    @Test void shortCircuitAnd() {
        assertThat(lang.eval("false && (1/0 > 0)")).isEqualTo(false);
    }

    /** Symmetric: || must short-circuit when left side is true. */
    @Test void shortCircuitOr() {
        assertThat(lang.eval("true || (1/0 > 0)")).isEqualTo(true);
    }

    // ── Comparison operators ──────────────────────────────────────────────────

    @Test void comparison() {
        assertThat(lang.eval("3 > 2 && 2 >= 2")).isEqualTo(true);
    }

    @Test void equalityStrings() {
        assertThat(lang.eval("\"abc\" == \"abc\" && 1 != 2")).isEqualTo(true);
    }

    /**
     * int == float must promote int to Double before comparing.
     * 1 == 1.0 must be true, not a type error.
     */
    @Test void intFloatEquality() {
        assertThat(lang.eval("1 == 1.0")).isEqualTo(true);
    }

    // ── Let bindings and scoping ──────────────────────────────────────────────

    @Test void letBinding() {
        assertThat(lang.eval("let x = 5 in x * x")).isEqualTo(25L);
    }

    @Test void nestedLet() {
        assertThat(lang.eval("let x = 2 in let y = x + 1 in x * y")).isEqualTo(6L);
    }

    /**
     * CRITICAL: inner let must shadow the outer binding.
     * Result is 2L (inner x), not 1L (outer x).
     */
    @Test void letShadowing() {
        assertThat(lang.eval("let x = 1 in let x = 2 in x")).isEqualTo(2L);
    }

    // ── Functions and closures ────────────────────────────────────────────────

    @Test void lambdaCall() {
        assertThat(lang.eval("let f = fn(x) => x * 2 in f(21)")).isEqualTo(42L);
    }

    @Test void multiArgFunction() {
        assertThat(lang.eval("let add = fn(x, y) => x + y in add(3, 4)")).isEqualTo(7L);
    }

    /**
     * CRITICAL: functions must capture their lexical environment (closure).
     * f is defined when x = 10. Calling f(5) must return 10 + 5 = 15L.
     * A model that uses dynamic scope instead of lexical scope will fail
     * this test (it may look up x in the call-time environment instead).
     */
    @Test void closure() {
        assertThat(lang.eval("let x = 10 in let f = fn(y) => x + y in f(5)"))
                .isEqualTo(15L);
    }

    // ── Conditionals ──────────────────────────────────────────────────────────

    @Test void ifTrue()  { assertThat(lang.eval("if 3 > 2 then \"yes\" else \"no\"")).isEqualTo("yes"); }
    @Test void ifFalse() { assertThat(lang.eval("if 1 > 2 then \"yes\" else \"no\"")).isEqualTo("no"); }

    // ── Recursion via letrec ──────────────────────────────────────────────────

    /**
     * CRITICAL: letrec must make the function name visible inside its own body.
     * Plain let would result in "undefined variable: fact" when fact calls itself.
     */
    @Test void letrecFactorial() {
        assertThat(lang.eval(
                "letrec fact = fn(n) => if n <= 1 then 1 else n * fact(n - 1) in fact(10)"
        )).isEqualTo(3628800L);
    }

    @Test void letrecFibonacci() {
        assertThat(lang.eval(
                "letrec fib = fn(n) => if n <= 1 then n else fib(n-1) + fib(n-2) in fib(10)"
        )).isEqualTo(55L);
    }

    // ── Higher-order functions ────────────────────────────────────────────────

    /**
     * Functions must be first-class values: passable as arguments and callable
     * through a variable that holds them.
     */
    @Test void higherOrder() {
        assertThat(lang.eval(
                "let apply = fn(f, x) => f(x) in apply(fn(x) => x * x, 7)"
        )).isEqualTo(49L);
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test void typeError_boolPlusInt() {
        assertThatThrownBy(() -> lang.eval("true + 1"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }

    @Test void intDivisionByZero() {
        assertThatThrownBy(() -> lang.eval("5 / 0"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }

    @Test void intModuloByZero() {
        assertThatThrownBy(() -> lang.eval("5 % 0"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }

    @Test void undefinedVariable() {
        assertThatThrownBy(() -> lang.eval("x + 1"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }

    @Test void wrongArgCountTooMany() {
        assertThatThrownBy(() -> lang.eval("let f = fn(x) => x in f(1, 2)"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }

    @Test void wrongArgCountTooFew() {
        assertThatThrownBy(() -> lang.eval("let f = fn(x, y) => x + y in f(1)"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }

    @Test void parseError() {
        assertThatThrownBy(() -> lang.eval("1 +"))
                .isInstanceOf(TinyLang.TinyLangException.class);
    }
}
