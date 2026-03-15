package com.aigent.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the Aigent starter.
 *
 * <p>Registered automatically via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * No {@code @EnableAigent} annotation is needed — just add the dependency.
 *
 * <p>To disable the starter entirely:
 * <pre>aigent.on-stub=IGNORE</pre>
 *
 * <p>To prevent startup when unimplemented stubs are present (recommended for production):
 * <pre>aigent.on-stub=FAIL</pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(AigentProperties.class)
public class AigentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StubDetector aigentStubDetector(ApplicationContext context, AigentProperties properties) {
        return new StubDetector(context, properties);
    }
}
