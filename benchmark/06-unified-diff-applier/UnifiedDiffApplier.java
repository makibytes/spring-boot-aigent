package de.makibytes.benchmark.diff;

/**
 * Applies a simplified unified diff to a text document.
 */
public interface UnifiedDiffApplier {
    /**
     * Applies the unified diff to the original text.
     *
     * Benchmark assumptions:
     * - both original and diff use \\n line endings only
     * - file header lines starting with --- and +++ may appear and should be ignored
     * - hunk headers use the form @@ -oldStart,oldCount +newStart,newCount @@
     * - hunk body lines begin with space (context), - (deletion), or + (addition)
     *
     * @param original original text, possibly empty
     * @param unifiedDiff unified diff text, possibly empty
     * @return patched text
     * @throws NullPointerException if either input is null
     * @throws IllegalArgumentException if the diff is malformed or does not apply cleanly
     */
    String apply(String original, String unifiedDiff);
}
