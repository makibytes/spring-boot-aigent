package com.aigent.benchmark.diff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Authoritative tests for UnifiedDiffApplier.
 * The implementation under test must be named UnifiedDiffApplierImpl in this package.
 */
class UnifiedDiffApplierTest {

    private UnifiedDiffApplier applier;

    @BeforeEach
    void setUp() throws Exception {
        applier = (UnifiedDiffApplier) Class
                .forName("com.aigent.benchmark.diff.UnifiedDiffApplierImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private static String text(String... lines) {
        return String.join("\n", lines);
    }

    @Test
    void emptyDiffReturnsOriginal() {
        assertThat(applier.apply(text("a", "b"), "")).isEqualTo(text("a", "b"));
    }

    @Test
    void fileHeadersWithoutHunksReturnOriginal() {
        assertThat(applier.apply(text("a", "b"), text("--- a.txt", "+++ a.txt")))
                .isEqualTo(text("a", "b"));
    }

    @Test
    void addLineAtBeginning() {
        String diff = text(
                "@@ -1,2 +1,3 @@",
                "+zero",
                " a",
                " b"
        );

        assertThat(applier.apply(text("a", "b"), diff)).isEqualTo(text("zero", "a", "b"));
    }

    @Test
    void addLineAtEnd() {
        String diff = text(
                "@@ -1,2 +1,3 @@",
                " a",
                " b",
                "+c"
        );

        assertThat(applier.apply(text("a", "b"), diff)).isEqualTo(text("a", "b", "c"));
    }

    @Test
    void deleteLineAtBeginning() {
        String diff = text(
                "@@ -1,3 +1,2 @@",
                "-zero",
                " a",
                " b"
        );

        assertThat(applier.apply(text("zero", "a", "b"), diff)).isEqualTo(text("a", "b"));
    }

    @Test
    void deleteLineAtEnd() {
        String diff = text(
                "@@ -1,3 +1,2 @@",
                " a",
                " b",
                "-c"
        );

        assertThat(applier.apply(text("a", "b", "c"), diff)).isEqualTo(text("a", "b"));
    }

    @Test
    void replaceMiddleLine() {
        String diff = text(
                "@@ -1,3 +1,3 @@",
                " a",
                "-old",
                "+new",
                " c"
        );

        assertThat(applier.apply(text("a", "old", "c"), diff)).isEqualTo(text("a", "new", "c"));
    }

    @Test
    void insertBlankLine() {
        String diff = text(
                "@@ -1,2 +1,3 @@",
                " a",
                "+",
                " b"
        );

        assertThat(applier.apply(text("a", "b"), diff)).isEqualTo(text("a", "", "b"));
    }

    @Test
    void deleteAllLines() {
        String diff = text(
                "@@ -1,2 +0,0 @@",
                "-a",
                "-b"
        );

        assertThat(applier.apply(text("a", "b"), diff)).isEqualTo("");
    }

    @Test
    void createFromEmptyOriginal() {
        String diff = text(
                "@@ -0,0 +1,2 @@",
                "+hello",
                "+world"
        );

        assertThat(applier.apply("", diff)).isEqualTo(text("hello", "world"));
    }

    @Test
    void multipleChangesInSingleHunk() {
        String diff = text(
                "@@ -1,4 +1,5 @@",
                " a",
                "-b",
                "+beta",
                " c",
                "+delta",
                " d"
        );

        assertThat(applier.apply(text("a", "b", "c", "d"), diff))
                .isEqualTo(text("a", "beta", "c", "delta", "d"));
    }

    @Test
    void multipleHunks() {
        String diff = text(
                "--- a.txt",
                "+++ a.txt",
                "@@ -1,2 +1,2 @@",
                " a",
                "-b",
                "+beta",
                "@@ -4,2 +4,3 @@",
                " d",
                " e",
                "+f"
        );

        assertThat(applier.apply(text("a", "b", "c", "d", "e"), diff))
                .isEqualTo(text("a", "beta", "c", "d", "e", "f"));
    }

    @Test
    void preservesUntouchedLinesBetweenHunks() {
        String diff = text(
                "@@ -2,2 +2,2 @@",
                " keep-2",
                "-change-me",
                "+changed",
                "@@ -5,1 +5,1 @@",
                "-tail",
                "+tail-2"
        );

        assertThat(applier.apply(text("keep-1", "keep-2", "change-me", "keep-4", "tail"), diff))
                .isEqualTo(text("keep-1", "keep-2", "changed", "keep-4", "tail-2"));
    }

    @Test
    void contextMismatchThrows() {
        String diff = text(
                "@@ -1,2 +1,2 @@",
                " x",
                " b"
        );

        assertThatThrownBy(() -> applier.apply(text("a", "b"), diff))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletionMismatchThrows() {
        String diff = text(
                "@@ -1,2 +1,1 @@",
                "-x",
                " b"
        );

        assertThatThrownBy(() -> applier.apply(text("a", "b"), diff))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedHeaderThrows() {
        String diff = text(
                "@@ wrong @@",
                " a"
        );

        assertThatThrownBy(() -> applier.apply(text("a"), diff))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void diffBodyWithoutHeaderThrows() {
        assertThatThrownBy(() -> applier.apply(text("a"), "+b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullOriginalThrows() {
        assertThatThrownBy(() -> applier.apply(null, ""))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullDiffThrows() {
        assertThatThrownBy(() -> applier.apply("", null))
                .isInstanceOf(NullPointerException.class);
    }
}
