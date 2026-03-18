package de.makibytes.benchmark.cron;

import de.makibytes.aigent.*;

/**
 * Aigent spec for CronMatcherImpl.
 * Implement the @Stub method. All rules are tested by CronMatcherTest.
 */
public class CronMatcherImpl implements CronMatcher {

    @Intent("""
            Evaluate whether a datetime string matches a 5-field Unix cron expression.
            Return true if the datetime falls on a scheduled time, false otherwise.

            CRON FORMAT — 5 space-separated fields:
              min   (0-59)
              hour  (0-23)
              dom   (1-31, day-of-month)
              month (1-12)
              dow   (0-7, day-of-week; 0=Sunday, 1=Monday, ..., 6=Saturday, 7=Sunday)

            DATETIME FORMAT: "yyyy-MM-dd HH:mm"

            FIELD SYNTAX — each field is one of:
              *          any value (wildcard)
              ?          same as * (used to avoid DOM/DOW conflict)
              N          exact value N
              N-M        inclusive range N to M
              */step     all values 0,step,2*step,... up to the field maximum
              N-M/step   values N, N+step, N+2*step, ... ≤ M
              a,b,c,...  comma-separated list; each item may be *, N, range, or step

            NAMES (case-insensitive):
              Month: JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC  (→ 1-12)
              DOW:   SUN MON TUE WED THU FRI SAT                        (→ 0-6)
              Names may appear anywhere a number is expected (ranges, lists, etc.)

            CRITICAL RULES ONE-SHOTS COMMONLY GET WRONG:

            1. DOM + DOW OR semantics:
               If BOTH dom AND dow are restricted (neither is * or ?), a datetime
               matches if the dom matches OR the dow matches — NOT both must match.
               This is the original Unix cron semantics (Vixie cron).
               If only one of dom/dow is restricted, only that field is checked.

            2. 0 AND 7 both mean Sunday:
               A dow value of 7 is a valid alias for Sunday (0). Both must work.

            3. */n starts at the field's minimum (0 for min/hour/dow, 1 for dom/month):
               */15 in the minute field → 0,15,30,45 (starts at 0, NOT at the current value).

            4. range/step — a-b/n starts at a, not at 0:
               1-10/3 in the hour field → 1,4,7,10 (NOT 0,3,6,9).

            5. Month names and DOW names must be case-insensitive.

            6. Leap year: Feb 29 only exists in leap years.
               A cron "0 0 29 2 *" never fires in non-leap years.
            """)
    @Contract(
        requires = "cron != null && datetime != null",
        ensures  = "$result == true || $result == false"
    )

    // ── Wildcard ───────────────────────────────────────────────────────────────
    @Example(label = "all stars",           input = "'* * * * *', '2024-06-15 10:30'",  output = "true")

    // ── Exact values ───────────────────────────────────────────────────────────
    @Example(label = "exact min match",     input = "'30 * * * *', '2024-01-01 10:30'", output = "true")
    @Example(label = "exact min miss",      input = "'30 * * * *', '2024-01-01 10:31'", output = "false")
    @Example(label = "exact hour match",    input = "'0 14 * * *', '2024-01-01 14:00'", output = "true")
    @Example(label = "exact DOM match",     input = "'0 0 15 * *', '2024-01-15 00:00'", output = "true")
    @Example(label = "exact month match",   input = "'0 0 1 3 *', '2024-03-01 00:00'",  output = "true")
    @Example(label = "exact DOW match",     input = "'0 0 * * 1', '2024-01-01 00:00'",  output = "true")

    // ── Ranges ─────────────────────────────────────────────────────────────────
    @Example(label = "min range low edge",  input = "'15-30 * * * *', '2024-01-01 10:15'", output = "true")
    @Example(label = "min range high edge", input = "'15-30 * * * *', '2024-01-01 10:30'", output = "true")
    @Example(label = "min range miss",      input = "'15-30 * * * *', '2024-01-01 10:31'", output = "false")
    @Example(label = "hour range mid",      input = "'0 9-17 * * *', '2024-01-01 12:00'",  output = "true")

    // ── Step expressions ───────────────────────────────────────────────────────
    @Example(label = "*/15 at 0",           input = "'*/15 * * * *', '2024-01-01 10:00'", output = "true")
    @Example(label = "*/15 at 15",          input = "'*/15 * * * *', '2024-01-01 10:15'", output = "true")
    @Example(label = "*/15 at 30",          input = "'*/15 * * * *', '2024-01-01 10:30'", output = "true")
    @Example(label = "*/15 at 45",          input = "'*/15 * * * *', '2024-01-01 10:45'", output = "true")
    @Example(label = "*/15 miss",           input = "'*/15 * * * *', '2024-01-01 10:14'", output = "false")
    @Example(label = "range/step 1-10/3",   input = "'0 1-10/3 * * *', '2024-01-01 07:00'", output = "true")
    @Example(label = "range/step miss",     input = "'0 1-10/3 * * *', '2024-01-01 02:00'", output = "false")

    // ── Comma lists ────────────────────────────────────────────────────────────
    @Example(label = "minute list match",   input = "'0,15,30,45 * * * *', '2024-01-01 10:30'", output = "true")
    @Example(label = "minute list miss",    input = "'0,15,30,45 * * * *', '2024-01-01 10:31'", output = "false")
    @Example(label = "DOW list match",      input = "'0 0 * * 1,3,5', '2024-01-01 00:00'",      output = "true")

    // ── Month names ────────────────────────────────────────────────────────────
    @Example(label = "month name JAN",      input = "'0 0 1 JAN *', '2024-01-01 00:00'",    output = "true")
    @Example(label = "month name lowercase",input = "'0 0 1 jan *', '2024-01-01 00:00'",    output = "true")
    @Example(label = "month name range",    input = "'0 0 1 MAR-MAY *', '2024-04-01 00:00'", output = "true")
    @Example(label = "month name range miss",input = "'0 0 1 MAR-MAY *', '2024-06-01 00:00'", output = "false")

    // ── DOW names and 7=Sunday ─────────────────────────────────────────────────
    @Example(label = "DOW 0 = Sunday",      input = "'0 0 * * 0', '2024-01-07 00:00'",    output = "true")
    @Example(label = "DOW 7 = Sunday",      input = "'0 0 * * 7', '2024-01-07 00:00'",    output = "true")
    @Example(label = "DOW name SUN",        input = "'0 0 * * SUN', '2024-01-07 00:00'",  output = "true")
    @Example(label = "DOW name range MON-FRI", input = "'0 0 * * MON-FRI', '2024-01-03 00:00'", output = "true")
    @Example(label = "DOW name range miss", input = "'0 0 * * MON-FRI', '2024-01-07 00:00'", output = "false")

    // ── DOM+DOW OR semantics ───────────────────────────────────────────────────
    // 2024-01-15 = Monday (DOW=1), DOM=15
    @Example(label = "DOM matches, DOW doesn't — OR → true",
             input = "'0 0 15 * 0', '2024-01-15 00:00'", output = "true")
    @Example(label = "DOW matches, DOM doesn't — OR → true",
             input = "'0 0 16 * 1', '2024-01-15 00:00'", output = "true")
    @Example(label = "neither DOM nor DOW matches — false",
             input = "'0 0 16 * 0', '2024-01-15 00:00'", output = "false")

    // ── Leap year ──────────────────────────────────────────────────────────────
    @Example(label = "Feb 29 in leap year",     input = "'0 0 29 2 *', '2024-02-29 00:00'", output = "true")
    @Example(label = "Feb 29 in non-leap year", input = "'0 0 29 2 *', '2023-02-28 00:00'", output = "false")

    // ── ? wildcard ─────────────────────────────────────────────────────────────
    @Example(label = "? equals wildcard",    input = "'0 0 ? * ?', '2024-01-15 00:00'",  output = "true")

    @Stub("""
            Parse the cron expression into 5 fields. For each field, build a
            Set<Integer> of matching values using this algorithm:
              - Split by comma to get sub-expressions
              - For each sub-expression:
                  - Extract optional /step suffix
                  - Resolve names (JAN→1, SUN→0, etc.) — case-insensitive
                  - Parse range a-b or single value or *
                  - Enumerate start,start+step,...,≤end into the set
              - For */n: start=field_min, end=field_max, step=n
              - For N/n: start=N, end=field_max, step=n

            Field min/max: min 0-59, hour 0-23, dom 1-31, month 1-12, dow 0-6.
            Map 7→0 for Sunday before adding to the DOW set.

            DOM+DOW special case:
              bothRestricted = (domExpr != "*" && domExpr != "?") &&
                               (dowExpr != "*" && dowExpr != "?")
              if bothRestricted: matches = domSet.contains(dom) || dowSet.contains(dow)
              else:              matches = domSet.contains(dom) && dowSet.contains(dow)

            For leap-year check: if month=2 and dom>28, verify the year is a leap year
            (year%4==0 && (year%100!=0 || year%400==0)). If dom=29 and not leap → false.
            """)
    @Override
    public boolean matches(String cron, String datetime) {
        throw new UnsupportedOperationException();
    }
}
