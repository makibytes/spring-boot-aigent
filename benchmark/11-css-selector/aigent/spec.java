package de.makibytes.benchmark.css;

import de.makibytes.aigent.*;
import java.util.List;

/**
 * Aigent spec for CssMatcherImpl.
 * Implement the @Stub method. All rules are tested by CssMatcherTest.
 */
public class CssMatcherImpl implements CssMatcher {

    @Intent("""
            Parse a simplified HTML string into a DOM tree, then evaluate a CSS
            selector against that tree. Return the 'id' attribute values of all
            matching elements in document order.

            HTML FORMAT (all test HTML conforms to this):
              - Tags: <tag attr1='v1' attr2='v2'>children</tag>
              - Attribute values are always single-quoted
              - id and class attributes follow their standard semantics
              - class value is a space-separated list of words
              - Always well-formed; every relevant element has a unique id

            SELECTOR TYPES:
              tag         type selector  (div, p, span; * matches everything)
              #id         id selector    (matches id attribute exactly)
              .cls        class selector (class list contains the word 'cls')
              .a.b        compound class (element must have BOTH classes)
              [attr]      has attribute
              [attr=v]    attribute equals v exactly
              [attr~=v]   attribute is space-separated word list containing v
              [attr|=v]   attribute equals v OR starts with v- (hyphen prefix)
              [attr^=v]   attribute starts with v
              [attr$=v]   attribute ends with v
              [attr*=v]   attribute contains v as substring
              :first-child, :last-child, :only-child
              :nth-child(An+B)   see formula below
              :not(S)     element does NOT match simple selector S

            COMBINATORS:
              A B         descendant (B anywhere inside A, any depth)
              A > B       child (B is a direct child of A)
              A + B       adjacent sibling (B immediately follows A, same parent)
              A ~ B       general sibling (B follows A, same parent, not necessarily adjacent)

            SELECTOR LIST:
              sel1, sel2  union — no duplicates in result

            :nth-child FORMULA (An+B), n = 0, 1, 2, …:
              Position k (1-indexed) matches iff ∃ integer n ≥ 0: A·n + B = k
              - 'odd'  = 2n+1 → positions 1, 3, 5, …
              - 'even' = 2n   → positions 2, 4, 6, …
              - :nth-child(3)  = only position 3          (A=0)
              - :nth-child(3n+1) → positions 1, 4, 7, …  (A=3, B=1)
              - :nth-child(2n) → positions 2, 4, 6, …    (n=0 gives 0, invalid; first match n=1→2)
              - :nth-child(-n+3) → positions 1, 2, 3      (A=-1, B=3; n=0→3, n=1→2, n=2→1)
            """)
    @Contract(requires = "html != null && selector != null")

    // ── Type / universal ───────────────────────────────────────────────────────
    @Example(label = "type selector",     input = "\"<div id='a'><p id='b'></p></div>\" | \"p\"",           output = "[b]")
    @Example(label = "universal",         input = "\"<div id='a'><p id='b'></p></div>\" | \"*\"",           output = "[a, b]")
    @Example(label = "type no match",     input = "\"<div id='a'></div>\" | \"p\"",                         output = "[]")

    // ── ID / class ─────────────────────────────────────────────────────────────
    @Example(label = "id selector",       input = "\"<div id='a'><p id='b'></p></div>\" | \"#b\"",          output = "[b]")
    @Example(label = "class selector",    input = "\"<p id='a' class='foo'></p><p id='b'></p>\" | \".foo\"",output = "[a]")
    @Example(label = "compound class",    input = "\"<p id='a' class='foo bar'></p><p id='b' class='foo'></p>\" | \".foo.bar\"", output = "[a]")
    @Example(label = "class no partial",  input = "\"<p id='a' class='foobar'></p>\" | \".foo\"",           output = "[]")

    // ── Attribute selectors ───────────────────────────────────────────────────
    @Example(label = "attr exists",       input = "\"<a id='a' href='x'></a><span id='b'></span>\" | \"[href]\"",    output = "[a]")
    @Example(label = "attr equals",       input = "\"<a id='a' href='foo'></a><a id='b' href='bar'></a>\" | \"[href='foo']\"", output = "[a]")
    @Example(label = "attr word ~=",      input = "\"<p id='a' class='foo bar'></p><p id='b' class='foobar'></p>\" | \"[class~='foo']\"", output = "[a]")
    @Example(label = "attr dash |=",      input = "\"<p id='a' lang='en'></p><p id='b' lang='en-US'></p><p id='c' lang='english'></p>\" | \"[lang|='en']\"", output = "[a, b]")
    @Example(label = "attr starts ^=",    input = "\"<a id='a' href='https://a'></a><a id='b' href='http://b'></a>\" | \"[href^='https']\"", output = "[a]")
    @Example(label = "attr ends $=",      input = "\"<a id='a' href='pic.png'></a><a id='b' href='pic.jpg'></a>\" | \"[href$='.png']\"", output = "[a]")
    @Example(label = "attr contains *=",  input = "\"<a id='a' href='/user/x'></a><a id='b' href='/home'></a>\" | \"[href*='user']\"", output = "[a]")

    // ── Structural pseudo-classes ─────────────────────────────────────────────
    @Example(label = ":first-child",      input = "\"<ul><li id='a'></li><li id='b'></li></ul>\" | \"li:first-child\"",      output = "[a]")
    @Example(label = ":last-child",       input = "\"<ul><li id='a'></li><li id='b'></li></ul>\" | \"li:last-child\"",       output = "[b]")
    @Example(label = ":only-child",       input = "\"<div><p id='a'></p></div><div><p id='b'></p><p id='c'></p></div>\" | \"p:only-child\"", output = "[a]")
    @Example(label = ":nth-child(2)",     input = "\"<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>\" | \"li:nth-child(2)\"",   output = "[b]")
    @Example(label = ":nth-child(odd)",   input = "\"<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>\" | \"li:nth-child(odd)\"",  output = "[a, c]")
    @Example(label = ":nth-child(even)",  input = "\"<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>\" | \"li:nth-child(even)\"", output = "[b, d]")
    @Example(label = ":nth-child(3n+1)",  input = "\"<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li><li id='e'></li><li id='f'></li></ul>\" | \"li:nth-child(3n+1)\"", output = "[a, d]")
    @Example(label = ":nth-child(-n+3)",  input = "\"<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>\" | \"li:nth-child(-n+3)\"",  output = "[a, b, c]")
    @Example(label = ":not(type)",        input = "\"<p id='a'></p><div id='b'></div><p id='c'></p>\" | \":not(p)\"",        output = "[b]")
    @Example(label = ":not(.class)",      input = "\"<p id='a' class='foo'></p><p id='b'></p>\" | \"p:not(.foo)\"",          output = "[b]")
    @Example(label = ":not(:first-child)",input = "\"<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>\" | \"li:not(:first-child)\"", output = "[b, c]")

    // ── Combinators ───────────────────────────────────────────────────────────
    @Example(label = "descendant",        input = "\"<div id='a'><p id='b'><span id='c'></span></p></div>\" | \"div span\"",  output = "[c]")
    @Example(label = "child >",          input = "\"<div id='a'><p id='b'><span id='c'></span></p></div>\" | \"div > p\"",   output = "[b]")
    @Example(label = "child > no deep",  input = "\"<div id='a'><p id='b'><span id='c'></span></p></div>\" | \"div > span\"",output = "[]")
    @Example(label = "adj sibling +",    input = "\"<div><p id='a'></p><span id='b'></span><span id='c'></span></div>\" | \"p + span\"", output = "[b]")
    @Example(label = "gen sibling ~",    input = "\"<div><p id='a'></p><span id='b'></span><span id='c'></span></div>\" | \"p ~ span\"",  output = "[b, c]")

    // ── Compound / selector list ──────────────────────────────────────────────
    @Example(label = "type+class",       input = "\"<p id='a' class='intro'></p><div id='b' class='intro'></div>\" | \"p.intro\"", output = "[a]")
    @Example(label = "selector list",    input = "\"<p id='a'></p><div id='b'></div><span id='c'></span>\" | \"p, div\"",          output = "[a, b]")
    @Example(label = "list no dups",     input = "\"<p id='a' class='foo'></p>\" | \"p, .foo\"",                                   output = "[a]")

    @Stub("""
            Build three components: HTML parser, CSS selector parser, matcher.

            1. HTML PARSER — recursive descent:
               - Scan for <tag attrs>children</tag>
               - Parse attrs: key='value' (single-quoted)
               - Build Node: {tag, id, classes (Set), attrs (Map), parent, children, indexAmongSiblings}
               - indexAmongSiblings is 1-indexed (first child = 1)

            2. SELECTOR PARSER — tokenize into a list of (compound, combinator) pairs:
               - Compound = sequence of simple selectors: tag, #id, .cls, [attr...], :pseudo
               - Combinator between compounds: ' ' (descendant), '>' (child), '+' (adjacent), '~' (general)
               - Selector list: split on ',' (outside brackets/parens), process each independently

            3. MATCHING:
               matchesSimple(node, simple):
                 - '*' or type: compare tag name (case-insensitive)
                 - '#id': compare id attr
                 - '.cls': classes set contains cls
                 - '[attr~=v]': split attr value by whitespace, check contains v as whole word
                 - '[attr|=v]': attr == v OR attr starts with v + '-'
                 - ':nth-child(An+B)': see formula — position 1-indexed among siblings
                 - ':not(S)': !matchesSimple(node, S)

               matchesComplex(node, complexSelector):
                 - rightmost compound must match node
                 - walk left through (compound, combinator) pairs:
                     ' ': node = any ancestor matching compound
                     '>': node = direct parent matching compound
                     '+': node = immediately preceding sibling matching compound
                     '~': node = any preceding sibling matching compound

            4. :nth-child FORMULA (A, B extracted from "An+B" / "odd" / "even" / "N"):
               k matches iff ∃ n ≥ 0: A·n + B = k
               - A=0: k == B
               - A>0: (k-B) >= 0 && (k-B) % A == 0
               - A<0: (B-k) >= 0 && (B-k) % (-A) == 0
               Parse "odd"→A=2,B=1; "even"→A=2,B=0; "n"→A=1,B=0; bare N→A=0,B=N

            5. RESULT: collect all nodes where matchesAny(node, selectors) is true,
               then return their id attributes in document order (preorder traversal).
               Remove duplicates when selector list matches the same element via multiple selectors.

            6. COMMON MISTAKES:
               a. :nth-child using 0-indexed positions — positions START AT 1
               b. :nth-child(-n+3): negative A is valid; positions decrease as n increases
               c. [class~='foo'] matching 'foobar' — must be whole word in space-separated list
               d. [lang|='en'] not matching 'en-US' — must also match val- prefix
               e. '+' matching non-adjacent siblings — only the IMMEDIATELY following sibling
               f. '~' including preceding siblings — only FOLLOWING siblings
               g. Descendant combinator checking only direct parent — must walk ALL ancestors
               h. .foo.bar matching elements with EITHER class — element must have BOTH
               i. Selector list producing duplicates when multiple selectors match same element
            """)
    @Override
    public List<String> select(String html, String selector) {
        throw new UnsupportedOperationException();
    }
}
