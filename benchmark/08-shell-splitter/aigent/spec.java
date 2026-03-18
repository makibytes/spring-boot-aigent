package de.makibytes.benchmark.shell;

import de.makibytes.aigent.*;
import java.util.List;

/**
 * Aigent spec for ShellSplitterImpl.
 * Implement the @Stub method. All rules are tested by ShellSplitterTest.
 */
public class ShellSplitterImpl implements ShellSplitter {

    @Intent("""
            Split a shell command-line string into a list of tokens following
            simplified POSIX word-splitting and quote-removal rules.

            RULES — apply in this order:

            1. SINGLE QUOTES 'x':
               Everything between ' and the next ' is literal — NO escape processing.
               Backslash, dollar, newline, etc. are all regular characters.
               There is NO way to include a single-quote inside single quotes.

            2. DOUBLE QUOTES "x":
               Only five sequences are special:
                 \\"  → literal double-quote
                 \\\\  → literal backslash
                 \\$  → literal dollar sign
                 \\`  → literal backtick
                 \\<newline>  → line continuation: BOTH characters are removed
               Any other \\X (e.g. \\n, \\t, \\r) is kept as-is: the backslash
               is NOT consumed (result is \\n / \\t / \\r as two chars).

            3. BACKSLASH OUTSIDE QUOTES:
               \\<newline>  → line continuation: BOTH characters are removed (not token break).
               \\X          → just X (backslash consumed, next character kept literally).
               This also means \\<space> escapes the space — it joins the adjacent
               characters into the same token rather than splitting.

            4. WHITESPACE SPLITTING:
               Space and tab (unquoted) are word delimiters.
               Multiple adjacent delimiters count as one.
               Leading/trailing whitespace produces NO empty tokens.

            5. TOKEN JOINING:
               Adjacent quoted/unquoted/backslash-escaped segments with NO intervening
               whitespace form a SINGLE token. E.g.: a"b c"d → "ab cd" (one token).

            6. EMPTY QUOTED STRINGS:
               '' and "" each contribute an empty string ("") to the token list
               when they appear as a standalone word or when they are adjacent to
               nothing else. A standalone '' or "" is a single empty-string token.

            7. ERROR:
               An unmatched ' or " that is never closed → throw ShellSplitter.SyntaxException.
            """)
    @Contract(
        requires = "input != null",
        throws_  = {ShellSplitter.SyntaxException.class}
    )

    // ── Basic splitting ────────────────────────────────────────────────────────
    @Example(label = "empty input",          input = "\"\"",             output = "T(java.util.List).of()")
    @Example(label = "whitespace only",      input = "\"   \"",          output = "T(java.util.List).of()")
    @Example(label = "single word",          input = "\"hello\"",        output = "T(java.util.List).of('hello')")
    @Example(label = "two words",            input = "\"hello world\"",  output = "T(java.util.List).of('hello', 'world')")
    @Example(label = "collapse spaces",      input = "\"  a   b  \"",    output = "T(java.util.List).of('a', 'b')")
    @Example(label = "tab separator",        input = "\"a\\tb\"",        output = "T(java.util.List).of('a', 'b')")

    // ── Single quotes ──────────────────────────────────────────────────────────
    @Example(label = "sq preserves space",   input = "\"'hello world'\"", output = "T(java.util.List).of('hello world')")
    @Example(label = "sq empty token",       input = "\"''\"",            output = "T(java.util.List).of('')")
    @Example(label = "sq backslash literal", input = "\"'\\\\n'\"",       output = "T(java.util.List).of('\\\\n')")
    @Example(label = "sq dollar literal",    input = "\"'$HOME'\"",       output = "T(java.util.List).of('$HOME')")

    // ── Double quotes ──────────────────────────────────────────────────────────
    @Example(label = "dq preserves space",     input = "\"\\\"hello world\\\"\"",  output = "T(java.util.List).of('hello world')")
    @Example(label = "dq empty token",         input = "\"\\\"\\\"\"",             output = "T(java.util.List).of('')")
    @Example(label = "dq escape quote",        input = "\"\\\"say \\\\\\\"hi\\\\\\\"\\\"\"",  output = "T(java.util.List).of('say \\\"hi\\\"')")
    @Example(label = "dq escape backslash",    input = "\"\\\"a\\\\\\\\b\\\"\"",   output = "T(java.util.List).of('a\\\\b')")
    @Example(label = "dq non-special backslash", input = "\"\\\"a\\\\nb\\\"\"",    output = "T(java.util.List).of('a\\\\nb')")
    @Example(label = "dq line continuation",   input = "\"\\\"hello\\\\\\nworld\\\"\"", output = "T(java.util.List).of('helloworld')")

    // ── Backslash outside quotes ───────────────────────────────────────────────
    @Example(label = "backslash-space = join",   input = "\"hello\\\\ world\"",  output = "T(java.util.List).of('hello world')")
    @Example(label = "backslash-backslash",      input = "\"a\\\\\\\\b\"",       output = "T(java.util.List).of('a\\\\b')")
    @Example(label = "backslash-letter",         input = "\"he\\\\llo\"",         output = "T(java.util.List).of('hello')")
    @Example(label = "backslash-newline cont",   input = "\"hello\\\\\\nworld\"", output = "T(java.util.List).of('helloworld')")

    // ── Concatenation ──────────────────────────────────────────────────────────
    @Example(label = "dq+unquoted concat",  input = "\"\\\"hello\\\"world\"",   output = "T(java.util.List).of('helloworld')")
    @Example(label = "sq+dq concat",        input = "\"'foo'\\\"bar\\\"\"",     output = "T(java.util.List).of('foobar')")
    @Example(label = "unquoted+dq+unquoted",input = "\"a\\\"b c\\\"d\"",        output = "T(java.util.List).of('ab cd')")
    @Example(label = "empty-sq in middle",  input = "\"foo''bar\"",             output = "T(java.util.List).of('foobar')")

    // ── Empty token cases ──────────────────────────────────────────────────────
    @Example(label = "empty token between words", input = "\"foo '' bar\"",  output = "T(java.util.List).of('foo', '', 'bar')")
    @Example(label = "two empty tokens",          input = "\"'' ''\"",        output = "T(java.util.List).of('', '')")

    // ── Error cases ────────────────────────────────────────────────────────────
    @Example(label = "unclosed single-quote", input = "\"'hello\"",       throws_ = ShellSplitter.SyntaxException.class)
    @Example(label = "unclosed double-quote", input = "\"\\\"hello\"",    throws_ = ShellSplitter.SyntaxException.class)

    @Stub("""
            Implement a character-by-character state machine with 3 states:
              NORMAL, IN_SINGLE_QUOTE, IN_DOUBLE_QUOTE.

            Use a StringBuilder for the current token and a List<String> for results.
            In NORMAL state, whitespace flushes the current token (if any) to results.
            Track whether the current word is "active" (has at least one character,
            even if that character came from an empty quoted string) to correctly
            emit empty tokens from '' or "".

            The most common one-shot mistakes:
              1. Treating \\n as a newline inside double quotes (it is NOT — only \\$, \\",
                 \\\\, \\` and \\<newline> are special in double-quotes; \\n is two chars).
              2. Forgetting that '' and "" produce an EMPTY token, not nothing.
              3. Treating backslash-space outside quotes as a word separator
                 (it is NOT — it escapes the space, joining the two sides).
              4. Forgetting line continuation (\\<newline>) both inside double-quotes
                 AND outside quotes.
            """)
    @Override
    public List<String> split(String input) {
        throw new UnsupportedOperationException();
    }
}
