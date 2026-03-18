package de.makibytes.benchmark.shell;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for ShellSplitter.
 *
 * Rules under test (simplified POSIX word-splitting + quote-removal):
 *   • Unquoted: whitespace (space/tab) splits tokens; multiple = one delimiter.
 *     Leading/trailing whitespace produces no empty tokens.
 *   • Single quotes 'x': every character between the delimiters is literal —
 *     no escape sequences, no way to include a single-quote inside.
 *   • Double quotes "x": only \" \\ \$ \`  and \<newline> are special;
 *     all other \X sequences are kept as-is (backslash preserved).
 *   • Backslash outside quotes: \<newline> = line continuation (both removed);
 *     \X for any other X = just X (backslash consumed).
 *   • Adjacent quoted/unquoted/other-quoted segments form ONE token.
 *   • '' and "" each contribute an empty string token when they stand alone
 *     or are concatenated with nothing else.
 *   • Unmatched ' or " → ShellSplitter.SyntaxException.
 *
 * The implementation must be named ShellSplitterImpl in this package.
 */
class ShellSplitterTest {

    private ShellSplitter splitter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        splitter = (ShellSplitter) Class
                .forName("de.makibytes.benchmark.shell.ShellSplitterImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private List<String> split(String input) {
        return splitter.split(input);
    }

    // ── Basic whitespace handling ──────────────────────────────────────────────

    @Test void emptyInput()     { assertThat(split("")).isEmpty(); }
    @Test void whitespaceOnly() { assertThat(split("   ")).isEmpty(); }
    @Test void tabOnly()        { assertThat(split("\t")).isEmpty(); }
    @Test void singleWord()     { assertThat(split("hello")).containsExactly("hello"); }
    @Test void twoWords()       { assertThat(split("hello world")).containsExactly("hello", "world"); }

    /** Multiple adjacent spaces collapse to a single delimiter. */
    @Test void collapseSpaces()  { assertThat(split("  hello   world  ")).containsExactly("hello", "world"); }

    /** Tab also separates words. */
    @Test void tabSeparates()   { assertThat(split("hello\tworld")).containsExactly("hello", "world"); }

    @Test void threeWords()     { assertThat(split("a b c")).containsExactly("a", "b", "c"); }

    // ── Single quotes ─────────────────────────────────────────────────────────

    /** Space inside single quotes is part of the token — NOT a word boundary. */
    @Test void singleQuotedSpace() {
        assertThat(split("'hello world'")).containsExactly("hello world");
    }

    /** Empty single-quoted string becomes an empty token. */
    @Test void singleQuotedEmpty() {
        assertThat(split("''")).containsExactly("");
    }

    /**
     * CRITICAL: backslash inside single quotes is LITERAL — no escaping.
     * '\n' → two-char string: backslash + n (NOT a newline).
     */
    @Test void singleQuoteBackslashLiteral() {
        // Java "\\n" is the 2-char string: backslash, n
        assertThat(split("'\\n'")).containsExactly("\\n");
    }

    /**
     * CRITICAL: dollar sign inside single quotes is literal — no expansion.
     * '$HOME' → the literal string $HOME.
     */
    @Test void singleQuoteDollarLiteral() {
        assertThat(split("'$HOME'")).containsExactly("$HOME");
    }

    /** Two single-quoted words separated by space → two tokens. */
    @Test void twoSingleQuotedWords() {
        assertThat(split("'hello' 'world'")).containsExactly("hello", "world");
    }

    // ── Double quotes ─────────────────────────────────────────────────────────

    /** Space inside double quotes is part of the token. */
    @Test void doubleQuotedSpace() {
        assertThat(split("\"hello world\"")).containsExactly("hello world");
    }

    /** Empty double-quoted string becomes an empty token. */
    @Test void doubleQuotedEmpty() {
        assertThat(split("\"\"")).containsExactly("");
    }

    /** \" inside double quotes → literal double-quote character. */
    @Test void doubleQuoteEscapesQuote() {
        // shell input: "say \"hi\""  →  say "hi"
        assertThat(split("\"say \\\"hi\\\"\"")).containsExactly("say \"hi\"");
    }

    /** \\ inside double quotes → single backslash. */
    @Test void doubleQuoteEscapesBackslash() {
        // shell input: "a\\b"  →  a\b
        assertThat(split("\"a\\\\b\"")).containsExactly("a\\b");
    }

    /**
     * CRITICAL: \n inside double quotes is NOT a special escape (only \\ \" \$ \` and
     * \<newline> are special). The backslash is preserved: "a\nb" → a\nb (backslash + n).
     */
    @Test void doubleQuoteNonSpecialBackslash() {
        // shell input: "a\nb" (6 chars: " a \ n b ")  →  a\nb (4 chars: a \ n b)
        assertThat(split("\"a\\nb\"")).containsExactly("a\\nb");
    }

    /** Single quote inside double quotes is literal — no quoting effect. */
    @Test void doubleQuoteSingleQuoteLiteral() {
        assertThat(split("\"it's\"")).containsExactly("it's");
    }

    /** \$ inside double quotes → literal dollar sign. */
    @Test void doubleQuoteEscapesDollar() {
        assertThat(split("\"\\$HOME\"")).containsExactly("$HOME");
    }

    /**
     * CRITICAL: \<newline> inside double quotes = line continuation — BOTH are removed.
     * "hello\<newline>world" → helloworld (one token, no newline).
     */
    @Test void doubleQuoteLineContinuation() {
        assertThat(split("\"hello\\\nworld\"")).containsExactly("helloworld");
    }

    // ── Backslash outside quotes ───────────────────────────────────────────────

    /**
     * Backslash-space outside quotes: the space is escaped and becomes part of the
     * current token — it does NOT split. hello\ world → one token "hello world".
     */
    @Test void backslashEscapesSpace() {
        assertThat(split("hello\\ world")).containsExactly("hello world");
    }

    /** \\ outside quotes → single backslash. */
    @Test void backslashEscapesBackslash() {
        assertThat(split("a\\\\b")).containsExactly("a\\b");
    }

    /** \l outside quotes → l (backslash consumed, letter kept). */
    @Test void backslashEscapesLetter() {
        assertThat(split("he\\llo")).containsExactly("hello");
    }

    /**
     * CRITICAL: backslash-newline outside quotes = line continuation.
     * hello\<newline>world → helloworld (one token, continuation removed).
     */
    @Test void backslashNewlineContinuation() {
        assertThat(split("hello\\\nworld")).containsExactly("helloworld");
    }

    // ── Concatenation ─────────────────────────────────────────────────────────

    /**
     * CRITICAL: adjacent quoted and unquoted segments merge into ONE token.
     * "hello"world → helloworld (no separator between them).
     */
    @Test void doubleQuotedConcatenation() {
        assertThat(split("\"hello\"world")).containsExactly("helloworld");
    }

    /**
     * CRITICAL: single-quoted + double-quoted side-by-side = one token.
     * 'foo'"bar" → foobar.
     */
    @Test void mixedQuotesConcatenation() {
        assertThat(split("'foo'\"bar\"")).containsExactly("foobar");
    }

    /** unquoted + quoted + unquoted → one token. */
    @Test void unquotedQuotedUnquoted() {
        // a"b c"d  → ab cd  (one token)
        assertThat(split("a\"b c\"d")).containsExactly("ab cd");
    }

    /** Empty single-quoted in the middle still joins the adjacent characters. */
    @Test void emptyQuotedInMiddle() {
        assertThat(split("foo''bar")).containsExactly("foobar");
    }

    // ── Empty tokens from standalone empty quotes ──────────────────────────────

    /** Empty quoted string between two words → three tokens: word, empty, word. */
    @Test void emptyTokenBetweenWords() {
        assertThat(split("foo '' bar")).containsExactly("foo", "", "bar");
    }

    /** Two empty quoted strings → two empty tokens. */
    @Test void twoEmptyTokens() {
        assertThat(split("'' ''")).containsExactly("", "");
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test void unclosedSingleQuote() {
        assertThatThrownBy(() -> split("'hello"))
                .isInstanceOf(ShellSplitter.SyntaxException.class);
    }

    @Test void unclosedDoubleQuote() {
        assertThatThrownBy(() -> split("\"hello"))
                .isInstanceOf(ShellSplitter.SyntaxException.class);
    }

    @Test void unclosedQuoteAfterWords() {
        assertThatThrownBy(() -> split("hello 'world"))
                .isInstanceOf(ShellSplitter.SyntaxException.class);
    }
}
