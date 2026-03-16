package de.makibytes.benchmark.deps;

import de.makibytes.aigent.testing.AbstractExampleTest;
import org.junit.jupiter.api.Test;

/**
 * Runs all @Example annotations on DependencyResolverImpl as JUnit tests.
 */
class DependencyResolverExampleTest extends AbstractExampleTest<Object> {

    @Override
    protected Object createTarget() throws Exception {
        return Class.forName("de.makibytes.benchmark.deps.DependencyResolverImpl")
                .getDeclaredConstructor().newInstance();
    }

    @Test
    void allExamplesPass() throws Exception {
        verifyAll();
    }
}
