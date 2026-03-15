package com.aigent;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that all Aigent annotations have correct RUNTIME retention and structural properties.
 * No Spring context needed — pure reflection tests.
 */
class AigentAnnotationsTest {

    @Test
    void allAnnotationsHaveRuntimeRetention() {
        Class<?>[] annotationTypes = {
                Intent.class, Contract.class,
                Example.class, Examples.class,
                Property.class, Properties.class,
                Stub.class, Pure.class, AiNote.class
        };
        for (Class<?> type : annotationTypes) {
            Retention retention = type.getAnnotation(Retention.class);
            assertThat(retention)
                    .as("%s must have @Retention", type.getSimpleName())
                    .isNotNull();
            assertThat(retention.value())
                    .as("%s must have RUNTIME retention", type.getSimpleName())
                    .isEqualTo(RetentionPolicy.RUNTIME);
        }
    }

    @Test
    void exampleIsRepeatableWithExamplesContainer() {
        assertThat(Example.class.isAnnotationPresent(Repeatable.class)).isTrue();
        assertThat(Example.class.getAnnotation(Repeatable.class).value())
                .isEqualTo(Examples.class);
    }

    @Test
    void propertyIsRepeatableWithPropertiesContainer() {
        assertThat(Property.class.isAnnotationPresent(Repeatable.class)).isTrue();
        assertThat(Property.class.getAnnotation(Repeatable.class).value())
                .isEqualTo(Properties.class);
    }

    @Test
    void contractHasThrowsField() throws NoSuchMethodException {
        assertThat(Contract.class.getDeclaredMethod("throws_")).isNotNull();
    }

    @Test
    void aiNoteDefaultsAreMediumConfidenceAndEmptyStrings() throws Exception {
        Method m = AnnotatedMethods.class.getDeclaredMethod("withDefaultAiNote");
        AiNote note = m.getAnnotation(AiNote.class);

        assertThat(note.confidence()).isEqualTo(Confidence.MEDIUM);
        assertThat(note.assumed()).isEmpty();
        assertThat(note.open()).isEmpty();
    }

    @Test
    void stubValueDefaultsToEmptyString() throws Exception {
        Method m = AnnotatedMethods.class.getDeclaredMethod("stubNoNote");
        Stub stub = m.getAnnotation(Stub.class);

        assertThat(stub.value()).isEmpty();
    }

    @Test
    void multipleExamplesAreReadableAtRuntime() throws Exception {
        Method m = AnnotatedMethods.class.getDeclaredMethod("multiExample");
        Example[] examples = m.getAnnotationsByType(Example.class);

        assertThat(examples).hasSize(2);
        assertThat(examples[0].label()).isEqualTo("first");
        assertThat(examples[1].label()).isEqualTo("second");
    }

    @Test
    void fullSpecAnnotationSetIsReadableAtRuntime() throws Exception {
        Method m = AnnotatedMethods.class.getDeclaredMethod("fullSpec", String.class);

        assertThat(m.getAnnotation(Intent.class).value()).isNotBlank();
        assertThat(m.getAnnotation(Contract.class).requires()).isNotBlank();
        assertThat(m.getAnnotation(Contract.class).ensures()).contains("$result");
        assertThat(m.getAnnotationsByType(Example.class)).isNotEmpty();
        assertThat(m.getAnnotationsByType(Property.class)).isNotEmpty();
        assertThat(m.getAnnotation(Stub.class)).isNotNull();
    }

    // ── Fixture methods ──────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    private static class AnnotatedMethods {

        @AiNote
        void withDefaultAiNote() {}

        @Stub
        void stubNoNote() {}

        @Example(label = "first",  input = "'a'", output = "'A'")
        @Example(label = "second", input = "'b'", output = "'B'")
        void multiExample() {}

        @Intent("Uppercase a string.")
        @Contract(requires = "$input != null", ensures = "$result.equals($input.toUpperCase())")
        @Example(label = "basic", input = "'hello'", output = "'HELLO'")
        @Property("$result.length() == $input.length()")
        @Stub
        String fullSpec(String x) { throw new UnsupportedOperationException(); }
    }
}
