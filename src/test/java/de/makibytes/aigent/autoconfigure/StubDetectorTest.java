package de.makibytes.aigent.autoconfigure;

import de.makibytes.aigent.Intent;
import de.makibytes.aigent.Stub;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link StubDetector} using Spring Boot's {@link ApplicationContextRunner},
 * which loads the auto-configuration without starting a full server.
 */
class StubDetectorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AigentAutoConfiguration.class));

    @Test
    void stubDetectorBeanIsRegisteredByDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(StubDetector.class));
    }

    @Test
    void noStubs_contextLoadsCleanly() {
        runner.withUserConfiguration(CleanServiceConfig.class)
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void stubPresent_defaultWarnMode_contextLoadsWithWarning() {
        // WARN mode: context loads even with @Stub present
        runner.withUserConfiguration(StubbedServiceConfig.class)
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void stubPresent_failMode_contextFailsToLoad() {
        runner.withUserConfiguration(StubbedServiceConfig.class)
                .withPropertyValues("aigent.on-stub=FAIL")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasMessageContaining("@Stub method")
                            .hasMessageContaining("StubbedService#doWork()");
                });
    }

    @Test
    void stubPresent_ignoreMode_contextLoadsWithoutError() {
        runner.withUserConfiguration(StubbedServiceConfig.class)
                .withPropertyValues("aigent.on-stub=IGNORE")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void stubWithNote_failMode_noteAppearsInMessage() {
        runner.withUserConfiguration(AnnotatedNoteServiceConfig.class)
                .withPropertyValues("aigent.on-stub=FAIL")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasMessageContaining("waiting for telephony lib");
                });
    }

    @Test
    void userCanReplaceStubDetectorWithOwnBean() {
        runner.withUserConfiguration(CustomDetectorConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(StubDetector.class))
                            .isSameAs(ctx.getBean("customDetector"));
                });
    }

    // ── Fixture configurations ────────────────────────────────────────────────

    @Configuration
    static class CleanServiceConfig {
        @Bean
        CleanService cleanService() { return new CleanService(); }
    }

    @Service
    static class CleanService {
        @Intent("Always returns hello.")
        public String greet() { return "hello"; }
    }

    @Configuration
    static class StubbedServiceConfig {
        @Bean
        StubbedService stubbedService() { return new StubbedService(); }
    }

    @Service
    static class StubbedService {
        @Intent("Does work.")
        @Stub
        public void doWork() { throw new UnsupportedOperationException(); }
    }

    @Configuration
    static class AnnotatedNoteServiceConfig {
        @Bean
        NoteService noteService() { return new NoteService(); }
    }

    @Service
    static class NoteService {
        @Stub("waiting for telephony lib")
        public String normalize(String number) { throw new UnsupportedOperationException(); }
    }

    @Configuration
    static class CustomDetectorConfig {
        @Bean("customDetector")
        StubDetector customDetector(
                org.springframework.context.ApplicationContext ctx,
                AigentProperties props) {
            return new StubDetector(ctx, props);
        }
    }
}
