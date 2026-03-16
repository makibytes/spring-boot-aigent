package de.makibytes.benchmark.tinylang;

/**
 * TinyLang — a dynamically typed functional expression language.
 *
 * <p>Create an interpreter instance with {@link #create()}.
 * Each call to {@link #eval} is independent (no shared mutable state between calls).
 *
 * <p>Result types:
 * <ul>
 *   <li>{@code Long}    — integer literal or integer arithmetic result
 *   <li>{@code Double}  — float literal, float arithmetic, or int-float promoted result
 *   <li>{@code Boolean} — boolean literal or logical/comparison result
 *   <li>{@code String}  — string literal or string concatenation result
 * </ul>
 */
public interface TinyLang {

    /**
     * Evaluates a TinyLang source expression and returns the result.
     *
     * @param source the expression source; must not be null
     * @return a {@code Long}, {@code Double}, {@code Boolean}, or {@code String}
     * @throws TinyLangException  on any runtime error (type mismatch, undefined variable,
     *                            division by zero, wrong argument count, parse error, …)
     * @throws NullPointerException if source is null
     */
    Object eval(String source);

    /**
     * Creates a new TinyLang interpreter.
     * The implementation class must be {@code TinyLangImpl} in this package.
     */
    static TinyLang create() {
        try {
            return (TinyLang) Class
                    .forName("de.makibytes.benchmark.tinylang.TinyLangImpl")
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("TinyLangImpl not found in this package", e);
        }
    }

    /** Thrown for all TinyLang runtime and parse errors. */
    class TinyLangException extends RuntimeException {
        public TinyLangException(String message) { super(message); }
    }
}
