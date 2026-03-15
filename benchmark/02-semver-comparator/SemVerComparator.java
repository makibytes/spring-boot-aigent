package com.aigent.benchmark.semver;

import java.util.Comparator;

/**
 * Compares two semantic version strings following SemVer 2.0.0 specification.
 * See https://semver.org/spec/v2.0.0.html
 */
public interface SemVerComparator extends Comparator<String> {
    /**
     * Compares two SemVer strings.
     *
     * @return negative if v1 < v2, zero if v1 == v2, positive if v1 > v2
     * @throws IllegalArgumentException if either string is not a valid SemVer
     */
    @Override
    int compare(String v1, String v2);
}
