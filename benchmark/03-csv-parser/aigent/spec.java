package de.makibytes.benchmark.csv;

import de.makibytes.aigent.*;
import java.util.List;

/**
 * Aigent spec for CsvParserImpl following RFC 4180.
 * All annotations are enforced at runtime via SpEL.
 */
public class CsvParserImpl implements CsvParser {

    @Intent("""
            Parse a CSV string following RFC 4180. Return a list of rows; each row is a list
            of field strings (never trimmed — spaces are significant).

            Parsing rules:
            1. Rows are separated by LF (\\n) or CRLF (\\r\\n).
            2. Fields within a row are separated by commas (,).
            3. A field MAY be enclosed in double-quote characters (").
               - A quoted field begins and ends with a double-quote.
               - A literal double-quote inside a quoted field is represented by two
                 consecutive double-quotes ("" → one literal ").
               - A quoted field may contain commas, newlines, and CRLFs — they are
                 part of the field, NOT row/field separators.
            4. Fields that are NOT quoted are taken literally (including spaces).
            5. An empty string input returns an empty list (no rows).
            6. DO NOT use String.split(",") — it is incorrect for quoted fields.
            """)
    @Contract(
        requires = "$csv != null",
        ensures  = "$result != null && $result.stream().noneMatch(row -> row == null || row.stream().anyMatch(f -> f == null))",
        throws_  = {NullPointerException.class}
    )
    @Example(label = "empty input",           input = "''",                              output = "{}")
    @Example(label = "single field",          input = "'hello'",                         output = "{{'hello'}}")
    @Example(label = "single row",            input = "'a,b,c'",                         output = "{{'a','b','c'}}")
    @Example(label = "multiple rows LF",      input = "'a,b\nc,d'",                      output = "{{'a','b'},{'c','d'}}")
    @Example(label = "CRLF line ending",      input = "'a,b\r\nc,d'",                    output = "{{'a','b'},{'c','d'}}")
    @Example(label = "empty fields comma",    input = "','",                             output = "{{'',''}}")
    @Example(label = "quoted comma CRITICAL", input = "'\"hello, world\"'",              output = "{{'hello, world'}}")
    @Example(label = "doubled quote CRITICAL",input = "'\"say \"\"hi\"\"\"'",            output = "{{'say \"hi\"'}}")
    @Example(label = "spaces not trimmed",    input = "' a , b '",                       output = "{{' a ',' b '}}")
    @Example(label = "mixed quoted and plain",input = "'a,\"b,c\",d'",                   output = "{{'a','b,c','d'}}")
    @Property("$result != null")
    @Property("$result.stream().noneMatch(row -> row.stream().anyMatch(f -> f == null))")
    @Stub("Use a character-by-character state machine — NOT split(). " +
          "States: NORMAL (accumulate), IN_QUOTE (accumulate, watch for \"\"), AFTER_QUOTE. " +
          "Quoted newlines are part of the field. CRLF outside quotes = row separator. " +
          "Empty input → empty list.")
    @Override
    public List<List<String>> parse(String csv) {
        throw new UnsupportedOperationException();
    }
}
