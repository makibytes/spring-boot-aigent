package de.makibytes.benchmark.toml;

import java.util.Map;

/**
 * Parses a subset of TOML v1.0 into a {@code Map<String, Object>} document.
 */
public interface TomlParser {

    /**
     * Parse {@code toml} and return the document as a nested map.
     *
     * <p>Value types map to Java types as follows:
     * <ul>
     *   <li>TOML Integer  → {@code Long}
     *   <li>TOML Float    → {@code Double}
     *   <li>TOML Boolean  → {@code Boolean}
     *   <li>TOML String   → {@code String}
     *   <li>TOML Array    → {@code List<Object>}
     *   <li>TOML Table    → {@code Map<String, Object>}
     * </ul>
     *
     * @throws ParseException on any syntax or semantic error
     */
    Map<String, Object> parse(String toml);

    /** Thrown for syntax errors or semantic violations (e.g. duplicate keys). */
    class ParseException extends RuntimeException {
        public ParseException(String message) { super(message); }
        public ParseException(String message, Throwable cause) { super(message, cause); }
    }
}
