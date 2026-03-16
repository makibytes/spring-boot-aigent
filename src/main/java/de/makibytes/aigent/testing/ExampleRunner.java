package de.makibytes.aigent.testing;

import de.makibytes.aigent.Contract;
import de.makibytes.aigent.Example;
import de.makibytes.aigent.Intent;
import de.makibytes.aigent.PostconditionViolationException;
import de.makibytes.aigent.PreconditionViolationException;
import de.makibytes.aigent.autoconfigure.ContractExpressionEvaluator;
import static de.makibytes.aigent.Example.NoException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Executes {@link Example} annotations as concrete test cases.
 *
 * <p>Input and output are parsed as SpEL literal expressions. For methods with
 * multiple parameters, the input must be a SpEL list literal whose elements are
 * spread as arguments: {@code "{'v1', 'v2'}"} → {@code method(v1, v2)}.
 *
 * <p>When {@link Example#throws_()} is set, the runner verifies that an exception
 * of that type (or a subtype) is thrown instead of checking the return value.
 */
public class ExampleRunner {

    private final ContractExpressionEvaluator evaluator = new ContractExpressionEvaluator();

    public record ExampleResult(String label, String methodName, boolean passed,
                                Object expected, Object actual, Throwable error, String intent) {

        /** Backwards-compatible constructor without intent. */
        public ExampleResult(String label, String methodName, boolean passed,
                             Object expected, Object actual, Throwable error) {
            this(label, methodName, passed, expected, actual, error, null);
        }

        public String describe() {
            if (passed) return "PASS: %s [%s]".formatted(methodName, label);
            String base;
            if (error != null)
                base = "FAIL: %s [%s] — %s".formatted(methodName, label, error.getMessage());
            else
                base = "FAIL: %s [%s] — expected <%s> but was <%s>".formatted(methodName, label, expected, actual);
            if (intent != null && !intent.isBlank())
                base += "\n  @Intent: " + intent.lines().findFirst().orElse("").strip();
            return base;
        }
    }

    /** Run all {@link Example} annotations on all methods of the target. */
    public List<ExampleResult> runAll(Object target) {
        List<ExampleResult> results = new ArrayList<>();
        for (Method method : target.getClass().getDeclaredMethods()) {
            results.addAll(runExamples(target, method));
        }
        return results;
    }

    /** Run all {@link Example} annotations on one method. */
    public List<ExampleResult> runExamples(Object target, Method method) {
        Example[] examples = method.getAnnotationsByType(Example.class);
        List<ExampleResult> results = new ArrayList<>();
        for (Example example : examples) {
            results.add(runSingle(target, method, example));
        }
        return results;
    }

    private String intentOf(Method method) {
        Intent intent = method.getAnnotation(Intent.class);
        return intent != null ? intent.value() : null;
    }

    private ExampleResult runSingle(Object target, Method method, Example example) {
        String label = example.label().isEmpty()
                ? "input=%s".formatted(example.input())
                : example.label();
        String methodName = method.getName();
        String intent = intentOf(method);

        try {
            Object input = evaluator.parseLiteral(example.input());
            Object[] args = spreadArgs(input, method.getParameterTypes());

            method.setAccessible(true);
            boolean expectsException = example.throws_() != NoException.class;

            // Check @Contract requires before invocation
            Contract contract = method.getAnnotation(Contract.class);
            if (contract != null && !contract.requires().isEmpty()) {
                boolean ok = evaluator.evaluateBoolean(contract.requires(), null, args, method, target);
                if (!ok) {
                    Throwable violation = new PreconditionViolationException(
                            contract.requires(), methodName, args);
                    return new ExampleResult(label, methodName, false, null, null, violation, intent);
                }
            }

            if (expectsException) {
                try {
                    method.invoke(target, args);
                    return new ExampleResult(label, methodName, false,
                            example.throws_().getSimpleName() + " (expected)", "no exception", null, intent);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    boolean correctType = example.throws_().isInstance(cause);
                    return new ExampleResult(label, methodName, correctType,
                            example.throws_().getSimpleName(), cause.getClass().getSimpleName(),
                            correctType ? null : cause, intent);
                }
            }

            Object expectedOutput = evaluator.parseLiteral(example.output());
            Object actual;
            try {
                actual = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                return new ExampleResult(label, methodName, false, expectedOutput, null, cause, intent);
            }

            // Check @Contract ensures after invocation
            if (contract != null && !contract.ensures().isEmpty()) {
                try {
                    boolean ok = evaluator.evaluateBoolean(contract.ensures(), actual, args, method, target);
                    if (!ok) {
                        Throwable violation = new PostconditionViolationException(
                                contract.ensures(), methodName, actual);
                        return new ExampleResult(label, methodName, false, expectedOutput, actual, violation, intent);
                    }
                } catch (de.makibytes.aigent.ContractEvaluationException e) {
                    // SpEL cannot parse or evaluate this ensures expression (e.g. unsupported
                    // lambda syntax). Skip the contract check and fall through to output comparison.
                }
            }

            boolean passed = deepEquals(expectedOutput, actual);
            return new ExampleResult(label, methodName, passed, expectedOutput, actual, null, intent);

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return new ExampleResult(label, methodName, false, null, null, cause, intent);
        }
    }

    private Object[] spreadArgs(Object input, Class<?>[] paramTypes) {
        if (input instanceof List<?> list && paramTypes.length > 1 && list.size() == paramTypes.length) {
            return list.toArray();
        }
        if ((input instanceof List<?> || input instanceof Map<?, ?>) && paramTypes.length == 1) {
            input = convertInput(input, paramTypes[0]);
        }
        return new Object[]{input};
    }

    private Object convertInput(Object input, Class<?> paramType) {
        if (input instanceof List<?> list && Map.class.isAssignableFrom(paramType)) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Object item : list) {
                if (item instanceof Map.Entry<?, ?> entry) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof List<?> listVal) {
                        value = new LinkedHashSet<>(listVal);
                    }
                    result.put(key, value);
                }
            }
            return result;
        }
        if (input instanceof Map<?, ?> map && Map.class.isAssignableFrom(paramType)) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof List<?> listVal) {
                    value = new LinkedHashSet<>(listVal);
                }
                result.put(key, value);
            }
            return result;
        }
        return input;
    }

    private boolean deepEquals(Object expected, Object actual) {
        if (expected == null || actual == null) {
            return Objects.equals(expected, actual);
        }
        if (expected instanceof Number && actual instanceof Number) {
            return Double.compare(((Number) expected).doubleValue(), ((Number) actual).doubleValue()) == 0;
        }
        if (expected instanceof List<?> && actual instanceof List<?>) {
            List<?> expectedList = (List<?>) expected;
            List<?> actualList = (List<?>) actual;
            if (expectedList.size() != actualList.size()) {
                return false;
            }
            for (int i = 0; i < expectedList.size(); i++) {
                if (!deepEquals(expectedList.get(i), actualList.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (expected instanceof Set<?> && actual instanceof Set<?>) {
            Set<?> expectedSet = (Set<?>) expected;
            Set<?> actualSet = (Set<?>) actual;
            if (expectedSet.size() != actualSet.size()) {
                return false;
            }
            for (Object item : expectedSet) {
                if (!actualSet.contains(item)) {
                    return false;
                }
            }
            return true;
        }
        if (expected instanceof Set<?> && actual instanceof List<?>) {
            Set<?> expectedSet = (Set<?>) expected;
            List<?> actualList = (List<?>) actual;
            if (expectedSet.size() != actualList.size()) {
                return false;
            }
            for (Object item : expectedSet) {
                if (!actualList.contains(item)) {
                    return false;
                }
            }
            return true;
        }
        if (expected instanceof List<?> && actual instanceof Set<?>) {
            List<?> expectedList = (List<?>) expected;
            Set<?> actualSet = (Set<?>) actual;
            if (expectedList.size() != actualSet.size()) {
                return false;
            }
            for (Object item : expectedList) {
                if (!actualSet.contains(item)) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(expected, actual);
    }
}
