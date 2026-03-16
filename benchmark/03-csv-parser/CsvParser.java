package de.makibytes.benchmark.csv;

import java.util.List;

/**
 * Parses CSV text following RFC 4180.
 * See https://www.rfc-editor.org/rfc/rfc4180
 */
public interface CsvParser {
    /**
     * Parses a CSV string into a list of rows, each row a list of field strings.
     * Fields are never trimmed. Quoted fields strip the surrounding quotes and
     * unescape doubled double-quotes. Both LF and CRLF are accepted as row delimiters.
     *
     * @param csv the CSV text to parse (never null)
     * @return list of rows; empty list if input is blank
     */
    List<List<String>> parse(String csv);
}
