package de.makibytes.benchmark.cron;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for CronMatcher.
 *
 * Reference calendar (all 2024, Mon=1 Sun=0/7):
 *   2024-01-01 Monday  (dow=1)
 *   2024-01-07 Sunday  (dow=0 or 7)
 *   2024-01-15 Monday  (dow=1)
 *   2024-01-31 Wednesday (dow=3)
 *   2024-02-29 Thursday (dow=4) — 2024 is a leap year
 *   2024-03-01 Friday  (dow=5)
 *   2024-06-15 Saturday (dow=6)
 *   2024-12-31 Tuesday  (dow=2)
 *
 * The implementation must be named CronMatcherImpl in this package.
 */
class CronMatcherTest {

    private CronMatcher matcher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        matcher = (CronMatcher) Class
                .forName("de.makibytes.benchmark.cron.CronMatcherImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private boolean matches(String cron, String dt) {
        return matcher.matches(cron, dt);
    }

    // ── Wildcard (everything matches) ─────────────────────────────────────────

    @Test void allStarsMatch() {
        assertThat(matches("* * * * *", "2024-06-15 10:30")).isTrue();
    }

    // ── Exact single-value fields ──────────────────────────────────────────────

    @Test void exactMinuteMatch() { assertThat(matches("30 * * * *", "2024-01-01 10:30")).isTrue(); }
    @Test void exactMinuteNoMatch() { assertThat(matches("30 * * * *", "2024-01-01 10:31")).isFalse(); }

    @Test void exactHourMatch()   { assertThat(matches("0 14 * * *", "2024-01-01 14:00")).isTrue(); }
    @Test void exactHourNoMatch() { assertThat(matches("0 14 * * *", "2024-01-01 15:00")).isFalse(); }

    @Test void exactDomMatch()   { assertThat(matches("0 0 15 * *", "2024-01-15 00:00")).isTrue(); }
    @Test void exactDomNoMatch() { assertThat(matches("0 0 15 * *", "2024-01-16 00:00")).isFalse(); }

    @Test void exactMonthMatch()   { assertThat(matches("0 0 1 3 *", "2024-03-01 00:00")).isTrue(); }
    @Test void exactMonthNoMatch() { assertThat(matches("0 0 1 3 *", "2024-04-01 00:00")).isFalse(); }

    /** 2024-01-01 is a Monday → dow=1. */
    @Test void exactDowMatch()   { assertThat(matches("0 0 * * 1", "2024-01-01 00:00")).isTrue(); }
    @Test void exactDowNoMatch() { assertThat(matches("0 0 * * 2", "2024-01-01 00:00")).isFalse(); }

    // ── Ranges ────────────────────────────────────────────────────────────────

    @Test void minuteRangeLow()  { assertThat(matches("15-30 * * * *", "2024-01-01 10:15")).isTrue(); }
    @Test void minuteRangeHigh() { assertThat(matches("15-30 * * * *", "2024-01-01 10:30")).isTrue(); }
    @Test void minuteRangeMiss() { assertThat(matches("15-30 * * * *", "2024-01-01 10:31")).isFalse(); }

    @Test void hourRange()       { assertThat(matches("0 9-17 * * *", "2024-01-01 12:00")).isTrue(); }
    @Test void hourRangeEdge()   { assertThat(matches("0 9-17 * * *", "2024-01-01 17:00")).isTrue(); }
    @Test void hourRangeMiss()   { assertThat(matches("0 9-17 * * *", "2024-01-01 18:00")).isFalse(); }

    // ── Step expressions ──────────────────────────────────────────────────────

    /**
     * CRITICAL: star/step — */n means values 0, n, 2n, ... up to the field's max.
     * For minutes: */15 → 0, 15, 30, 45.  NOT every minute that is a multiple of 15
     * starting from the current value.
     */
    @Test void stepEvery15MinAt0()  { assertThat(matches("*/15 * * * *", "2024-01-01 10:00")).isTrue(); }
    @Test void stepEvery15MinAt15() { assertThat(matches("*/15 * * * *", "2024-01-01 10:15")).isTrue(); }
    @Test void stepEvery15MinAt30() { assertThat(matches("*/15 * * * *", "2024-01-01 10:30")).isTrue(); }
    @Test void stepEvery15MinAt45() { assertThat(matches("*/15 * * * *", "2024-01-01 10:45")).isTrue(); }
    @Test void stepEvery15MinMiss() { assertThat(matches("*/15 * * * *", "2024-01-01 10:14")).isFalse(); }

    /** */6 for hours → 0, 6, 12, 18. */
    @Test void stepHourMatch() { assertThat(matches("0 */6 * * *", "2024-01-01 12:00")).isTrue(); }
    @Test void stepHourMiss()  { assertThat(matches("0 */6 * * *", "2024-01-01 13:00")).isFalse(); }

    /**
     * CRITICAL: range/step — a-b/n means values a, a+n, a+2n, ... ≤ b.
     * 1-10/3 → 1, 4, 7, 10.  (NOT 0, 3, 6, 9 starting from 0.)
     */
    @Test void rangeStepMatch() { assertThat(matches("0 1-10/3 * * *", "2024-01-01 07:00")).isTrue(); }
    @Test void rangeStepMiss()  { assertThat(matches("0 1-10/3 * * *", "2024-01-01 02:00")).isFalse(); }

    // ── Comma-separated lists ─────────────────────────────────────────────────

    @Test void minuteList() { assertThat(matches("0,15,30,45 * * * *", "2024-01-01 10:30")).isTrue(); }
    @Test void minuteListMiss() { assertThat(matches("0,15,30,45 * * * *", "2024-01-01 10:31")).isFalse(); }
    @Test void hourList()   { assertThat(matches("0 9,12,17 * * *", "2024-01-01 12:00")).isTrue(); }
    @Test void dowList()    { assertThat(matches("0 0 * * 1,3,5", "2024-01-01 00:00")).isTrue(); }  // Monday=1

    // ── Month names (case-insensitive) ────────────────────────────────────────

    @Test void monthNameJan() { assertThat(matches("0 0 1 JAN *", "2024-01-01 00:00")).isTrue(); }
    @Test void monthNameDec() { assertThat(matches("0 0 31 DEC *", "2024-12-31 00:00")).isTrue(); }
    @Test void monthNameLowercase() { assertThat(matches("0 0 1 jan *", "2024-01-01 00:00")).isTrue(); }
    @Test void monthNameRange() { assertThat(matches("0 0 1 MAR-MAY *", "2024-04-01 00:00")).isTrue(); }
    @Test void monthNameRangeMiss() { assertThat(matches("0 0 1 MAR-MAY *", "2024-06-01 00:00")).isFalse(); }

    // ── DOW names and special numbering ───────────────────────────────────────

    /** 0 = Sunday — the traditional Unix mapping. */
    @Test void dowZeroIsSunday() { assertThat(matches("0 0 * * 0", "2024-01-07 00:00")).isTrue(); }

    /**
     * CRITICAL: 7 is ALSO Sunday.  Both 0 and 7 must match Sunday.
     * Many implementations only check 0 and miss 7.
     */
    @Test void dowSevenIsSunday() { assertThat(matches("0 0 * * 7", "2024-01-07 00:00")).isTrue(); }

    @Test void dowNameSun() { assertThat(matches("0 0 * * SUN", "2024-01-07 00:00")).isTrue(); }
    @Test void dowNameMon() { assertThat(matches("0 0 * * MON", "2024-01-01 00:00")).isTrue(); }
    @Test void dowNameRange() { assertThat(matches("0 0 * * MON-FRI", "2024-01-03 00:00")).isTrue(); }  // Wed
    @Test void dowNameRangeMiss() { assertThat(matches("0 0 * * MON-FRI", "2024-01-07 00:00")).isFalse(); }  // Sun

    // ── DOM + DOW interaction (OR semantics when both restricted) ─────────────

    /**
     * CRITICAL: if BOTH dom AND dow fields are restricted (neither is '*'),
     * the cron fires when EITHER the DOM matches OR the DOW matches — not both.
     * This is the original Unix cron "OR" semantics.
     *
     * 2024-01-15 = Monday (dow=1), dom=15.
     */

    /** DOM=15 matches. DOW=0 (Sunday) does NOT match. With OR: true. */
    @Test void domMatchDowDoesNot() {
        assertThat(matches("0 0 15 * 0", "2024-01-15 00:00")).isTrue();
    }

    /** DOM=16 does NOT match. DOW=1 (Monday) matches. With OR: true. */
    @Test void dowMatchDomDoesNot() {
        assertThat(matches("0 0 16 * 1", "2024-01-15 00:00")).isTrue();
    }

    /** DOM=16 and DOW=0 (Sunday) — NEITHER matches → false. */
    @Test void neitherDomNorDowMatch() {
        assertThat(matches("0 0 16 * 0", "2024-01-15 00:00")).isFalse();
    }

    /**
     * When DOM is '*' and DOW is restricted, only DOW is checked (standard AND semantics
     * because the unrestricted '*' means "don't restrict by this field").
     */
    @Test void wildcardDomDowRestricted() {
        assertThat(matches("0 0 * * 1", "2024-01-15 00:00")).isTrue();   // Monday
        assertThat(matches("0 0 * * 2", "2024-01-15 00:00")).isFalse();  // not Tuesday
    }

    // ── Leap year ─────────────────────────────────────────────────────────────

    /** 2024 is a leap year — Feb 29 exists. */
    @Test void feb29LeapYear() {
        assertThat(matches("0 0 29 2 *", "2024-02-29 00:00")).isTrue();
    }

    /** 2023 is NOT a leap year — Feb 29 does not exist; the cron never fires. */
    @Test void feb29NonLeapYear() {
        assertThat(matches("0 0 29 2 *", "2023-02-28 00:00")).isFalse();
    }

    // ── Miscellaneous ─────────────────────────────────────────────────────────

    /** '?' is treated identically to '*' (used to avoid DOM/DOW conflict). */
    @Test void questionMarkEqualsWildcard() {
        assertThat(matches("0 0 ? * ?", "2024-01-15 00:00")).isTrue();
    }

    /** Complex combined expression: list + range in same field. */
    @Test void complexCombinedField() {
        // minute field: 0,30  hour: 9-17  day: *  month: MON-FRI  (should be month 1-5 but that's a range)
        // Let's use: "0,30 9-17 * * MON-FRI" on 2024-01-01 09:30 (Monday)
        assertThat(matches("0,30 9-17 * * MON-FRI", "2024-01-01 09:30")).isTrue();
    }
}
