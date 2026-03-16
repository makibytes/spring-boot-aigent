package de.makibytes.benchmark.tinylang;

import de.makibytes.aigent.*;

/**
 * Aigent spec for TinyLangImpl.
 * All annotations are enforced at runtime via SpEL.
 *
 * You may (and should) create additional classes in the same package:
 * a Lexer, Token, Expr hierarchy, Parser, Value types, Evaluator, and Env record.
 * TinyLangImpl is the required entry point.
 */
public class TinyLangImpl implements TinyLang {

    @Intent("""
            Implement a tree-walking interpreter for TinyLang, a small dynamically typed
            functional expression language. Return the result of evaluating the source string.

            === GRAMMAR ===

              program     ::= expr

              expr        ::= let_expr | letrec_expr | if_expr | fn_expr | or_expr
              let_expr    ::= 'let' IDENT '=' expr 'in' expr
              letrec_expr ::= 'letrec' IDENT '=' 'fn' '(' params ')' '=>' expr 'in' expr
              if_expr     ::= 'if' expr 'then' expr 'else' expr
              fn_expr     ::= 'fn' '(' params ')' '=>' expr
              params      ::= IDENT (',' IDENT)*

              or_expr     ::= and_expr ('||' and_expr)*
              and_expr    ::= not_expr ('&&' not_expr)*
              not_expr    ::= '!' not_expr | cmp_expr
              cmp_expr    ::= add_expr (('==' | '!=' | '<' | '<=' | '>' | '>=') add_expr)?
              add_expr    ::= mul_expr (('+' | '-') mul_expr)*
              mul_expr    ::= pow_expr (('*' | '/' | '%') pow_expr)*
              pow_expr    ::= unary ('^' pow_expr)?           ← RIGHT-associative
              unary       ::= '-' unary | call_expr
              call_expr   ::= primary ('(' args ')')*
              primary     ::= '(' expr ')' | INT | FLOAT | BOOL | STRING | IDENT
              args        ::= (expr (',' expr)*)?

            === TYPES ===

              INT literal   → Java Long
              FLOAT literal → Java Double   (contains a decimal point, e.g. 3.14, 1.0)
              BOOL literal  → Java Boolean  (true / false)
              STRING literal → Java String  (delimited by ", supports \\n \\t \\\\ \\" escapes)

            === ARITHMETIC TYPE RULES ===

              Long   op Long   → Long
              Double op Double → Double
              Long   op Double → Double  (int is promoted)
              Double op Long   → Double

              String + String  → String (concatenation; String op non-String → TinyLangException)
              int / int        → Long with truncation toward zero  (7/2 = 3, NOT 3.5)
              int / 0          → TinyLangException
              float / 0.0      → Double.POSITIVE/NEGATIVE_INFINITY (IEEE 754, NOT exception)
              int % int        → Long; int % 0 → TinyLangException
              int ^ int(≥ 0)   → Long  (via (long) Math.pow(base, exp))
              int ^ int(< 0)   → TinyLangException  (negative exponent for integer base)
              float ^ anything → Double  (via Math.pow)

            === COMPARISON TYPE RULES ===

              == and !=  : Long==Long, Double==Double, Boolean==Boolean, String==String → Boolean
                           Long==Double or Double==Long → promote to Double, then compare
                           any other type pair → TinyLangException
              < <= > >=  : numeric only (Long, Double, with promotion); other types → TinyLangException

            === LOGICAL OPERATORS ===

              && || !    : Boolean operands only; non-Boolean → TinyLangException
              && and ||  : SHORT-CIRCUIT — do NOT evaluate the right operand when the result
                           is already determined by the left operand alone.
                           false && e  ← e is never evaluated
                           true  || e  ← e is never evaluated

            === SCOPING ===

              Lexical scope throughout.
              let x = e1 in e2   : evaluates e1, binds result to x in a new scope, evaluates e2.
                                   x is NOT visible in e1.
              letrec f = fn(params) => body in expr
                                 : f IS visible inside body (enables recursion).
                                   Implemented via mutable cell / back-patching.
              fn(params) => body : creates a closure that captures the CURRENT environment
                                   at definition time (lexical, not dynamic).
              Calling a function: evaluate all arguments left-to-right, then evaluate body
                                  in a new scope. Wrong argument count → TinyLangException.

            === RECOMMENDED IMPLEMENTATION STRUCTURE ===

              1. Lexer    : tokenises the source string
              2. Parser   : recursive-descent, builds an AST (sealed interfaces + records)
              3. Env      : immutable chain — record Env(String name, Object value, Env parent)
              4. Evaluator: tree-walking eval(Expr, Env) with pattern matching
                            Handle && and || as SPECIAL FORMS so short-circuit works correctly.
            """)
    @Contract(
        requires = "$source != null",
        ensures  = "$result instanceof T(Long) || $result instanceof T(Double) || $result instanceof T(Boolean) || $result instanceof T(String)",
        throws_  = {TinyLang.TinyLangException.class, NullPointerException.class}
    )

    // ── Arithmetic ──────────────────────────────────────────────────────────
    @Example(label = "int arithmetic",      input = "'1 + 2 * 3'",  output = "7L")
    @Example(label = "int div truncates",   input = "'7 / 2'",      output = "3L")
    @Example(label = "float division",      input = "'7.0 / 2.0'",  output = "3.5")
    @Example(label = "int-float promotion", input = "'1 + 0.5'",    output = "1.5")
    @Example(label = "power int",           input = "'2 ^ 10'",     output = "1024L")

    // ── CRITICAL: right-associative power ───────────────────────────────────
    @Example(label = "power right-assoc",   input = "'2 ^ 3 ^ 2'",  output = "512L")

    // ── CRITICAL: short-circuit evaluation ──────────────────────────────────
    @Example(label = "short-circuit AND",   input = "'false && (1/0 > 0)'",  output = "false")
    @Example(label = "short-circuit OR",    input = "'true || (1/0 > 0)'",   output = "true")

    // ── CRITICAL: lexical closure capture ───────────────────────────────────
    @Example(label = "closure",             input = "'let x = 10 in let f = fn(y) => x + y in f(5)'", output = "15L")

    // ── CRITICAL: letrec self-reference ─────────────────────────────────────
    @Example(label = "letrec factorial",    input = "'letrec fact = fn(n) => if n <= 1 then 1 else n * fact(n - 1) in fact(5)'", output = "120L")

    // ── let shadowing ────────────────────────────────────────────────────────
    @Example(label = "let shadowing",       input = "'let x = 1 in let x = 2 in x'", output = "2L")

    // ── int-float equality (promotion, not type error) ───────────────────────
    @Example(label = "int-float equality",  input = "'1 == 1.0'",   output = "true")

    // ── higher-order functions ───────────────────────────────────────────────
    @Example(label = "higher-order apply",  input = "'let apply = fn(f, x) => f(x) in apply(fn(x) => x * x, 7)'", output = "49L")

    // ── Error cases ──────────────────────────────────────────────────────────
    @Example(label = "negative int exponent",  input = "'2 ^ -1'",       throws_ = TinyLang.TinyLangException.class)
    @Example(label = "int division by zero",   input = "'5 / 0'",        throws_ = TinyLang.TinyLangException.class)
    @Example(label = "type error bool+int",    input = "'true + 1'",     throws_ = TinyLang.TinyLangException.class)
    @Example(label = "undefined variable",     input = "'x + 1'",        throws_ = TinyLang.TinyLangException.class)
    @Example(label = "wrong arg count",        input = "'let f = fn(x) => x in f(1, 2)'", throws_ = TinyLang.TinyLangException.class)

    @Property("$result instanceof T(Long) || $result instanceof T(Double) || $result instanceof T(Boolean) || $result instanceof T(String)")
    @Property("!T(Double).isNaN($result instanceof T(Double) ? (Double)$result : 0.0D)")

    @Stub("""
            Implement as a pipeline: Lexer → Parser → tree-walking Evaluator.

            Key correctness requirements:
            1. Use an immutable env CHAIN (record Env(String name, Object value, Env parent)).
               Do NOT use a single HashMap — it will break closure capture and shadowing.
            2. Handle && and || as SPECIAL FORMS in the evaluator switch/dispatch,
               NOT as regular binary operators. This ensures short-circuit evaluation.
            3. For letrec: create a one-element array Object[] ref = {null}, build the closure
               with a lambda/anonymous class, then assign ref[0] = closure and bind the name
               to a forwarding lookup — or use a mutable Env cell just for the recursive binding.
            4. ^ is right-associative: parse pow_expr ::= unary ('^' pow_expr)? recursively.
            5. int ^ negative_int → TinyLangException (do not return 0 from casting Math.pow).
            6. int / int → Long with truncation (Java's / operator on long does this natively).

            The @Contract.ensures and @Example annotations are runtime-verified — they will
            catch type-rule and short-circuit mistakes automatically.
            """)
    @Override
    public Object eval(String source) {
        throw new UnsupportedOperationException();
    }
}
