package de.makibytes.aigent.autoconfigure;

import de.makibytes.aigent.Pure;
import de.makibytes.aigent.PurityViolationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that {@link Pure}-annotated methods do not mutate their {@link Serializable} arguments.
 * Takes a deep copy of arguments before invocation and compares after.
 */
@Aspect
public class PureAspect {

    private static final Log log = LogFactory.getLog(PureAspect.class);

    @Around("@annotation(pure)")
    public Object checkPurity(ProceedingJoinPoint joinPoint, Pure pure) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String sig = ((MethodSignature) joinPoint.getSignature()).getMethod()
                .getDeclaringClass().getSimpleName() + "#" +
                ((MethodSignature) joinPoint.getSignature()).getMethod().getName() + "()";

        List<Snapshot> snapshots = snapshot(args);

        Object result = joinPoint.proceed();

        for (Snapshot snap : snapshots) {
            if (!deepEquals(snap.copy, args[snap.index])) {
                throw new PurityViolationException(sig,
                        "parameter %d was mutated".formatted(snap.index));
            }
        }

        return result;
    }

    private record Snapshot(int index, Object copy) {}

    private List<Snapshot> snapshot(Object[] args) {
        List<Snapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Serializable s) {
                try {
                    snapshots.add(new Snapshot(i, deepCopy(s)));
                } catch (Exception e) {
                    log.debug("Cannot snapshot parameter %d for purity check: %s"
                            .formatted(i, e.getMessage()));
                }
            }
        }
        return snapshots;
    }

    private Object deepCopy(Serializable obj) throws IOException, ClassNotFoundException {
        var bos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        try (var ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return ois.readObject();
        }
    }

    private boolean deepEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
