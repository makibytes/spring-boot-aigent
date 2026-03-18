package de.makibytes.benchmark.toml;

import de.makibytes.aigent.*;
import java.util.Map;

/**
 * Aigent spec for TomlParserImpl.
 * Implement the @Stub method. All rules are tested by TomlParserTest.
 */
public class TomlParserImpl implements TomlParser {

    @Intent("""
            Parse a subset of TOML v1.0 and return the document as Map<String, Object>.

            VALUE TYPES:
              Integer  → Long     (decimal, hex 0x, octal 0o, binary 0b; _ separators allowed)
              Float    → Double   (standard decimal, exponent; also inf, +inf, -inf, nan)
              Boolean  → Boolean  (true / false — lowercase ONLY; True or TRUE is a parse error)
              String   → String   (basic "...", literal '...', multiline versions)
              Array    → List<Object>  (mixed types allowed — TOML 1.0 removed type restriction)
              Table    → Map<String, Object>

            STRING TYPES:

            1. Basic string "...":
               Escape sequences: \\" \\\\ \\n \\t \\r \\uXXXX \\UXXXXXXXX
               No other backslash sequences are valid.

            2. Literal string '...':
               Everything between ' delimiters is literal — NO escape processing.
               Backslash is a regular character. Hash is a regular character.

            3. Multiline basic string \\"\\"\\"...\\"\\"\\"\\":
               The FIRST newline immediately after the opening delimiter is stripped.
               Remaining content uses the same escape sequences as basic strings.
               A backslash at the end of a line (before the newline) is a line
               continuation: removes the backslash, the newline, and all leading
               whitespace on the next line.

            4. Multiline literal string '''...''':
               The FIRST newline immediately after the opening delimiter is stripped.
               NO escape processing — all content is literal.

            INTEGER LITERALS:
              +99      → 99L  (explicit positive sign)
              -99      → -99L
              1_000    → 1000L (underscores between digits as visual separators)
              0xFF     → 255L  (hexadecimal; 0x prefix, digits 0-9 A-F a-f, _ allowed)
              0o77     → 63L   (octal; 0o prefix, digits 0-7, _ allowed)
              0b1010   → 10L   (binary; 0b prefix, digits 0-1, _ allowed)

            FLOAT LITERALS:
              inf      → Double.POSITIVE_INFINITY
              +inf     → Double.POSITIVE_INFINITY
              -inf     → Double.NEGATIVE_INFINITY
              nan      → Double.NaN
              Standard: 3.14, 6.626e-34, 1_000.5 (underscores allowed)

            BOOLEAN LITERALS:
              true     → Boolean.TRUE   (lowercase only!)
              false    → Boolean.FALSE  (lowercase only!)
              True or TRUE → ParseException

            KEYS:
              Bare key: [a-zA-Z0-9_-]+
              Quoted key: "..." (basic string rules) or '...' (literal string rules)
              Dotted key: a.b.c = val → creates nested maps

            TABLES:
              [key]        → define table 'key', all following k=v pairs go into it
              [a.b]        → dotted table; creates a.b as a nested map
              [[key]]      → array-of-tables; appends a new table to the array 'key'

            INLINE TABLE:
              {a = 1, b = 2}  → Map {a→1, b→2}
              No newlines allowed inside. No trailing comma. Immutable after definition.

            COMMENTS:
              # from hash to end-of-line is ignored (but # inside strings is literal).

            DUPLICATE KEY:
              Redefining any key (including via dotted keys or tables) throws ParseException.
            """)
    @Contract(
        requires = "toml != null",
        throws_  = {TomlParser.ParseException.class}
    )

    // ── Strings ────────────────────────────────────────────────────────────────
    @Example(label = "basic string",          input = "\"key = \\\"hello\\\"\"",   output = "{'key': 'hello'}")
    @Example(label = "literal string",        input = "\"key = 'hello'\"",          output = "{'key': 'hello'}")
    @Example(label = "escape newline",        input = "\"key = \\\"line1\\\\nline2\\\"\"", output = "{'key': 'line1\\nline2'}")
    @Example(label = "escape backslash",      input = "\"key = \\\"a\\\\\\\\b\\\"\"", output = "{'key': 'a\\\\b'}")
    @Example(label = "literal no-escape",     input = "\"key = 'C:\\\\path'\"",     output = "{'key': 'C:\\\\path'}")
    @Example(label = "literal hash literal",  input = "\"key = '# not a comment'\"", output = "{'key': '# not a comment'}")

    // ── Multiline strings ──────────────────────────────────────────────────────
    @Example(label = "ml-basic first-newline stripped", input = "\"key = \\\"\\\"\\\"\\nhello\\\"\\\"\\\"\"", output = "{'key': 'hello'}")
    @Example(label = "ml-basic line continuation",      input = "\"key = \\\"\\\"\\\"foo \\\\\\n   bar\\\"\\\"\\\"\"", output = "{'key': 'foo bar'}")
    @Example(label = "ml-literal first-newline stripped", input = "\"key = '''\\nhello'''\"", output = "{'key': 'hello'}")
    @Example(label = "ml-literal no escaping",            input = "\"key = '''\\nC:\\\\path'''\"", output = "{'key': 'C:\\\\path'}")

    // ── Integer types ──────────────────────────────────────────────────────────
    @Example(label = "integer decimal",    input = "\"n = 42\"",          output = "{'n': 42}")
    @Example(label = "integer positive",   input = "\"n = +42\"",         output = "{'n': 42}")
    @Example(label = "integer negative",   input = "\"n = -42\"",         output = "{'n': -42}")
    @Example(label = "integer underscores",input = "\"n = 1_000_000\"",   output = "{'n': 1000000}")
    @Example(label = "integer hex",        input = "\"n = 0xFF\"",         output = "{'n': 255}")
    @Example(label = "integer octal",      input = "\"n = 0o77\"",         output = "{'n': 63}")
    @Example(label = "integer binary",     input = "\"n = 0b1010\"",       output = "{'n': 10}")

    // ── Float types ────────────────────────────────────────────────────────────
    @Example(label = "float decimal",       input = "\"f = 3.14\"",   output = "{'f': 3.14}")
    @Example(label = "float inf",           input = "\"f = inf\"",    output = "{'f': Infinity}")
    @Example(label = "float -inf",          input = "\"f = -inf\"",   output = "{'f': -Infinity}")
    @Example(label = "float nan",           input = "\"f = nan\"",    output = "{'f': NaN}")

    // ── Booleans ───────────────────────────────────────────────────────────────
    @Example(label = "boolean true",        input = "\"b = true\"",   output = "{'b': true}")
    @Example(label = "boolean false",       input = "\"b = false\"",  output = "{'b': false}")
    @Example(label = "boolean capital error",input = "\"b = True\"",  throws_ = TomlParser.ParseException.class)

    // ── Arrays ─────────────────────────────────────────────────────────────────
    @Example(label = "array of ints",  input = "\"arr = [1, 2, 3]\"",       output = "{'arr': [1, 2, 3]}")
    @Example(label = "mixed array",    input = "\"arr = [1, \\\"two\\\", true]\"", output = "{'arr': [1, two, true]}")
    @Example(label = "empty array",    input = "\"arr = []\"",               output = "{'arr': []}")

    // ── Dotted keys ────────────────────────────────────────────────────────────
    @Example(label = "dotted key",     input = "\"a.b = 1\"",   output = "{'a': {'b': 1}}")
    @Example(label = "deep dotted",    input = "\"a.b.c = 42\"",output = "{'a': {'b': {'c': 42}}}")

    // ── Tables ─────────────────────────────────────────────────────────────────
    @Example(label = "simple table",   input = "\"[server]\\nhost = \\\"localhost\\\"\"", output = "{'server': {'host': 'localhost'}}")
    @Example(label = "dotted table",   input = "\"[a.b]\\nc = 1\"",           output = "{'a': {'b': {'c': 1}}}")

    // ── Array of tables ────────────────────────────────────────────────────────
    @Example(label = "array of tables",
             input = "\"[[fruits]]\\nname = \\\"apple\\\"\\n[[fruits]]\\nname = \\\"banana\\\"\"",
             output = "{'fruits': [{name=apple}, {name=banana}]}")

    // ── Comments ───────────────────────────────────────────────────────────────
    @Example(label = "inline comment",  input = "\"key = 42 # ignored\"", output = "{'key': 42}")

    // ── Errors ─────────────────────────────────────────────────────────────────
    @Example(label = "duplicate key",   input = "\"key = 1\\nkey = 2\"", throws_ = TomlParser.ParseException.class)

    @Stub("""
            Implement a line-by-line parser with these phases:

            1. LEXING: scan character by character, handling:
               - Comments: skip from # to end-of-line (except inside strings)
               - String literals: detect opening " or ' (or triple versions)
               - All other tokens

            2. DOCUMENT STATE: maintain
               - root map (the document)
               - currentTable: the Map being currently populated (starts at root)
               - definedTables: Set<String> to detect duplicates
               - arrayOfTablesCounts: Map<String, Integer>

            3. VALUE PARSING: parse_value() → Object
               - Try in order: multiline string, basic string, literal string,
                 integer (0x/0o/0b/+/-/digit), float (inf/nan/digit+dot/exponent),
                 boolean (true/false), array, inline table

            4. KEY WRITING: write to nested maps following dotted key path.
               For each segment except the last: get-or-create the nested Map.
               Before writing the final key, check for duplicates.

            5. FLOAT SPECIAL VALUES: parse before integers — "inf" and "nan" are NOT
               bare keys because they appear only as values. Check for these before
               attempting integer parsing.

            6. COMMON MISTAKES ONE-SHOTS MAKE:
               a. Missing underscore separator support in integers/floats
               b. Missing 0x/0o/0b integer support
               c. Not stripping the first newline in multiline strings
               d. Not implementing line continuation (\\<newline>) in multiline basic strings
               e. Treating "True"/"TRUE" as valid booleans (they are parse errors)
               f. Not detecting duplicate keys across dotted-key style and table style
               g. Not supporting mixed-type arrays (TOML 1.0 allows them)
               h. Parsing "inf"/"nan" as bare key names instead of float values
            """)
    @Override
    public Map<String, Object> parse(String toml) {
        throw new UnsupportedOperationException();
    }
}
