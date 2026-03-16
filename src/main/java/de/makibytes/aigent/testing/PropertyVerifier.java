package de.makibytes.aigent.testing;

import de.makibytes.aigent.Property;
import de.makibytes.aigent.autoconfigure.ContractExpressionEvaluator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Verifies {@link Property} invariants against a set of sample inputs.
 *
 * <p>For each sample input, invokes the method and checks that all {@code @Property}
 * expressions evaluate to {@code true}. This is the manual-input version of property-based
 * testing — useful standalone or as a building block for jqwik integration.
 *
 * <p>Usage:
 * <pre>{@code
 * PropertyVerifier.verify(myService, "average",
 *     List.of(1.0, 2.0, 3.0),  // sample input 1
 *     List.of(5.0),             // sample input 2
 *     List.of()                 // edge case
 * );
 * }</pre>
 */
public class PropertyVerifier {

    private static final ContractExpressionEvaluator EVALUATOR = new ContractExpressionEvaluator();

    public record Violation(String methodName, String property, Object input, Object result) {
        public String describe() {
            return "Property \"%s\" violated on %s — input: %s, result: %s"
                    .formatted(property, methodName, input, result);
        }
    }

    /**
     * Verify all {@code @Property} invariants on the named method for each sample input.
     *
     * @throws AssertionError if any property is violated
     */
    public static void verify(Object target, String methodName, Object... sampleInputs) {
        Method method = findMethod(target.getClass(), methodName);
        Property[] properties = method.getAnnotationsByType(Property.class);
        if (properties.length == 0) return;

        List<Violation> violations = new ArrayList<>();

        for (Object input : sampleInputs) {
            try {
                method.setAccessible(true);
                Object result = method.invoke(target, input);

                for (Property prop : properties) {
                    boolean holds = EVALUATOR.evaluate(
                            prop.value(), method, target,
                            new Object[]{input}, result, true);
                    if (!holds) {
                        violations.add(new Violation(methodName, prop.value(), input, result));
                    }
                }
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                violations.add(new Violation(methodName, "(invocation failed: " + cause.getMessage() + ")",
                        input, null));
            }
        }

        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(Violation::describe)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            throw new AssertionError("Property verification failed:\n" + msg);
        }
    }

    /**
     * Returns all violations without throwing — useful for reporting.
     */
    public static List<Violation> check(Object target, String methodName, Object... sampleInputs) {
        Method method = findMethod(target.getClass(), methodName);
        Property[] properties = method.getAnnotationsByType(Property.class);
        List<Violation> violations = new ArrayList<>();

        for (Object input : sampleInputs) {
            try {
                method.setAccessible(true);
                Object result = method.invoke(target, input);

                for (Property prop : properties) {
                    boolean holds = EVALUATOR.evaluate(
                            prop.value(), method, target,
                            new Object[]{input}, result, true);
                    if (!holds) {
                        violations.add(new Violation(methodName, prop.value(), input, result));
                    }
                }
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                violations.add(new Violation(methodName, "(invocation failed: " + cause.getMessage() + ")",
                        input, null));
            }
        }

        return violations;
    }

    private static Method findMethod(Class<?> clazz, String name) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No method '%s' found on %s".formatted(name, clazz.getSimpleName())));
    }
}
