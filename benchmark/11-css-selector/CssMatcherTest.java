package de.makibytes.benchmark.css;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Authoritative tests for CssMatcher.
 *
 * HTML uses single-quoted attribute values throughout.
 * Every test element has a unique id; results are checked by id list in document order.
 *
 * The implementation must be named CssMatcherImpl in this package.
 */
class CssMatcherTest {

    private CssMatcher matcher;

    @BeforeEach
    void setUp() throws Exception {
        matcher = (CssMatcher) Class
                .forName("de.makibytes.benchmark.css.CssMatcherImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private List<String> sel(String html, String selector) {
        return matcher.select(html, selector);
    }

    // ── Type selector ─────────────────────────────────────────────────────────

    @Test void typeMatchesByTag() {
        assertThat(sel("<div id='a'><p id='b'>text</p></div>", "p"))
                .containsExactly("b");
    }

    @Test void typeMatchesMultiple() {
        assertThat(sel("<p id='a'></p><p id='b'></p>", "p"))
                .containsExactly("a", "b");
    }

    @Test void typeNoMatch() {
        assertThat(sel("<div id='a'></div>", "p")).isEmpty();
    }

    @Test void universalSelector() {
        assertThat(sel("<div id='a'><p id='b'></p></div>", "*"))
                .containsExactly("a", "b");
    }

    // ── ID selector ───────────────────────────────────────────────────────────

    @Test void idSelector() {
        assertThat(sel("<div id='a'><p id='b'></p></div>", "#b"))
                .containsExactly("b");
    }

    @Test void idSelectorNoMatch() {
        assertThat(sel("<div id='a'></div>", "#b")).isEmpty();
    }

    // ── Class selector ────────────────────────────────────────────────────────

    @Test void classSelector() {
        var html = "<p id='a' class='foo'></p><p id='b' class='bar'></p>";
        assertThat(sel(html, ".foo")).containsExactly("a");
    }

    @Test void classOneOfMultiple() {
        // An element with "foo bar" matches .foo and .bar individually
        var html = "<p id='a' class='foo bar'></p>";
        assertThat(sel(html, ".foo")).containsExactly("a");
        assertThat(sel(html, ".bar")).containsExactly("a");
    }

    /**
     * CRITICAL: .foo.bar means the element must have BOTH classes, not either.
     */
    @Test void compoundClassBothRequired() {
        var html = "<p id='a' class='foo bar'></p><p id='b' class='foo'></p><p id='c' class='bar'></p>";
        assertThat(sel(html, ".foo.bar")).containsExactly("a");
    }

    /**
     * CRITICAL: class selector .foo must match a whole class word.
     * An element with class='foobar' does NOT match .foo.
     */
    @Test void classNoPartialMatch() {
        assertThat(sel("<p id='a' class='foobar'></p>", ".foo")).isEmpty();
    }

    // ── Attribute selectors ───────────────────────────────────────────────────

    @Test void attrExists() {
        var html = "<a id='a' href='http://x.com'></a><span id='b'></span>";
        assertThat(sel(html, "[href]")).containsExactly("a");
    }

    @Test void attrEquals() {
        var html = "<a id='a' href='foo'></a><a id='b' href='bar'></a>";
        assertThat(sel(html, "[href='foo']")).containsExactly("a");
    }

    /**
     * CRITICAL: [attr~=val] matches if the attribute is a space-separated list
     * that contains val as a whole word.  'foobar' does NOT match [class~='foo'].
     */
    @Test void attrWordMatch() {
        var html = "<p id='a' class='foo bar'></p><p id='b' class='foobar'></p>";
        assertThat(sel(html, "[class~='foo']")).containsExactly("a");
    }

    /**
     * CRITICAL: [attr|=val] matches the attribute if it equals val exactly
     * OR if it starts with val followed by a hyphen.  'english' does NOT match [lang|='en'].
     */
    @Test void attrDashPrefix() {
        var html = "<p id='a' lang='en'></p><p id='b' lang='en-US'></p><p id='c' lang='english'></p>";
        assertThat(sel(html, "[lang|='en']")).containsExactly("a", "b");
    }

    @Test void attrStartsWith() {
        var html = "<a id='a' href='https://a.com'></a><a id='b' href='http://b.com'></a>";
        assertThat(sel(html, "[href^='https']")).containsExactly("a");
    }

    @Test void attrEndsWith() {
        var html = "<a id='a' href='pic.png'></a><a id='b' href='pic.jpg'></a>";
        assertThat(sel(html, "[href$='.png']")).containsExactly("a");
    }

    @Test void attrContains() {
        var html = "<a id='a' href='/user/profile'></a><a id='b' href='/home'></a>";
        assertThat(sel(html, "[href*='user']")).containsExactly("a");
    }

    // ── :first-child / :last-child / :only-child ──────────────────────────────

    @Test void firstChild() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>";
        assertThat(sel(html, "li:first-child")).containsExactly("a");
    }

    @Test void lastChild() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>";
        assertThat(sel(html, "li:last-child")).containsExactly("c");
    }

    @Test void onlyChild() {
        var html = "<div><p id='a'></p></div><div><p id='b'></p><p id='c'></p></div>";
        assertThat(sel(html, "p:only-child")).containsExactly("a");
    }

    // ── :nth-child ────────────────────────────────────────────────────────────

    /**
     * CRITICAL: :nth-child is 1-indexed. :nth-child(1) = first child.
     */
    @Test void nthChildExact() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>";
        assertThat(sel(html, "li:nth-child(2)")).containsExactly("b");
    }

    /** :nth-child(odd) = positions 1, 3, 5, … */
    @Test void nthChildOdd() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>";
        assertThat(sel(html, "li:nth-child(odd)")).containsExactly("a", "c");
    }

    /** :nth-child(even) = positions 2, 4, 6, … */
    @Test void nthChildEven() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>";
        assertThat(sel(html, "li:nth-child(even)")).containsExactly("b", "d");
    }

    /** :nth-child(3n+1) = positions 1, 4, 7, … */
    @Test void nthChildFormula() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li>"
                + "<li id='d'></li><li id='e'></li><li id='f'></li></ul>";
        assertThat(sel(html, "li:nth-child(3n+1)")).containsExactly("a", "d");
    }

    /** :nth-child(2n) = positions 2, 4, 6, … (same as even; n=0 gives 0, invalid) */
    @Test void nthChild2n() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>";
        assertThat(sel(html, "li:nth-child(2n)")).containsExactly("b", "d");
    }

    /**
     * CRITICAL: :nth-child(-n+3) selects the first 3 children.
     * A=-1, B=3 → n=0→3, n=1→2, n=2→1 → positions {1,2,3}.
     */
    @Test void nthChildNegative() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>";
        assertThat(sel(html, "li:nth-child(-n+3)")).containsExactly("a", "b", "c");
    }

    // ── :not() ────────────────────────────────────────────────────────────────

    @Test void notType() {
        var html = "<p id='a'></p><div id='b'></div><p id='c'></p>";
        assertThat(sel(html, ":not(p)")).containsExactly("b");
    }

    @Test void notClass() {
        var html = "<p id='a' class='foo'></p><p id='b'></p><p id='c' class='foo'></p>";
        assertThat(sel(html, "p:not(.foo)")).containsExactly("b");
    }

    @Test void notFirstChild() {
        var html = "<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>";
        assertThat(sel(html, "li:not(:first-child)")).containsExactly("b", "c");
    }

    // ── Descendant combinator ─────────────────────────────────────────────────

    @Test void descendantAnyDepth() {
        // span is nested two levels under div — must still be found
        var html = "<div id='a'><p id='b'><span id='c'></span></p></div>";
        assertThat(sel(html, "div span")).containsExactly("c");
    }

    @Test void descendantDoesNotRequireDirectParent() {
        var html = "<div id='outer'><div id='inner'><p id='p'></p></div></div>";
        assertThat(sel(html, "div p")).containsExactly("p");
    }

    // ── Child combinator ──────────────────────────────────────────────────────

    /**
     * CRITICAL: '>' is direct child only. A grandchild does NOT match.
     */
    @Test void childCombinator() {
        var html = "<div id='a'><p id='b'><span id='c'></span></p></div>";
        assertThat(sel(html, "div > p")).containsExactly("b");
        assertThat(sel(html, "div > span")).isEmpty();
    }

    // ── Adjacent sibling combinator ───────────────────────────────────────────

    /**
     * CRITICAL: '+' matches the immediately following sibling only.
     * The second span (id='c') does NOT match 'p + span'.
     */
    @Test void adjacentSibling() {
        var html = "<div><p id='a'></p><span id='b'></span><span id='c'></span></div>";
        assertThat(sel(html, "p + span")).containsExactly("b");
    }

    @Test void adjacentSiblingNoMatch() {
        // 'div + p': the p must immediately follow the div (same parent)
        var html = "<div id='a'></div><span id='b'></span><p id='c'></p>";
        assertThat(sel(html, "div + p")).isEmpty();
    }

    // ── General sibling combinator ────────────────────────────────────────────

    /** '~' matches ALL following siblings, not just the adjacent one. */
    @Test void generalSibling() {
        var html = "<div><p id='a'></p><span id='b'></span><span id='c'></span></div>";
        assertThat(sel(html, "p ~ span")).containsExactly("b", "c");
    }

    @Test void generalSiblingNotPreceding() {
        // '~' only looks AFTER the anchor, not before
        var html = "<div><span id='b'></span><p id='a'></p><span id='c'></span></div>";
        assertThat(sel(html, "p ~ span")).containsExactly("c");
    }

    // ── Chained combinators ───────────────────────────────────────────────────

    @Test void chainedChildCombinators() {
        var html = "<nav><ul id='ul1'><li id='li1'><a id='link1'></a></li></ul></nav>";
        assertThat(sel(html, "nav > ul > li > a")).containsExactly("link1");
    }

    // ── Compound selectors ────────────────────────────────────────────────────

    @Test void typeAndClass() {
        var html = "<p id='a' class='intro'></p><div id='b' class='intro'></div>";
        assertThat(sel(html, "p.intro")).containsExactly("a");
    }

    @Test void typeAndId() {
        assertThat(sel("<div id='main'></div><p id='main'></p>", "div#main"))
                .containsExactly("main");
    }

    @Test void typeAndAttr() {
        var html = "<a id='a' href='#'></a><a id='b'></a><span id='c' href='#'></span>";
        assertThat(sel(html, "a[href]")).containsExactly("a");
    }

    // ── Selector list ─────────────────────────────────────────────────────────

    @Test void selectorList() {
        var html = "<p id='a'></p><div id='b'></div><span id='c'></span>";
        assertThat(sel(html, "p, div")).containsExactly("a", "b");
    }

    /** CRITICAL: selector list must not return duplicates. */
    @Test void selectorListNoDuplicates() {
        var html = "<p id='a' class='foo'></p>";
        assertThat(sel(html, "p, .foo")).containsExactly("a");
    }

    // ── Document order ────────────────────────────────────────────────────────

    @Test void documentOrder() {
        var html = "<div id='a'><p id='b'></p></div><div id='c'></div>";
        assertThat(sel(html, "*")).containsExactly("a", "b", "c");
    }
}
