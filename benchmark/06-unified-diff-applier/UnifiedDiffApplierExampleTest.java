package de.makibytes.benchmark.diff;

import de.makibytes.aigent.testing.AbstractExampleTest;
import org.junit.jupiter.api.Test;

/**
 * Runs all @Example annotations on UnifiedDiffApplierImpl as JUnit tests.
 */
class UnifiedDiffApplierExampleTest extends AbstractExampleTest<Object> {

    @Override
    protected Object createTarget() throws Exception {
        return Class.forName("de.makibytes.benchmark.diff.UnifiedDiffApplierImpl")
                .getDeclaredConstructor().newInstance();
    }

    @Test
    void allExamplesPass() throws Exception {
        verifyAll();
    }
}
