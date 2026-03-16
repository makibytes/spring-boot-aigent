package de.makibytes.benchmark.gitignore;

import de.makibytes.aigent.testing.AbstractExampleTest;
import org.junit.jupiter.api.Test;

/**
 * Runs all @Example annotations on GitIgnoreMatcherImpl as JUnit tests.
 */
class GitIgnoreMatcherExampleTest extends AbstractExampleTest<Object> {

    @Override
    protected Object createTarget() throws Exception {
        return Class.forName("de.makibytes.benchmark.gitignore.GitIgnoreMatcherImpl")
                .getDeclaredConstructor().newInstance();
    }

    @Test
    void allExamplesPass() throws Exception {
        verifyAll();
    }
}
