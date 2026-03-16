package de.makibytes.aigent.autoconfigure;

import de.makibytes.aigent.Contract;
import de.makibytes.aigent.PostconditionViolationException;
import de.makibytes.aigent.PreconditionViolationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Spring AOP aspect that enforces {@link Contract} annotations at runtime.
 *
 * <p>Evaluates {@code requires} before method execution and {@code ensures} after.
 * Behavior is controlled by {@link AigentProperties.ContractMode}:
 * <ul>
 *   <li>{@code ENFORCE} — throw on violation (default)
 *   <li>{@code MONITOR} — log violation as warning, continue execution
 *   <li>{@code OFF} — skip all evaluation
 * </ul>
 */
@Aspect
public class ContractAspect {

    private static final Log log = LogFactory.getLog(ContractAspect.class);

    /** Prevents infinite recursion when a contract expression calls methods on the target. */
    private static final ThreadLocal<Boolean> EVALUATING = ThreadLocal.withInitial(() -> false);

    private final AigentProperties properties;
    private final ContractExpressionEvaluator evaluator = new ContractExpressionEvaluator();

    public ContractAspect(AigentProperties properties) {
        this.properties = properties;
    }

    @Around("@annotation(contract)")
    public Object enforceContract(ProceedingJoinPoint joinPoint, Contract contract) throws Throwable {
        if (properties.getContracts() == AigentProperties.ContractMode.OFF || EVALUATING.get()) {
            return joinPoint.proceed();
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        String sig = ContractExpressionEvaluator.methodSignature(method);

        EVALUATING.set(true);
        try {
            // --- Precondition ---
            if (!contract.requires().isEmpty()) {
                boolean satisfied = evaluator.evaluate(
                        contract.requires(), method, target, args, null, false);
                if (!satisfied) {
                    handlePreconditionViolation(contract.requires(), sig, args);
                }
            }
        } finally {
            EVALUATING.set(false);
        }

        Object result = joinPoint.proceed();

        EVALUATING.set(true);
        try {
            // --- Postcondition ---
            if (!contract.ensures().isEmpty()) {
                boolean satisfied = evaluator.evaluate(
                        contract.ensures(), method, target, args, result, true);
                if (!satisfied) {
                    handlePostconditionViolation(contract.ensures(), sig, args, result);
                }
            }
        } finally {
            EVALUATING.set(false);
        }

        return result;
    }

    private void handlePreconditionViolation(String expression, String sig, Object[] args) {
        if (properties.getContracts() == AigentProperties.ContractMode.MONITOR) {
            log.warn("[AIGENT] Precondition violated on %s: \"%s\" | args: %s"
                    .formatted(sig, expression, Arrays.toString(args)));
            log.warn("[AIGENT] Consider re-stubbing: @Stub(\"precondition '%s' violated at runtime\")"
                    .formatted(expression));
        } else {
            throw new PreconditionViolationException(expression, sig, args);
        }
    }

    private void handlePostconditionViolation(String expression, String sig, Object[] args, Object result) {
        if (properties.getContracts() == AigentProperties.ContractMode.MONITOR) {
            log.warn("[AIGENT] Postcondition violated on %s: \"%s\" | result: %s | args: %s"
                    .formatted(sig, expression, result, Arrays.toString(args)));
            log.warn("[AIGENT] Consider re-stubbing: @Stub(\"postcondition '%s' violated at runtime\")"
                    .formatted(expression));
        } else {
            throw new PostconditionViolationException(expression, sig, result);
        }
    }
}
