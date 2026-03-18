package de.makibytes.benchmark.cron;

/**
 * Evaluates whether a datetime matches a 5-field Unix cron expression.
 */
public interface CronMatcher {

    /**
     * Returns {@code true} if {@code datetime} matches {@code cron}.
     *
     * @param cron     5 space-separated fields: {@code min hour dom month dow}
     * @param datetime ISO-like string {@code "yyyy-MM-dd HH:mm"}
     */
    boolean matches(String cron, String datetime);
}
