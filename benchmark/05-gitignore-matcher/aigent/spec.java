package de.makibytes.benchmark.gitignore;

import de.makibytes.aigent.*;
import java.util.List;

/**
 * Aigent spec for GitIgnoreMatcherImpl.
 * All annotations are enforced at runtime via SpEL.
 */
public class GitIgnoreMatcherImpl implements GitIgnoreMatcher {

    @Intent("""
            Decide whether a relative repository path should be ignored by a simplified gitignore matcher.

            Supported pattern subset for this benchmark:
              - blank lines and lines beginning with # are ignored
              - ! negates a pattern; the LAST matching pattern wins
              - leading / anchors a pattern to the repository root
              - trailing / means the pattern targets directories and everything under them
              - * matches within a single path segment only
              - ** may span across directories
              - incoming paths may use either / or \\ and must be normalised to /

            Matching rules:
              - a pattern without leading / may match at any directory depth
              - a plain pattern like 'target' matches a path segment named exactly 'target'
              - a directory pattern like 'build/' matches the build directory (when directory=true)
                and any descendant path underneath build/
              - negation only flips the result if that negated pattern itself matches the path
            """)
    @Contract(
        requires = "$patterns != null && $path != null",
        ensures  = "$patterns.isEmpty() ? !$result : true",
        throws_  = {NullPointerException.class}
    )
    @Example(label = "plain name root",         input = "{ {'target'}, 'target', false }",                   output = "true")
    @Example(label = "plain name nested",        input = "{ {'target'}, 'build/target', true }",              output = "true")
    @Example(label = "plain name no substring",  input = "{ {'target'}, 'mytarget', false }",                 output = "false")
    @Example(label = "glob basename",            input = "{ {'*.log'}, 'logs/app.log', false }",              output = "true")
    @Example(label = "rooted wildcard match",    input = "{ {'/docs/*.md'}, 'docs/readme.md', false }",       output = "true")
    @Example(label = "rooted wildcard miss",     input = "{ {'/docs/*.md'}, 'nested/docs/readme.md', false }", output = "false")
    @Example(label = "directory descendant",     input = "{ {'build/'}, 'build/tmp/cache.bin', false }",      output = "true")
    @Example(label = "directory not file",       input = "{ {'build/'}, 'build', false }",                    output = "false")
    @Example(label = "negation",                 input = "{ {'*.log', '!keep.log'}, 'keep.log', false }",     output = "false")
    @Example(label = "last match wins",          input = "{ {'*.log', '!debug.log', 'debug.log'}, 'debug.log', false }", output = "true")
    @Example(label = "globstar",                 input = "{ {'logs/**'}, 'logs/2026/03/app.log', false }",    output = "true")
    @Example(label = "empty patterns",           input = "{ {}, 'anything.log', false }",                     output = "false")
    @Property("$patterns.isEmpty() ? !$result : true")
    @Property("$result == true || $result == false")
    @Stub("Translate each pattern to regex or equivalent matching logic. " +
          "Pay special attention to: last-match-wins, directory descendants, " +
          "plain names matching path SEGMENTS (not substrings), and normalize \\\\ to /.")
    @Override
    public boolean shouldIgnore(List<String> patterns, String path, boolean directory) {
        throw new UnsupportedOperationException();
    }
}
