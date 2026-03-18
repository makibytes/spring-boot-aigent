package de.makibytes.benchmark.mustache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for MustacheRenderer.
 *
 * The implementation must be named MustacheRendererImpl in this package.
 */
@SuppressWarnings("unchecked")
class MustacheRendererTest {

    private MustacheRenderer renderer;

    @BeforeEach
    void setUp() throws Exception {
        renderer = (MustacheRenderer) Class
                .forName("de.makibytes.benchmark.mustache.MustacheRendererImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private String render(String template, Map<String, Object> ctx) {
        return renderer.render(template, ctx);
    }

    // ── Variable interpolation ────────────────────────────────────────────────

    @Test void variableBasic() {
        assertThat(render("Hello, {{name}}!", Map.of("name", "World")))
                .isEqualTo("Hello, World!");
    }

    @Test void variableMissingIsEmpty() {
        assertThat(render("{{missing}}", Map.of())).isEqualTo("");
    }

    @Test void variableNumber() {
        assertThat(render("{{n}}", Map.of("n", 42L))).isEqualTo("42");
    }

    /**
     * CRITICAL: {{name}} HTML-escapes &, <, >, ".
     * Escape order matters: & must be escaped FIRST to prevent double-escaping.
     */
    @Test void variableHtmlEscapes() {
        var ctx = Map.<String, Object>of("v", "<b>Tom & \"Jerry\"</b>");
        assertThat(render("{{v}}", ctx))
                .isEqualTo("&lt;b&gt;Tom &amp; &quot;Jerry&quot;&lt;/b&gt;");
    }

    @Test void variableAmpersandEscapedFirst() {
        // If & is not escaped first, "& " → "&amp; " → "&amp;amp; " (double-escape bug)
        assertThat(render("{{v}}", Map.of("v", "a & b")))
                .isEqualTo("a &amp; b");
    }

    /** {{{name}}} renders without HTML escaping. */
    @Test void variableTripleStacheUnescaped() {
        var ctx = Map.<String, Object>of("v", "<b>bold</b>");
        assertThat(render("{{{v}}}", ctx)).isEqualTo("<b>bold</b>");
    }

    /** {{&name}} is equivalent to {{{name}}} — no escaping. */
    @Test void variableAmpersandUnescaped() {
        var ctx = Map.<String, Object>of("v", "<b>bold</b>");
        assertThat(render("{{&v}}", ctx)).isEqualTo("<b>bold</b>");
    }

    // ── Sections — falsy (not rendered) ──────────────────────────────────────

    @Test void sectionFalse() {
        assertThat(render("{{#s}}yes{{/s}}", Map.of("s", false))).isEqualTo("");
    }

    @Test void sectionMissingKey() {
        assertThat(render("{{#s}}yes{{/s}}", Map.of())).isEqualTo("");
    }

    @Test void sectionEmptyList() {
        assertThat(render("{{#s}}yes{{/s}}", Map.of("s", List.of()))).isEqualTo("");
    }

    /**
     * CRITICAL: empty string is FALSY in Mustache.
     * Many models mistakenly treat it as truthy (JavaScript: "".length > 0 is false but "" is falsy).
     */
    @Test void sectionEmptyStringIsFalsy() {
        assertThat(render("{{#s}}yes{{/s}}", Map.of("s", ""))).isEqualTo("");
    }

    // ── Sections — truthy (rendered) ─────────────────────────────────────────

    @Test void sectionTrue() {
        assertThat(render("{{#s}}yes{{/s}}", Map.of("s", true))).isEqualTo("yes");
    }

    /**
     * CRITICAL: zero (0) is TRUTHY in Mustache (unlike JavaScript/Python).
     */
    @Test void sectionZeroIsTruthy() {
        assertThat(render("{{#s}}yes{{/s}}", Map.of("s", 0L))).isEqualTo("yes");
    }

    @Test void sectionNonEmptyString() {
        assertThat(render("{{#s}}yes{{/s}}", Map.of("s", "x"))).isEqualTo("yes");
    }

    // ── Sections — list iteration ─────────────────────────────────────────────

    @Test void sectionListOfMaps() {
        var items = List.<Object>of(
                Map.of("name", "Alice"),
                Map.of("name", "Bob"));
        var ctx = Map.<String, Object>of("people", items);
        assertThat(render("{{#people}}{{name}} {{/people}}", ctx))
                .isEqualTo("Alice Bob ");
    }

    /** {{.}} refers to the current iteration item. */
    @Test void sectionListOfStrings() {
        var ctx = Map.<String, Object>of("items", List.<Object>of("a", "b", "c"));
        assertThat(render("{{#items}}{{.}},{{/items}}", ctx))
                .isEqualTo("a,b,c,");
    }

    @Test void sectionListOfNumbers() {
        var ctx = Map.<String, Object>of("nums", List.<Object>of(1L, 2L, 3L));
        assertThat(render("[{{#nums}}{{.}} {{/nums}}]", ctx))
                .isEqualTo("[1 2 3 ]");
    }

    // ── Sections — map context ────────────────────────────────────────────────

    @Test void sectionMapContext() {
        var ctx = Map.<String, Object>of("person", Map.of("name", "Alice", "age", 30L));
        assertThat(render("{{#person}}{{name}} is {{age}}{{/person}}", ctx))
                .isEqualTo("Alice is 30");
    }

    // ── Inverted sections ─────────────────────────────────────────────────────

    @Test void invertedSectionFalse() {
        assertThat(render("{{^s}}no{{/s}}", Map.of("s", false))).isEqualTo("no");
    }

    @Test void invertedSectionMissing() {
        assertThat(render("{{^s}}no{{/s}}", Map.of())).isEqualTo("no");
    }

    @Test void invertedSectionEmptyList() {
        assertThat(render("{{^s}}no{{/s}}", Map.of("s", List.of()))).isEqualTo("no");
    }

    @Test void invertedSectionTruthy() {
        assertThat(render("{{^s}}no{{/s}}", Map.of("s", "yes"))).isEqualTo("");
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    @Test void commentProducesNoOutput() {
        assertThat(render("before{{! this is a comment }}after", Map.of()))
                .isEqualTo("beforeafter");
    }

    @Test void commentWithNewlines() {
        assertThat(render("a{{! comment\n  spanning lines }}b", Map.of()))
                .isEqualTo("ab");
    }

    // ── Dot notation ──────────────────────────────────────────────────────────

    @Test void dotNotation() {
        var ctx = Map.<String, Object>of("person", Map.of("name", "Alice"));
        assertThat(render("{{person.name}}", ctx)).isEqualTo("Alice");
    }

    @Test void dotNotationDeep() {
        var ctx = Map.<String, Object>of("a", Map.of("b", Map.of("c", "deep")));
        assertThat(render("{{a.b.c}}", ctx)).isEqualTo("deep");
    }

    @Test void dotNotationMissingIntermediateIsEmpty() {
        assertThat(render("{{a.b.c}}", Map.of())).isEqualTo("");
    }

    // ── Nested sections ───────────────────────────────────────────────────────

    @Test void nestedSections() {
        var ctx = Map.<String, Object>of(
                "show", true,
                "inner", true);
        assertThat(render("{{#show}}A{{#inner}}B{{/inner}}C{{/show}}", ctx))
                .isEqualTo("ABC");
    }

    @Test void nestedSectionParentContextAccessible() {
        var items = List.<Object>of(Map.of("name", "x"));
        var ctx = Map.<String, Object>of("title", "T", "items", items);
        assertThat(render("{{#items}}{{title}}:{{name}} {{/items}}", ctx))
                .isEqualTo("T:x ");
    }

    // ── Standalone tag removal ─────────────────────────────────────────────────
    //
    // A "standalone" tag is a section open/close/inverted/comment tag that appears
    // on a line by itself (optionally surrounded by whitespace only).
    // The ENTIRE line — including its trailing newline — is removed from the output.

    /**
     * CRITICAL: {{#section}} on its own line is a standalone tag.
     * The line and its newline are suppressed — it does NOT produce a blank line.
     */
    @Test void standaloneOpenTagLineRemoved() {
        var tmpl = "before\n{{#s}}\ncontent\n{{/s}}\nafter";
        var ctx = Map.<String, Object>of("s", true);
        assertThat(render(tmpl, ctx)).isEqualTo("before\ncontent\nafter");
    }

    @Test void standaloneInvertedTagLineRemoved() {
        var tmpl = "before\n{{^s}}\ncontent\n{{/s}}\nafter";
        assertThat(render(tmpl, Map.of())).isEqualTo("before\ncontent\nafter");
    }

    @Test void standaloneCommentLineRemoved() {
        var tmpl = "before\n{{! comment }}\nafter";
        assertThat(render(tmpl, Map.of())).isEqualTo("before\nafter");
    }

    @Test void standaloneWithLeadingWhitespace() {
        // Leading whitespace before standalone tag is also removed
        var tmpl = "before\n  {{#s}}\ncontent\n  {{/s}}\nafter";
        var ctx = Map.<String, Object>of("s", true);
        assertThat(render(tmpl, ctx)).isEqualTo("before\ncontent\nafter");
    }

    /**
     * CRITICAL: if a section tag is NOT the only non-whitespace on its line
     * (i.e., it is inline), whitespace is preserved and NO line is removed.
     */
    @Test void nonStandaloneTagPreservesWhitespace() {
        var tmpl = "before {{#s}} content {{/s}} after";
        var ctx = Map.<String, Object>of("s", true);
        assertThat(render(tmpl, ctx)).isEqualTo("before  content  after");
    }

    @Test void standaloneTagInFalsySection() {
        // Even when a section is falsy, its standalone tags consume their lines
        var tmpl = "A\n{{#s}}\nB\n{{/s}}\nC";
        assertThat(render(tmpl, Map.of("s", false))).isEqualTo("A\nC");
    }
}
