package com.aigent.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Aigent starter.
 *
 * <p>Set in {@code application.properties} / {@code application.yml}:
 * <pre>
 * # Warn at startup when @Stub methods are found (default)
 * aigent.on-stub=WARN
 *
 * # Prevent startup when @Stub methods are found (recommended for production profiles)
 * aigent.on-stub=FAIL
 *
 * # Disable the detector entirely
 * aigent.on-stub=IGNORE
 * </pre>
 */
@ConfigurationProperties(prefix = "aigent")
public class AigentProperties {

    /**
     * What to do when {@code @Stub}-annotated methods are discovered in Spring beans at startup.
     * Defaults to {@code WARN}.
     */
    private OnStub onStub = OnStub.WARN;

    public OnStub getOnStub() { return onStub; }
    public void setOnStub(OnStub onStub) { this.onStub = onStub; }

    public enum OnStub {
        /** Log a warning and continue. Good for development. */
        WARN,
        /** Throw {@link IllegalStateException} and abort startup. Good for production profiles. */
        FAIL,
        /** Do nothing. Disables the detector. */
        IGNORE
    }
}
