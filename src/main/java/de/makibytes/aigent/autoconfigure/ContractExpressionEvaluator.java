package de.makibytes.aigent.autoconfigure;

import de.makibytes.aigent.ContractEvaluationException;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Evaluates contract expressions (SpEL) with parameter and result bindings.
 * Shared by {@link ContractAspect} (runtime) and the testing module.
 *
 * <p>Contract expressions use {@code $result}, {@code $input}, and {@code $paramName}
 * as variable references. These are translated to SpEL's {@code #variable} syntax
 * before evaluation.
 */
public class ContractExpressionEvaluator {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAM_NAMES = new DefaultParameterNameDiscoverer();
    private static final Pattern DOLLAR_VAR = Pattern.compile("\\$(\\w+)");

    private final ConcurrentHashMap<String, Expression> cache = new ConcurrentHashMap<>();

    /**
     * Evaluate a boolean contract expression.
     *
     * @param expression the contract expression (using $-prefixed variables)
     * @param method     the method being checked
     * @param target     the object the method is invoked on (root object for SpEL)
     * @param args       the method arguments
     * @param result     the return value (null for preconditions)
     * @param hasResult  true when evaluating a postcondition (result is bound)
     * @return the boolean result of the expression
     */
    public boolean evaluate(String expression, Method method, Object target,
                            Object[] args, Object result, boolean hasResult) {
        String spelExpr = translateVariables(expression);
        try {
            Expression parsed = cache.computeIfAbsent(spelExpr, PARSER::parseExpression);
            EvaluationContext ctx = buildContext(method, target, args, result, hasResult);
            Boolean value = parsed.getValue(ctx, Boolean.class);
            if (value == null) {
                throw new ContractEvaluationException(expression, methodSignature(method),
                        new NullPointerException("Expression evaluated to null, expected boolean"));
            }
            return value;
        } catch (SpelParseException | SpelEvaluationException e) {
            throw new ContractEvaluationException(expression, methodSignature(method), e);
        }
    }

    /**
     * Parse a SpEL literal value (used by {@code @Example} input/output parsing).
     *
     * <p>SpEL single-quoted strings treat {@code \n}, {@code \t}, {@code \r} as literal
     * two-character sequences. This method unescapes them in the resulting String (or
     * recursively in List elements) after SpEL evaluation.
     */
    public Object parseLiteral(String expression) {
        Object value = PARSER.parseExpression(expression).getValue();
        return unescape(value);
    }

    /**
     * Unescape common escape sequences in String values produced by SpEL.
     * If value is a List, recursively unescape all String elements.
     * Otherwise, return as-is.
     */
    private Object unescape(Object value) {
        if (value instanceof String s) {
            // Order matters: handle specific sequences first, then \\ last to avoid double-processing
            s = s.replace("\\n", "\n");
            s = s.replace("\\t", "\t");
            s = s.replace("\\r", "\r");
            s = s.replace("\\\\", "\\");
            return s;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(unescape(item));
            }
            return result;
        }
        return value;
    }

    /**
     * Evaluate a boolean expression with {@code $result} and parameter bindings.
     * Suitable for use outside of Spring AOP (e.g., in {@code ExampleRunner} or
     * {@code ContractJUnitExtension}).
     *
     * @param expression the contract expression (using $-prefixed variables)
     * @param result     the return value (may be null for preconditions)
     * @param args       the method arguments
     * @param method     the method being checked
     * @param target     the object the method is invoked on
     * @return the boolean result of the expression
     */
    public boolean evaluateBoolean(String expression, Object result, Object[] args,
                                   Method method, Object target) {
        boolean hasResult = result != null || expression.contains("$result") || expression.contains("#result");
        return evaluate(expression, method, target, args != null ? args : new Object[0], result, hasResult);
    }

    private EvaluationContext buildContext(Method method, Object target,
                                          Object[] args, Object result, boolean hasResult) {
        StandardEvaluationContext ctx = new StandardEvaluationContext(target);

        // Bind #input to first parameter
        if (args != null && args.length > 0) {
            ctx.setVariable("input", args[0]);
        }

        // Bind named parameters
        String[] paramNames = PARAM_NAMES.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        // Bind #result for postconditions
        if (hasResult) {
            ctx.setVariable("result", result);
        }

        return ctx;
    }

    /**
     * Translates {@code $varName} to SpEL's {@code #varName}.
     */
    static String translateVariables(String expression) {
        return DOLLAR_VAR.matcher(expression).replaceAll("#$1");
    }

    static String methodSignature(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName() + "()";
    }
}
