package com.aigent.benchmark.csv;

import java.util.List;

/**
 * Aigent spec for CsvParserImpl following RFC 4180.
 * Every edge case that typically destroys naive implementations is listed in @Example.
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
        requires = "csv != null",
        ensures  = "$result is a non-null list; fields are never null",
        throws_  = {IllegalArgumentException.class}
    )
    @Example(label = "single field",        input = "\"hello\"",              output = "[[\"hello\"]]")
    @Example(label = "single row",          input = "\"a,b,c\"",              output = "[[\"a\",\"b\",\"c\"]]")
    @Example(label = "multiple rows LF",    input = "\"a,b\\nc,d\"",           output = "[[\"a\",\"b\"],[\"c\",\"d\"]]")
    @Example(label = "CRLF line ending",    input = "\"a,b\\r\\nc,d\"",        output = "[[\"a\",\"b\"],[\"c\",\"d\"]]")
    @Example(label = "empty fields comma",  input = "\",\"",                   output = "[[\"\" ,\"\"]]")
    @Example(label = "empty input",         input = "\"\"",                    output = "[]")
    @Example(label = "quoted comma — CRITICAL", input = "\"\\\"hello, world\\\"\"",     output = "[[\"hello, world\"]]  // one field despite the comma")
    @Example(label = "quoted newline — CRITICAL", input = "\"\\\"line1\\nline2\\\"\"",  output = "[[\"line1\\nline2\"]]  // newline inside quotes = part of field")
    @Example(label = "doubled quote — CRITICAL",  input = "\"\\\"say \\\"\\\"hi\\\"\\\"\\\"\"", output = "[[\"say \\\"hi\\\"\"]]  // \"\" inside quotes → one literal \"")
    @Example(label = "mixed quoted and plain",    input = "\"a,\\\"b,c\\\",d\"",         output = "[[\"a\",\"b,c\",\"d\"]]")
    @Example(label = "spaces not trimmed",        input = "\" a , b \"",                 output = "[[\" a \",\" b \"]]")
    @Property("parse(csv).stream().mapToInt(List::size).sum() == (number of unquoted commas that are field separators)")
    @Property("no field in $result is ever null")
    @Stub("Use a character-by-character state machine or index scanner — NOT split(). " +
          "States: NORMAL (accumulate char), IN_QUOTE (accumulate char, watch for \"\"), " +
          "AFTER_QUOTE (expect comma, \\n, \\r, or end). " +
          "Quoted newlines are part of the field. CRLF outside quotes = row separator. " +
          "Empty input → empty list.")
    @Override
    public List<List<String>> parse(String csv) {
        throw new UnsupportedOperationException();
    }
}
