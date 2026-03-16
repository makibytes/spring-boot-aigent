package de.makibytes.benchmark.csv;

import de.makibytes.aigent.testing.AbstractExampleTest;
import org.junit.jupiter.api.Test;

/**
 * Runs all @Example annotations on CsvParserImpl as JUnit tests.
 */
class CsvParserExampleTest extends AbstractExampleTest<Object> {

    @Override
    protected Object createTarget() throws Exception {
        return Class.forName("de.makibytes.benchmark.csv.CsvParserImpl")
                .getDeclaredConstructor().newInstance();
    }

    @Test
    void allExamplesPass() throws Exception {
        verifyAll();
    }
}
