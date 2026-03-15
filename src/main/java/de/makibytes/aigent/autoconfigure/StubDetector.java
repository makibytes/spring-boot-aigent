package de.makibytes.aigent.autoconfigure;

import de.makibytes.aigent.Stub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans all Spring beans at startup for methods annotated with {@link Stub} and
 * reacts according to {@link AigentProperties#getOnStub()}:
 * <ul>
 *   <li>{@code WARN}   — logs a warning listing every unimplemented stub (default).
 *   <li>{@code FAIL}   — throws {@link IllegalStateException}, preventing startup.
 *   <li>{@code IGNORE} — does nothing.
 * </ul>
 *
 * <p>Runs after all singletons are instantiated ({@link SmartInitializingSingleton}),
 * before the application accepts requests — catching stubs before they reach production.
 *
 * <p>CGLIB-proxied beans (e.g. {@code @Transactional} services) are handled correctly
 * via {@link ClassUtils#getUserClass(Class)}.
 */
public class StubDetector implements SmartInitializingSingleton {

    private static final Log log = LogFactory.getLog(StubDetector.class);

    private final ApplicationContext context;
    private final AigentProperties properties;

    public StubDetector(ApplicationContext context, AigentProperties properties) {
        this.context = context;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (properties.getOnStub() == AigentProperties.OnStub.IGNORE) {
            return;
        }

        List<String> stubs = findStubMethods();
        if (stubs.isEmpty()) {
            return;
        }

        String message = String.format(
                "Found %d unimplemented @Stub method(s):%n  %s",
                stubs.size(),
                String.join(System.lineSeparator() + "  ", stubs)
        );

        if (properties.getOnStub() == AigentProperties.OnStub.FAIL) {
            throw new IllegalStateException(message);
        }

        log.warn(message);
    }

    private List<String> findStubMethods() {
        List<String> found = new ArrayList<>();

        for (String beanName : context.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = context.getBean(beanName);
            } catch (Exception e) {
                // skip beans that cannot be retrieved (e.g. scoped proxies, prototype beans)
                continue;
            }

            // Unwrap CGLIB/JDK proxies to get the user's actual class
            Class<?> targetClass = ClassUtils.getUserClass(bean.getClass());

            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Stub.class)) {
                    Stub stub = method.getAnnotation(Stub.class);
                    String entry = targetClass.getSimpleName() + "#" + method.getName() + "()";
                    if (!stub.value().isEmpty()) {
                        entry += " [" + stub.value() + "]";
                    }
                    found.add(entry);
                }
            }
        }

        return found;
    }
}
