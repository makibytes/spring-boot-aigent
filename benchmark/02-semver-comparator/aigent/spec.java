package de.makibytes.benchmark.semver;

import de.makibytes.aigent.*;

/**
 * Aigent spec for SemVerComparatorImpl following SemVer 2.0.0 precisely.
 * All annotations are enforced at runtime via SpEL.
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
        requires = "$v1 != null && $v2 != null",
        ensures  = "compare($v2, $v1) == -$result || ($result == 0 && compare($v2, $v1) == 0)",
        throws_  = {IllegalArgumentException.class}
    )
    @Example(label = "equal",                    input = "{'1.0.0', '1.0.0'}",          output = "0")
    @Example(label = "major differs",            input = "{'2.0.0', '1.0.0'}",          output = "1")
    @Example(label = "numeric minor — CRITICAL", input = "{'1.10.0', '1.9.0'}",         output = "1")
    @Example(label = "numeric patch",            input = "{'1.0.10', '1.0.9'}",         output = "1")
    @Example(label = "prerelease lt release",    input = "{'1.0.0-alpha', '1.0.0'}",    output = "-1")
    @Example(label = "shorter pre lt longer",    input = "{'1.0.0-alpha', '1.0.0-alpha.1'}", output = "-1")
    @Example(label = "numeric pre field",        input = "{'1.0.0-alpha.2', '1.0.0-alpha.10'}", output = "-1")
    @Example(label = "numeric lt alpha pre",     input = "{'1.0.0-1', '1.0.0-alpha'}",  output = "-1")
    @Example(label = "build metadata ignored",   input = "{'1.0.0+b1', '1.0.0+b2'}",   output = "0")
    @Example(label = "build vs no build",        input = "{'1.0.0+sha', '1.0.0'}",      output = "0")
    @Example(label = "pre+build vs release",     input = "{'1.0.0-alpha+b1', '1.0.0'}", output = "-1")
    @Property("compare($v1, $v1) == 0")
    @Property("T(Integer).signum($result) == -T(Integer).signum(compare($v2, $v1)) || ($result == 0 && compare($v2, $v1) == 0)")
    @Stub("Strip build metadata first (+...), then split into release and pre-release (-). " +
          "Compare major/minor/patch as integers. For pre-release: numeric-only fields as int, " +
          "others as string, numeric always < alphanumeric. Missing pre-release means higher precedence.")
    @Override
    public int compare(String v1, String v2) {
        throw new UnsupportedOperationException();
    }
}
