package de.makibytes.benchmark.toml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for TomlParser.
 *
 * Supported TOML v1.0 subset:
 *   - Basic strings, literal strings, multiline basic/literal strings
 *   - Integers: decimal, hex (0x), octal (0o), binary (0b), underscore separators
 *   - Floats: including inf, -inf, +inf, nan
 *   - Booleans: true / false (lowercase only)
 *   - Arrays (mixed types allowed per TOML 1.0)
 *   - Inline tables: {key = val, ...}
 *   - Standard tables: [key]
 *   - Dotted keys: a.b = val
 *   - Array of tables: [[key]]
 *   - Comments: # to end of line
 *   - Duplicate key detection (ParseException)
 *
 * The implementation must be named TomlParserImpl in this package.
 */
@SuppressWarnings("unchecked")
class TomlParserTest {

    private TomlParser parser;

    @BeforeEach
    void setUp() throws Exception {
        parser = (TomlParser) Class
                .forName("de.makibytes.benchmark.toml.TomlParserImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private Map<String, Object> parse(String toml) {
        return parser.parse(toml);
    }

    // ── Strings ───────────────────────────────────────────────────────────────

    @Test void basicString() {
        assertThat(parse("key = \"hello\"")).containsEntry("key", "hello");
    }

    @Test void literalString() {
        assertThat(parse("key = 'hello'")).containsEntry("key", "hello");
    }

    /** Escape sequences in basic strings. */
    @Test void stringEscapeNewline() {
        assertThat(parse("key = \"line1\\nline2\""))
                .containsEntry("key", "line1\nline2");
    }

    @Test void stringEscapeTab() {
        assertThat(parse("key = \"col1\\tcol2\""))
                .containsEntry("key", "col1\tcol2");
    }

    @Test void stringEscapeQuote() {
        assertThat(parse("key = \"say \\\"hi\\\"\""))
                .containsEntry("key", "say \"hi\"");
    }

    @Test void stringEscapeBackslash() {
        assertThat(parse("key = \"a\\\\b\"")).containsEntry("key", "a\\b");
    }

    /**
     * CRITICAL: literal strings use single quotes and perform NO escape processing.
     * Backslashes are literal — useful for Windows paths.
     */
    @Test void literalStringNoEscaping() {
        assertThat(parse("key = 'C:\\Users\\alice'"))
                .containsEntry("key", "C:\\Users\\alice");
    }

    /**
     * CRITICAL: literal strings preserve '#' as a regular character — it does NOT
     * start a comment inside a literal string.
     */
    @Test void literalStringHash() {
        assertThat(parse("key = '# not a comment'"))
                .containsEntry("key", "# not a comment");
    }

    // ── Multiline basic strings ───────────────────────────────────────────────

    /**
     * CRITICAL: the first newline immediately after the opening \"\"\" is stripped.
     * All subsequent content is kept as-is.
     */
    @Test void multilineBasicFirstNewlineStripped() {
        assertThat(parse("key = \"\"\"\nhello\"\"\"")).containsEntry("key", "hello");
    }

    /** Escape sequences still work inside multiline basic strings. */
    @Test void multilineBasicEscapesWork() {
        assertThat(parse("key = \"\"\"\nhello\\tworld\"\"\""))
                .containsEntry("key", "hello\tworld");
    }

    /**
     * CRITICAL: a backslash followed by a newline (and optional whitespace) inside
     * a multiline basic string is a line continuation — it removes the backslash,
     * the newline, and any leading whitespace on the next line.
     */
    @Test void multilineBasicLineContinuation() {
        // key = """The quick brown \
        //    fox"""  →  "The quick brown fox"
        String toml = "key = \"\"\"The quick brown \\\n   fox\"\"\"";
        assertThat(parse(toml)).containsEntry("key", "The quick brown fox");
    }

    // ── Multiline literal strings ─────────────────────────────────────────────

    /** First newline is stripped; no escape processing. */
    @Test void multilineLiteralFirstNewlineStripped() {
        assertThat(parse("key = '''\nhello'''")).containsEntry("key", "hello");
    }

    /**
     * CRITICAL: backslash inside multiline literal strings is LITERAL — no escaping.
     */
    @Test void multilineLiteralNoEscaping() {
        assertThat(parse("key = '''\nC:\\path'''")).containsEntry("key", "C:\\path");
    }

    // ── Integers ─────────────────────────────────────────────────────────────

    @Test void integerDecimal()  { assertThat(parse("n = 42")).containsEntry("n", 42L); }
    @Test void integerNegative() { assertThat(parse("n = -42")).containsEntry("n", -42L); }
    @Test void integerPositive() { assertThat(parse("n = +42")).containsEntry("n", 42L); }
    @Test void integerZero()     { assertThat(parse("n = 0")).containsEntry("n", 0L); }

    /**
     * CRITICAL: underscores may appear between digits as visual separators.
     * They are ignored in the numeric value.
     */
    @Test void integerUnderscores() {
        assertThat(parse("n = 1_000_000")).containsEntry("n", 1_000_000L);
    }

    /** 0x prefix for hexadecimal integers. */
    @Test void integerHex() { assertThat(parse("n = 0xFF")).containsEntry("n", 255L); }

    /** TOML hex allows uppercase and lowercase digits. */
    @Test void integerHexLowercase() { assertThat(parse("n = 0xff")).containsEntry("n", 255L); }

    /** 0o prefix for octal integers. */
    @Test void integerOctal() { assertThat(parse("n = 0o77")).containsEntry("n", 63L); }

    /** 0b prefix for binary integers. */
    @Test void integerBinary() { assertThat(parse("n = 0b1010")).containsEntry("n", 10L); }

    // ── Floats ────────────────────────────────────────────────────────────────

    @Test void floatDecimal() { assertThat(parse("f = 3.14")).containsEntry("f", 3.14); }
    @Test void floatExponent() { assertThat(parse("f = 6.626e-34")).containsEntry("f", 6.626e-34); }

    /** CRITICAL: inf, +inf, -inf, nan are special float literals. */
    @Test void floatPositiveInf() {
        assertThat(parse("f = inf")).containsEntry("f", Double.POSITIVE_INFINITY);
    }

    @Test void floatExplicitPositiveInf() {
        assertThat(parse("f = +inf")).containsEntry("f", Double.POSITIVE_INFINITY);
    }

    @Test void floatNegativeInf() {
        assertThat(parse("f = -inf")).containsEntry("f", Double.NEGATIVE_INFINITY);
    }

    @Test void floatNan() {
        assertThat(parse("f = nan"))
                .hasEntrySatisfying("f", v -> assertThat((Double) v).isNaN());
    }

    // ── Booleans ─────────────────────────────────────────────────────────────

    @Test void booleanTrue()  { assertThat(parse("b = true")).containsEntry("b", true); }
    @Test void booleanFalse() { assertThat(parse("b = false")).containsEntry("b", false); }

    /**
     * CRITICAL: TOML booleans are CASE-SENSITIVE. "True" or "TRUE" are NOT valid
     * boolean literals — they are parse errors.
     */
    @Test void booleanCapitalInvalid() {
        assertThatThrownBy(() -> parse("b = True"))
                .isInstanceOf(TomlParser.ParseException.class);
    }

    // ── Arrays ────────────────────────────────────────────────────────────────

    @Test void arrayOfIntegers() {
        var result = parse("arr = [1, 2, 3]");
        assertThat((List<Object>) result.get("arr")).containsExactly(1L, 2L, 3L);
    }

    @Test void arrayOfStrings() {
        var result = parse("arr = [\"a\", \"b\"]");
        assertThat((List<Object>) result.get("arr")).containsExactly("a", "b");
    }

    /** TOML 1.0 allows mixed-type arrays (removed the restriction from earlier versions). */
    @Test void mixedTypeArray() {
        var result = parse("arr = [1, \"two\", true]");
        assertThat((List<Object>) result.get("arr")).containsExactly(1L, "two", true);
    }

    @Test void emptyArray() {
        var result = parse("arr = []");
        assertThat((List<Object>) result.get("arr")).isEmpty();
    }

    @Test void nestedArray() {
        var result = parse("arr = [[1, 2], [3, 4]]");
        var outer = (List<Object>) result.get("arr");
        assertThat((List<Object>) outer.get(0)).containsExactly(1L, 2L);
        assertThat((List<Object>) outer.get(1)).containsExactly(3L, 4L);
    }

    // ── Inline tables ─────────────────────────────────────────────────────────

    @Test void inlineTable() {
        var result = parse("point = {x = 1, y = 2}");
        var point = (Map<String, Object>) result.get("point");
        assertThat(point).containsEntry("x", 1L).containsEntry("y", 2L);
    }

    @Test void nestedInlineTable() {
        var result = parse("a = {b = {c = 42}}");
        var a = (Map<String, Object>) result.get("a");
        var b = (Map<String, Object>) a.get("b");
        assertThat(b).containsEntry("c", 42L);
    }

    // ── Dotted keys ───────────────────────────────────────────────────────────

    /**
     * CRITICAL: dotted keys like a.b = 1 create nested maps.
     * They must not overwrite an existing table created by [a].
     */
    @Test void dottedKey() {
        var result = parse("a.b = 1");
        var a = (Map<String, Object>) result.get("a");
        assertThat(a).containsEntry("b", 1L);
    }

    @Test void deepDottedKey() {
        var result = parse("a.b.c = 42");
        var a = (Map<String, Object>) result.get("a");
        var b = (Map<String, Object>) a.get("b");
        assertThat(b).containsEntry("c", 42L);
    }

    // ── Standard tables ───────────────────────────────────────────────────────

    @Test void simpleTable() {
        var result = parse("[server]\nhost = \"localhost\"\nport = 8080");
        var server = (Map<String, Object>) result.get("server");
        assertThat(server).containsEntry("host", "localhost").containsEntry("port", 8080L);
    }

    @Test void multipleTables() {
        var result = parse("[a]\nx = 1\n[b]\nx = 2");
        assertThat(((Map<String, Object>) result.get("a"))).containsEntry("x", 1L);
        assertThat(((Map<String, Object>) result.get("b"))).containsEntry("x", 2L);
    }

    @Test void dottedTable() {
        var result = parse("[a.b]\nc = 42");
        var a = (Map<String, Object>) result.get("a");
        var b = (Map<String, Object>) a.get("b");
        assertThat(b).containsEntry("c", 42L);
    }

    // ── Array of tables ───────────────────────────────────────────────────────

    @Test void arrayOfTables() {
        var result = parse("[[fruits]]\nname = \"apple\"\n[[fruits]]\nname = \"banana\"");
        var fruits = (List<Map<String, Object>>) result.get("fruits");
        assertThat(fruits).hasSize(2);
        assertThat(fruits.get(0)).containsEntry("name", "apple");
        assertThat(fruits.get(1)).containsEntry("name", "banana");
    }

    // ── Comments ─────────────────────────────────────────────────────────────

    @Test void inlineComment() {
        assertThat(parse("key = \"value\" # ignored")).containsEntry("key", "value");
    }

    @Test void commentLine() {
        assertThat(parse("# whole line is comment\nkey = 42")).containsEntry("key", 42L);
    }

    // ── Duplicate key detection ───────────────────────────────────────────────

    /**
     * CRITICAL: redefining an already-defined key is a TOML error.
     */
    @Test void duplicateKeyThrows() {
        assertThatThrownBy(() -> parse("key = 1\nkey = 2"))
                .isInstanceOf(TomlParser.ParseException.class);
    }

    @Test void duplicateTableThrows() {
        assertThatThrownBy(() -> parse("[a]\nx = 1\n[a]\ny = 2"))
                .isInstanceOf(TomlParser.ParseException.class);
    }
}
