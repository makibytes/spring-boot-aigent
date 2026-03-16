package de.makibytes.aigent.autoconfigure;

import de.makibytes.aigent.Pure;
import de.makibytes.aigent.PurityViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PureAspectTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AigentAutoConfiguration.class,
                    AopAutoConfiguration.class));

    @Test
    void pureMethod_noMutation_passes() {
        runner.withUserConfiguration(PureServiceConfig.class).run(ctx -> {
            PureService svc = ctx.getBean(PureService.class);
            List<String> input = new ArrayList<>(List.of("a", "b"));
            assertThat(svc.sorted(input)).isEqualTo(List.of("a", "b"));
            assertThat(input).containsExactly("a", "b"); // unchanged
        });
    }

    @Test
    void pureMethod_mutatesInput_throwsPurityViolation() {
        runner.withUserConfiguration(ImpureServiceConfig.class).run(ctx -> {
            ImpureService svc = ctx.getBean(ImpureService.class);
            List<String> input = new ArrayList<>(List.of("b", "a"));
            assertThatThrownBy(() -> svc.sortInPlace(input))
                    .isInstanceOf(PurityViolationException.class)
                    .hasMessageContaining("parameter 0 was mutated");
        });
    }

    @Test
    void pureCheckDisabled_noEnforcement() {
        runner.withPropertyValues("aigent.pure-check=false")
                .withUserConfiguration(ImpureServiceConfig.class).run(ctx -> {
            assertThat(ctx.containsBean("aigentPureAspect")).isFalse();
        });
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    @Configuration
    @EnableAspectJAutoProxy
    static class PureServiceConfig {
        @Bean PureService pureService() { return new PureService(); }
    }

    static class PureService {
        @Pure
        public List<String> sorted(List<String> input) {
            return input.stream().sorted().toList();
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class ImpureServiceConfig {
        @Bean ImpureService impureService() { return new ImpureService(); }
    }

    static class ImpureService {
        @Pure
        public List<String> sortInPlace(List<String> input) {
            java.util.Collections.sort(input); // mutates!
            return input;
        }
    }
}
