package com.aigent.benchmark.gitignore;

import de.makibytes.aigent.*;
import java.util.List;

/**
 * Aigent spec for GitIgnoreMatcherImpl.
 */
public class GitIgnoreMatcherImpl implements GitIgnoreMatcher {

    @Intent("""
            Decide whether a relative repository path should be ignored by a simplified gitignore matcher.

            Supported pattern subset for this benchmark:
              - blank lines and lines beginning with # are ignored
              - ! negates a later match; the last matching pattern wins
              - leading / anchors a pattern to the repository root
              - trailing / means the pattern targets directories and everything under them
              - * matches within a single path segment only
              - ** may span across directories
              - incoming paths may use either / or \\ and must be normalised to /

            Matching rules for this benchmark:
              - a pattern without leading / may match at any directory depth
              - a plain pattern like target matches a path segment named exactly target, not mytarget
              - a directory pattern like build/ matches the build directory itself (when directory=true)
                and any descendant path underneath build/
              - negation only flips the result if that negated pattern itself matches the path
            """)
    @Contract(
        requires = "patterns != null && path != null",
        ensures  = "$result is true iff the last matching non-comment pattern ignores the path under the benchmark rules",
        throws_  = {NullPointerException.class}
    )
    @Example(label = "plain name root", input = "patterns=[target], path=target, directory=false", output = "true")
    @Example(label = "plain name nested", input = "patterns=[target], path=build/target, directory=true", output = "true")
    @Example(label = "plain name no substring", input = "patterns=[target], path=mytarget, directory=false", output = "false")
    @Example(label = "glob basename", input = "patterns=[*.log], path=logs/app.log, directory=false", output = "true")
    @Example(label = "rooted wildcard", input = "patterns=[/docs/*.md], path=docs/readme.md, directory=false", output = "true")
    @Example(label = "rooted wildcard nested miss", input = "patterns=[/docs/*.md], path=nested/docs/readme.md, directory=false", output = "false")
    @Example(label = "directory descendant", input = "patterns=[build/], path=build/tmp/cache.bin, directory=false", output = "true")
    @Example(label = "directory not file", input = "patterns=[build/], path=build, directory=false", output = "false")
    @Example(label = "negation", input = "patterns=[*.log, !keep.log], path=keep.log, directory=false", output = "false")
    @Example(label = "last match wins", input = "patterns=[*.log, !debug.log, debug.log], path=debug.log, directory=false", output = "true")
    @Example(label = "globstar", input = "patterns=[logs/**], path=logs/2026/03/app.log, directory=false", output = "true")
    @Example(label = "normalize slashes", input = "patterns=[build/**], path=build\\\\tmp\\\\cache.bin, directory=false", output = "true")
    @Property("adding unmatched comment or blank patterns does not change the result")
    @Property("normalizing path separators from \\\\ to / does not change the result")
    @Stub("Translate the supported pattern subset to safe regexes or equivalent matching logic. Pay special attention to last-match-wins, directory descendants, and plain names matching path segments rather than substrings.")
    @Override
    public boolean shouldIgnore(List<String> patterns, String path, boolean directory) {
        throw new UnsupportedOperationException();
    }
}
