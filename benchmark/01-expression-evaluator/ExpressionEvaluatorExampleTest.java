package de.makibytes.benchmark.expression;

import de.makibytes.aigent.testing.AbstractExampleTest;
import org.junit.jupiter.api.Test;

/**
 * Runs all @Example annotations on ExpressionEvaluatorImpl as JUnit tests.
 * These are derived directly from the aigent spec — any mismatch is a spec violation.
 */
class ExpressionEvaluatorExampleTest extends AbstractExampleTest<Object> {

    @Override
    protected Object createTarget() throws Exception {
        return Class.forName("de.makibytes.benchmark.expression.ExpressionEvaluatorImpl")
                .getDeclaredConstructor().newInstance();
    }

    @Test
    void allExamplesPass() throws Exception {
        verifyAll();
    }
}
