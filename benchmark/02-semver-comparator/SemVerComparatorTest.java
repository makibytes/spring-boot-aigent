package com.aigent.benchmark.semver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for SemVerComparator following SemVer 2.0.0.
 * Tests ordered: basic first, then the tricky cases that one-shot approaches often fail.
 */
class SemVerComparatorTest {

    private SemVerComparator cmp;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        cmp = (SemVerComparator) Class
                .forName("com.aigent.benchmark.semver.SemVerComparatorImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    // ── Basic comparisons ─────────────────────────────────────────────────────

    @Test void equalVersions()   { assertThat(cmp.compare("1.0.0", "1.0.0")).isZero(); }
    @Test void majorDiffers()    { assertThat(cmp.compare("2.0.0", "1.0.0")).isPositive(); }
    @Test void majorDiffersRev() { assertThat(cmp.compare("1.0.0", "2.0.0")).isNegative(); }
    @Test void minorDiffers()    { assertThat(cmp.compare("1.1.0", "1.0.0")).isPositive(); }
    @Test void patchDiffers()    { assertThat(cmp.compare("1.0.1", "1.0.0")).isPositive(); }

    // ── CRITICAL: Numeric, not lexicographic ──────────────────────────────────
    //
    // The classic LLM failure: lexicographic comparison gives "1.9.0" > "1.10.0"
    // because "9" > "1" as a string. Correct answer: 1.10.0 > 1.9.0 (ten > nine).

    @Test void numericMinorComparison() {
        assertThat(cmp.compare("1.10.0", "1.9.0")).isPositive();   // 10 > 9, not "10" < "9"
    }

    @Test void numericPatchComparison() {
        assertThat(cmp.compare("1.0.10", "1.0.9")).isPositive();
    }

    @Test void numericMajorComparison() {
        assertThat(cmp.compare("10.0.0", "9.0.0")).isPositive();
    }

    // ── Pre-release versions ──────────────────────────────────────────────────

    /** Pre-release has LOWER precedence than the release version. */
    @Test void prereleaseBeforeRelease() {
        assertThat(cmp.compare("1.0.0-alpha", "1.0.0")).isNegative();
    }

    /** Shorter pre-release < longer pre-release when all fields match. */
    @Test void shorterPrereleaseIsLess() {
        assertThat(cmp.compare("1.0.0-alpha", "1.0.0-alpha.1")).isNegative();
    }

    /** Numeric identifiers compared as integers, not strings. */
    @Test void numericPrereleaseField() {
        assertThat(cmp.compare("1.0.0-alpha.2", "1.0.0-alpha.10")).isNegative();
    }

    /**
     * CRITICAL: Numeric-only identifiers have lower precedence than alphanumeric.
     * SemVer 2.0.0 §11.4.1: "Identifiers consisting of only digits are compared
     * numerically and identifiers with letters or hyphens are compared lexically.
     * Numeric identifiers always have lower precedence than alphanumeric identifiers."
     */
    @Test void numericIdentifierBeforeAlpha() {
        assertThat(cmp.compare("1.0.0-1", "1.0.0-alpha")).isNegative();
    }

    /** Alpha-then-numeric pre-release identifiers compared per SemVer rules. */
    @Test void prereleaseAlphaOrder() {
        // alpha < beta lexicographically
        assertThat(cmp.compare("1.0.0-alpha", "1.0.0-beta")).isNegative();
    }

    // ── Build metadata ────────────────────────────────────────────────────────

    /**
     * CRITICAL: Build metadata MUST be ignored when determining version precedence.
     * 1.0.0+build.1 and 1.0.0+build.2 are considered equal.
     */
    @Test void buildMetadataIgnored() {
        assertThat(cmp.compare("1.0.0+build.1", "1.0.0+build.2")).isZero();
    }

    @Test void buildMetadataIgnoredVsRelease() {
        assertThat(cmp.compare("1.0.0+sha.abc", "1.0.0")).isZero();
    }

    /** Pre-release with build metadata still has lower precedence than release. */
    @Test void prereleaseWithBuildMetadata() {
        assertThat(cmp.compare("1.0.0-alpha+build.1", "1.0.0")).isNegative();
    }
}
