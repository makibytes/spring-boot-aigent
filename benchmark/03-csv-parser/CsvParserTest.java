package de.makibytes.benchmark.csv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for CsvParser following RFC 4180.
 * Tests ordered from trivial to the cases that destroy split(",") approaches.
 */
class CsvParserTest {

    private CsvParser parser;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        parser = (CsvParser) Class
                .forName("de.makibytes.benchmark.csv.CsvParserImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    // ── Basic parsing ─────────────────────────────────────────────────────────

    @Test void singleField() {
        assertThat(parser.parse("hello")).containsExactly(List.of("hello"));
    }

    @Test void singleRow() {
        assertThat(parser.parse("a,b,c"))
                .containsExactly(List.of("a", "b", "c"));
    }

    @Test void multipleRows() {
        assertThat(parser.parse("a,b\nc,d"))
                .containsExactly(List.of("a", "b"), List.of("c", "d"));
    }

    @Test void emptyInput() {
        assertThat(parser.parse("")).isEmpty();
    }

    // ── Empty fields ──────────────────────────────────────────────────────────

    @Test void emptyFieldsCommaOnly() {
        assertThat(parser.parse(","))
                .containsExactly(List.of("", ""));
    }

    @Test void emptyFieldsInRow() {
        assertThat(parser.parse("a,,c"))
                .containsExactly(List.of("a", "", "c"));
    }

    @Test void emptyFieldsAtStart() {
        assertThat(parser.parse(",b"))
                .containsExactly(List.of("", "b"));
    }

    // ── CRLF line endings ─────────────────────────────────────────────────────

    @Test void crlfLineEnding() {
        assertThat(parser.parse("a,b\r\nc,d"))
                .containsExactly(List.of("a", "b"), List.of("c", "d"));
    }

    // ── Quoted fields ─────────────────────────────────────────────────────────

    @Test void quotedSimple() {
        assertThat(parser.parse("\"hello\""))
                .containsExactly(List.of("hello"));
    }

    /**
     * CRITICAL: A quoted field containing a comma must be treated as one field.
     * split(",") destroys this completely.
     */
    @Test void quotedFieldWithComma() {
        assertThat(parser.parse("\"hello, world\""))
                .containsExactly(List.of("hello, world"));
    }

    /** Multiple fields, one quoted with a comma. */
    @Test void mixedQuotedAndUnquoted() {
        assertThat(parser.parse("a,\"b,c\",d"))
                .containsExactly(List.of("a", "b,c", "d"));
    }

    /**
     * CRITICAL: A quoted field can contain an embedded newline.
     * The entire quoted span is one field across multiple lines.
     */
    @Test void quotedFieldWithNewline() {
        assertThat(parser.parse("\"line1\nline2\""))
                .containsExactly(List.of("line1\nline2"));
    }

    /** Quoted field with embedded CRLF. */
    @Test void quotedFieldWithCrlf() {
        assertThat(parser.parse("\"line1\r\nline2\""))
                .containsExactly(List.of("line1\r\nline2"));
    }

    /**
     * CRITICAL: A literal double-quote inside a quoted field is represented
     * by doubling it: "" → one literal ".
     * "say ""hello""" → say "hello"
     */
    @Test void doubledQuoteEscape() {
        assertThat(parser.parse("\"say \"\"hello\"\"\""))
                .containsExactly(List.of("say \"hello\""));
    }

    @Test void doubledQuoteEscapeInMultiColumn() {
        assertThat(parser.parse("a,\"it's \"\"great\"\"\",b"))
                .containsExactly(List.of("a", "it's \"great\"", "b"));
    }

    // ── Whitespace is significant ─────────────────────────────────────────────

    @Test void spacesNotTrimmed() {
        assertThat(parser.parse(" a , b "))
                .containsExactly(List.of(" a ", " b "));
    }

    // ── Multi-row with quoted content ─────────────────────────────────────────

    @Test void multiRowWithQuotedCommas() {
        assertThat(parser.parse("\"a,b\",c\n\"d,e\",f"))
                .containsExactly(
                        List.of("a,b", "c"),
                        List.of("d,e", "f")
                );
    }
}
