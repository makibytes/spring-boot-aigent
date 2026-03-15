package com.aigent.benchmark.gitignore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Authoritative tests for GitIgnoreMatcher.
 * The implementation under test must be named GitIgnoreMatcherImpl in this package.
 */
class GitIgnoreMatcherTest {

    private GitIgnoreMatcher matcher;

    @BeforeEach
    void setUp() throws Exception {
        matcher = (GitIgnoreMatcher) Class
                .forName("com.aigent.benchmark.gitignore.GitIgnoreMatcherImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private static List<String> patterns(String... values) {
        return List.of(values);
    }

    @Test
    void emptyPatternListDoesNotIgnoreAnything() {
        assertThat(matcher.shouldIgnore(patterns(), "src/App.java", false)).isFalse();
    }

    @Test
    void commentsAndBlankLinesAreIgnored() {
        assertThat(matcher.shouldIgnore(patterns("", "# generated files"), "build/output.txt", false)).isFalse();
    }

    @Test
    void exactNameMatchesRootFile() {
        assertThat(matcher.shouldIgnore(patterns("target"), "target", false)).isTrue();
    }

    @Test
    void exactNameMatchesNestedSegment() {
        assertThat(matcher.shouldIgnore(patterns("target"), "build/target", true)).isTrue();
    }

    @Test
    void exactNameDoesNotMatchSubstring() {
        assertThat(matcher.shouldIgnore(patterns("target"), "mytarget", false)).isFalse();
    }

    @Test
    void starMatchesBasenameAtRoot() {
        assertThat(matcher.shouldIgnore(patterns("*.log"), "server.log", false)).isTrue();
    }

    @Test
    void starMatchesBasenameAtAnyDepth() {
        assertThat(matcher.shouldIgnore(patterns("*.log"), "logs/server.log", false)).isTrue();
    }

    @Test
    void starDoesNotCrossDirectorySeparators() {
        assertThat(matcher.shouldIgnore(patterns("foo*bar"), "foo/x/bar", false)).isFalse();
    }

    @Test
    void rootedPatternMatchesOnlyAtRoot() {
        assertThat(matcher.shouldIgnore(patterns("/target"), "target", true)).isTrue();
        assertThat(matcher.shouldIgnore(patterns("/target"), "build/target", true)).isFalse();
    }

    @Test
    void rootedWildcardMatchesOnlyRootDirectory() {
        assertThat(matcher.shouldIgnore(patterns("/docs/*.md"), "docs/readme.md", false)).isTrue();
        assertThat(matcher.shouldIgnore(patterns("/docs/*.md"), "nested/docs/readme.md", false)).isFalse();
    }

    @Test
    void directoryPatternMatchesDirectoryItself() {
        assertThat(matcher.shouldIgnore(patterns("build/"), "build", true)).isTrue();
    }

    @Test
    void directoryPatternMatchesDescendants() {
        assertThat(matcher.shouldIgnore(patterns("build/"), "build/output.txt", false)).isTrue();
        assertThat(matcher.shouldIgnore(patterns("build/"), "build/tmp/cache.bin", false)).isTrue();
    }

    @Test
    void directoryPatternDoesNotMatchFileWithSameName() {
        assertThat(matcher.shouldIgnore(patterns("build/"), "build", false)).isFalse();
    }

    @Test
    void negationCanReincludeSpecificFile() {
        assertThat(matcher.shouldIgnore(patterns("*.log", "!keep.log"), "keep.log", false)).isFalse();
    }

    @Test
    void lastMatchWins() {
        assertThat(matcher.shouldIgnore(patterns("*.log", "!debug.log", "debug.log"), "debug.log", false)).isTrue();
    }

    @Test
    void standaloneNegationWithoutEarlierIgnoreKeepsFileIncluded() {
        assertThat(matcher.shouldIgnore(patterns("!keep.log"), "keep.log", false)).isFalse();
    }

    @Test
    void doubleStarMatchesNestedDescendants() {
        assertThat(matcher.shouldIgnore(patterns("logs/**"), "logs/2026/03/app.log", false)).isTrue();
    }

    @Test
    void leadingDoubleStarMatchesAnywhere() {
        assertThat(matcher.shouldIgnore(patterns("**/generated/**"), "src/generated/Foo.java", false)).isTrue();
        assertThat(matcher.shouldIgnore(patterns("**/generated/**"), "a/b/generated/Foo.java", false)).isTrue();
    }

    @Test
    void normalizeBackslashesInPath() {
        assertThat(matcher.shouldIgnore(patterns("build/**"), "build\\\\tmp\\\\cache.bin", false)).isTrue();
    }

    @Test
    void unmatchedPathIsIncluded() {
        assertThat(matcher.shouldIgnore(patterns("*.log", "build/"), "src/App.java", false)).isFalse();
    }

    @Test
    void combinedPatternsRespectOrdering() {
        List<String> patterns = patterns("*.log", "!important.log", "archive/**", "archive/keep.log");

        assertThat(matcher.shouldIgnore(patterns, "error.log", false)).isTrue();
        assertThat(matcher.shouldIgnore(patterns, "important.log", false)).isFalse();
        assertThat(matcher.shouldIgnore(patterns, "archive/2026/error.log", false)).isTrue();
        assertThat(matcher.shouldIgnore(patterns, "archive/keep.log", false)).isTrue();
    }

    @Test
    void nullPatternListThrows() {
        assertThatThrownBy(() -> matcher.shouldIgnore(null, "src/App.java", false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullPathThrows() {
        assertThatThrownBy(() -> matcher.shouldIgnore(patterns("*.log"), null, false))
                .isInstanceOf(NullPointerException.class);
    }
}
