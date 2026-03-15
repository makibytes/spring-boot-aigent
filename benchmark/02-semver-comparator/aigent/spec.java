package com.aigent.benchmark.semver;

/**
 * Aigent spec for SemVerComparatorImpl following SemVer 2.0.0 precisely.
 * Implement the @Stub method. All edge cases are specified in @Example.
 */
public class SemVerComparatorImpl implements SemVerComparator {

    @Intent("""
            Compare two SemVer 2.0.0 version strings. Return negative if v1 < v2, 0 if equal,
            positive if v1 > v2.

            SemVer 2.0.0 precedence rules (§11):

            1. Major, minor, patch are compared as INTEGERS (not strings).
               "1.10.0" > "1.9.0" because 10 > 9 numerically.

            2. Pre-release version has LOWER precedence than the associated normal version.
               "1.0.0-alpha" < "1.0.0"

            3. Pre-release is compared field-by-field, split on '.':
               a. Fields consisting only of digits are compared as integers.
               b. Fields with letters/hyphens are compared lexicographically.
               c. Numeric fields always have LOWER precedence than alphanumeric fields.
                  "1.0.0-1" < "1.0.0-alpha" (numeric < alphanumeric)
               d. A shorter pre-release is less than a longer one when all initial
                  fields are equal. "1.0.0-alpha" < "1.0.0-alpha.1"

            4. Build metadata (the part after '+') MUST be IGNORED for precedence.
               "1.0.0+build.1" == "1.0.0+build.2" == "1.0.0"

            A version string has the format: MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
            """)
    @Contract(
        requires = "v1 != null && v2 != null && both match SemVer format",
        ensures  = "result < 0 iff v1 < v2, result == 0 iff v1 == v2, result > 0 iff v1 > v2",
        throws_  = {IllegalArgumentException.class}
    )
    @Example(label = "equal",                  input = "v1=\"1.0.0\",   v2=\"1.0.0\"",              output = "0")
    @Example(label = "major differs",          input = "v1=\"2.0.0\",   v2=\"1.0.0\"",              output = "positive")
    @Example(label = "numeric minor — CRITICAL", input = "v1=\"1.10.0\", v2=\"1.9.0\"",             output = "positive  // 10>9 numerically, NOT '1'<'9' lexicographically")
    @Example(label = "numeric patch",          input = "v1=\"1.0.10\",  v2=\"1.0.9\"",              output = "positive")
    @Example(label = "prerelease < release",   input = "v1=\"1.0.0-alpha\", v2=\"1.0.0\"",          output = "negative")
    @Example(label = "shorter pre < longer",   input = "v1=\"1.0.0-alpha\", v2=\"1.0.0-alpha.1\"",  output = "negative")
    @Example(label = "numeric pre field",      input = "v1=\"1.0.0-alpha.2\", v2=\"1.0.0-alpha.10\"", output = "negative  // 2<10 numerically")
    @Example(label = "numeric < alpha pre",    input = "v1=\"1.0.0-1\", v2=\"1.0.0-alpha\"",        output = "negative  // numeric ID has lower precedence than alphanumeric")
    @Example(label = "build metadata ignored", input = "v1=\"1.0.0+b1\", v2=\"1.0.0+b2\"",         output = "0  // build metadata irrelevant for precedence")
    @Example(label = "build vs no build",      input = "v1=\"1.0.0+sha\", v2=\"1.0.0\"",            output = "0")
    @Example(label = "pre+build vs release",   input = "v1=\"1.0.0-alpha+b1\", v2=\"1.0.0\"",      output = "negative")
    @Property("compare(v, v) == 0 for all valid v")
    @Property("compare(v1, v2) == -compare(v2, v1) for all valid v1, v2")
    @Stub("Strip build metadata first (+...), then split into release and pre-release (-)." +
          "Compare major/minor/patch as integers. For pre-release: numeric-only fields as int, " +
          "others as string, numeric always < alphanumeric. A missing pre-release means higher precedence.")
    @Override
    public int compare(String v1, String v2) {
        throw new UnsupportedOperationException();
    }
}
