package de.makibytes.benchmark.shell;

import java.util.List;

/**
 * Splits a shell command-line string into tokens following simplified POSIX
 * word-splitting and quote-removal rules.
 */
public interface ShellSplitter {

    /**
     * Split {@code input} into a list of shell tokens applying POSIX quoting rules.
     *
     * @throws SyntaxException if the input contains an unmatched quote
     */
    List<String> split(String input);

    /** Thrown when a quote is opened but never closed. */
    class SyntaxException extends RuntimeException {
        public SyntaxException(String message) { super(message); }
    }
}
