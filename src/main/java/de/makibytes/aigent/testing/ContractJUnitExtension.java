package de.makibytes.aigent.testing;

import de.makibytes.aigent.Contract;
import de.makibytes.aigent.PostconditionViolationException;
import de.makibytes.aigent.PreconditionViolationException;
import de.makibytes.aigent.autoconfigure.ContractExpressionEvaluator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JUnit 5 extension that enforces {@code @Contract} pre/postconditions on the
 * object under test without requiring Spring Boot or AOP.
 *
 * <p>Usage: annotate your test class with {@code @ExtendWith(ContractJUnitExtension.class)}
 * and wrap your target with {@link #wrap(Object, Class)}.
 *
 * <p>This is automatically applied when extending {@link AbstractExampleTest} —
 * override {@link AbstractExampleTest#enforceContracts()} to return {@code true}.
 */
public class ContractJUnitExtension {

    private final ContractExpressionEvaluator evaluator = new ContractExpressionEvaluator();

    /**
     * Wraps {@code target} in a JDK dynamic proxy that enforces all
     * {@code @Contract} annotations on {@code iface}'s methods.
     *
     * @param target the implementation instance
     * @param iface  the interface to proxy (must be implemented by target)
     */
    @SuppressWarnings("unchecked")
    public <T> T wrap(T target, Class<T> iface) {
        if (!iface.isInterface()) {
            // Can't proxy a class without cglib — return unwrapped and rely on ExampleRunner contract checks
            return target;
        }
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                new ContractHandler(target, evaluator)
        );
    }

    private static class ContractHandler implements InvocationHandler {

        private final Object target;
        private final ContractExpressionEvaluator evaluator;

        ContractHandler(Object target, ContractExpressionEvaluator evaluator) {
            this.target = target;
            this.evaluator = evaluator;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Find @Contract on the implementation class method (not interface method)
            Method implMethod = findImplMethod(method);
            Contract contract = implMethod != null ? implMethod.getAnnotation(Contract.class) : null;
            if (contract == null) contract = method.getAnnotation(Contract.class);

            Object[] safeArgs = args != null ? args : new Object[0];

            if (contract != null && !contract.requires().isEmpty()) {
                boolean ok = evaluator.evaluateBoolean(contract.requires(), null, safeArgs,
                        implMethod != null ? implMethod : method, target);
                if (!ok) {
                    throw new PreconditionViolationException(contract.requires(), method.getName(), safeArgs);
                }
            }

            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }

            if (contract != null && !contract.ensures().isEmpty()) {
                boolean ok = evaluator.evaluateBoolean(contract.ensures(), result, safeArgs,
                        implMethod != null ? implMethod : method, target);
                if (!ok) {
                    throw new PostconditionViolationException(contract.ensures(), method.getName(), result);
                }
            }

            return result;
        }

        private Method findImplMethod(Method ifaceMethod) {
            try {
                return target.getClass().getMethod(ifaceMethod.getName(), ifaceMethod.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }
}
