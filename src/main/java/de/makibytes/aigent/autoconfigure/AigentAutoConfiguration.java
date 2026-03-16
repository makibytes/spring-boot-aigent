package de.makibytes.aigent.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the Aigent starter.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 */
@AutoConfiguration
@EnableConfigurationProperties(AigentProperties.class)
public class AigentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StubDetector aigentStubDetector(ApplicationContext context, AigentProperties properties) {
        return new StubDetector(context, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public ContractAspect aigentContractAspect(AigentProperties properties) {
        return new ContractAspect(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(name = "aigent.pure-check", havingValue = "true", matchIfMissing = true)
    public PureAspect aigentPureAspect() {
        return new PureAspect();
    }
}
