package de.makibytes.aigent.autoconfigure;

import de.makibytes.aigent.Contract;
import de.makibytes.aigent.ContractEvaluationException;
import de.makibytes.aigent.PostconditionViolationException;
import de.makibytes.aigent.PreconditionViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static org.assertj.core.api.Assertions.*;

class ContractAspectTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AigentAutoConfiguration.class,
                    AopAutoConfiguration.class));

    // ── ENFORCE mode (default) ──────────────────────────────────────────────

    @Test
    void preconditionPasses_methodExecutesNormally() {
        runner.withUserConfiguration(MathServiceConfig.class).run(ctx -> {
            MathService svc = ctx.getBean(MathService.class);
            assertThat(svc.abs(-5)).isEqualTo(5);
        });
    }

    @Test
    void preconditionFails_throwsPreconditionViolationException() {
        runner.withUserConfiguration(MathServiceConfig.class).run(ctx -> {
            MathService svc = ctx.getBean(MathService.class);
            assertThatThrownBy(() -> svc.divide(10, 0))
                    .isInstanceOf(PreconditionViolationException.class)
                    .hasMessageContaining("divisor != 0");
        });
    }

    @Test
    void postconditionFails_throwsPostconditionViolationException() {
        runner.withUserConfiguration(BuggyServiceConfig.class).run(ctx -> {
            BuggyService svc = ctx.getBean(BuggyService.class);
            assertThatThrownBy(() -> svc.alwaysPositive(-3))
                    .isInstanceOf(PostconditionViolationException.class)
                    .hasMessageContaining("$result > 0");
        });
    }

    @Test
    void postconditionPasses_resultIsReturned() {
        runner.withUserConfiguration(MathServiceConfig.class).run(ctx -> {
            MathService svc = ctx.getBean(MathService.class);
            assertThat(svc.divide(10, 3)).isEqualTo(3);
        });
    }

    // ── MONITOR mode ────────────────────────────────────────────────────────

    @Test
    void monitorMode_preconditionFails_noPreconditionExceptionThrown() {
        runner.withPropertyValues("aigent.contracts=MONITOR")
                .withUserConfiguration(MonitorServiceConfig.class).run(ctx -> {
            MonitorService svc = ctx.getBean(MonitorService.class);
            // Contract logs the violation but doesn't throw PreconditionViolationException.
            // The method still runs and returns the input unchanged.
            assertThat(svc.mustBePositive(-5)).isEqualTo(-5);
        });
    }

    @Test
    void monitorMode_postconditionFails_noPostconditionExceptionThrown() {
        runner.withPropertyValues("aigent.contracts=MONITOR")
                .withUserConfiguration(BuggyServiceConfig.class).run(ctx -> {
            BuggyService svc = ctx.getBean(BuggyService.class);
            assertThat(svc.alwaysPositive(-3)).isEqualTo(-3);
        });
    }

    // ── OFF mode ────────────────────────────────────────────────────────────

    @Test
    void offMode_noEvaluationAtAll() {
        runner.withPropertyValues("aigent.contracts=OFF")
                .withUserConfiguration(MonitorServiceConfig.class).run(ctx -> {
            MonitorService svc = ctx.getBean(MonitorService.class);
            // Precondition would fail, but OFF skips it
            assertThat(svc.mustBePositive(-5)).isEqualTo(-5);
        });
    }

    // ── Named parameters ────────────────────────────────────────────────────

    @Test
    void namedParametersAreAvailable() {
        runner.withUserConfiguration(MathServiceConfig.class).run(ctx -> {
            MathService svc = ctx.getBean(MathService.class);
            assertThat(svc.divide(10, 2)).isEqualTo(5);
        });
    }

    // ── Malformed expression ────────────────────────────────────────────────

    @Test
    void malformedExpression_throwsContractEvaluationException() {
        runner.withUserConfiguration(MalformedServiceConfig.class).run(ctx -> {
            MalformedService svc = ctx.getBean(MalformedService.class);
            assertThatThrownBy(() -> svc.broken("x"))
                    .isInstanceOf(ContractEvaluationException.class);
        });
    }

    // ── $input binding ──────────────────────────────────────────────────────

    @Test
    void inputBindingWorks() {
        runner.withUserConfiguration(StringServiceConfig.class).run(ctx -> {
            StringService svc = ctx.getBean(StringService.class);
            assertThat(svc.toUpper("hello")).isEqualTo("HELLO");
            assertThatThrownBy(() -> svc.toUpper(null))
                    .isInstanceOf(PreconditionViolationException.class);
        });
    }

    // ── Fixture services ────────────────────────────────────────────────────

    @Configuration
    @EnableAspectJAutoProxy
    static class MathServiceConfig {
        @Bean MathService mathService() { return new MathService(); }
    }

    static class MathService {
        @Contract(requires = "$input >= 0 || $input < 0", ensures = "$result >= 0")
        public int abs(int value) {
            return Math.abs(value);
        }

        @Contract(requires = "$divisor != 0", ensures = "$result * $divisor <= $dividend")
        public int divide(int dividend, int divisor) {
            return dividend / divisor;
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class BuggyServiceConfig {
        @Bean BuggyService buggyService() { return new BuggyService(); }
    }

    static class BuggyService {
        @Contract(ensures = "$result > 0")
        public int alwaysPositive(int x) {
            return x; // BUG: doesn't ensure positive
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class MonitorServiceConfig {
        @Bean MonitorService monitorService() { return new MonitorService(); }
    }

    static class MonitorService {
        @Contract(requires = "$input > 0")
        public int mustBePositive(int x) {
            return x; // no side effects, safe to call with invalid input
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class MalformedServiceConfig {
        @Bean MalformedService malformedService() { return new MalformedService(); }
    }

    static class MalformedService {
        @Contract(requires = "$input %%% bogus")
        public String broken(String x) { return x; }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class StringServiceConfig {
        @Bean StringService stringService() { return new StringService(); }
    }

    static class StringService {
        @Contract(requires = "$input != null", ensures = "$result.length() == $input.length()")
        public String toUpper(String s) {
            return s.toUpperCase();
        }
    }
}
