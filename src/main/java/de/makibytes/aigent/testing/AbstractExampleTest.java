package de.makibytes.aigent.testing;

import de.makibytes.aigent.Example;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for automatically testing all {@link Example} annotations on a target class.
 *
 * <p>Usage with JUnit 5 {@code @TestFactory}:
 * <pre>{@code
 * class MyServiceExampleTest extends AbstractExampleTest<MyService> {
 *     @Override protected MyService createTarget() {
 *         return new MyService(new SomeDependency());
 *     }
 * }
 * }</pre>
 *
 * <p>Or use {@link #verifyAll()} directly:
 * <pre>{@code
 * new AbstractExampleTest<>() {
 *     protected MyService createTarget() { return new MyService(); }
 * }.verifyAll();
 * }</pre>
 */
public abstract class AbstractExampleTest<T> {

    private final ExampleRunner runner = new ExampleRunner();

    /**
     * Create an instance of the class under test.
     * May throw if instantiation requires reflection or setup that can fail.
     */
    protected abstract T createTarget() throws Exception;

    /**
     * Whether to enforce {@code @Contract} annotations at example-run time.
     * Subclasses may override to return {@code true}.
     */
    protected boolean enforceContracts() { return false; }

    /**
     * The interface type to use when wrapping the target for contract enforcement.
     * Must be non-null when {@link #enforceContracts()} returns {@code true} and
     * the target implements an interface.
     */
    protected Class<T> contractInterface() { return null; }

    /**
     * Runs all {@link Example} annotations and throws {@link AssertionError} for any failure.
     */
    public void verifyAll() throws Exception {
        T target = createTarget();
        if (enforceContracts() && contractInterface() != null) {
            target = new ContractJUnitExtension().wrap(target, contractInterface());
        }
        List<ExampleRunner.ExampleResult> results = runner.runAll(target);
        List<ExampleRunner.ExampleResult> failures = results.stream()
                .filter(r -> !r.passed())
                .toList();

        if (!failures.isEmpty()) {
            String msg = "Example verification failed:\n" +
                    failures.stream()
                            .map(ExampleRunner.ExampleResult::describe)
                            .collect(Collectors.joining("\n"));
            throw new AssertionError(msg);
        }
    }

    /**
     * Returns individual results per {@link Example} — suitable for JUnit 5 {@code @TestFactory}
     * integration by the consumer.
     */
    public List<ExampleRunner.ExampleResult> runAll() throws Exception {
        return runner.runAll(createTarget());
    }
}
