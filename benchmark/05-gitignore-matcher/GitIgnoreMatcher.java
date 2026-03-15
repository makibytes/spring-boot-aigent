package com.aigent.benchmark.gitignore;

import java.util.List;

/**
 * Evaluates a simplified subset of gitignore-style patterns.
 */
public interface GitIgnoreMatcher {
    /**
     * Returns true when the given relative path should be ignored by the pattern list.
     *
     * Supported pattern features in this benchmark:
     * - blank lines and lines starting with # are ignored
     * - ! negates a later match (last matching pattern wins)
     * - leading / anchors the pattern to the repository root
     * - trailing / matches directories and everything underneath them
     * - * matches within a single path segment
     * - ** matches across path segments
     *
     * Paths use / as separators; implementations should also accept incoming \\ characters.
     *
     * @param patterns ordered gitignore-style patterns
     * @param path relative path to check
     * @param directory true when path itself is a directory
     * @return true when ignored, false when included
     */
    boolean shouldIgnore(List<String> patterns, String path, boolean directory);
}
