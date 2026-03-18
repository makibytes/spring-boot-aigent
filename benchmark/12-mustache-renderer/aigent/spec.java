package de.makibytes.benchmark.mustache;

import de.makibytes.aigent.*;
import java.util.List;
import java.util.Map;

/**
 * Aigent spec for MustacheRendererImpl.
 * Implement the @Stub method. All rules are tested by MustacheRendererTest.
 */
public class MustacheRendererImpl implements MustacheRenderer {

    @Intent("""
            Render a Mustache template string against a data context.

            TAG SYNTAX:
              {{name}}         variable — HTML-escaped
              {{{name}}}       variable — NOT escaped
              {{&name}}        variable — NOT escaped (same as triple-stache)
              {{#name}}...{{/name}}   section
              {{^name}}...{{/name}}   inverted section (renders when name is falsy)
              {{! comment }}   comment — produces no output

            VARIABLE LOOKUP:
              - Split name on '.' to navigate nested maps: {{a.b}} → ctx.get("a").get("b")
              - {{.}} refers to the current context item (used in list iteration)
              - If any segment is missing, produce empty string (no error)

            HTML ESCAPING (applied to {{name}}, NOT to {{{name}}} or {{&name}}):
              & → &amp;    (MUST be escaped FIRST — before < > " — to avoid double-escaping)
              < → &lt;
              > → &gt;
              " → &quot;

            FALSY values (section not rendered; inverted section IS rendered):
              - null or missing key
              - Boolean.FALSE
              - empty List (size 0)
              - empty String ("")

            TRUTHY values (section rendered; inverted section NOT rendered):
              - Boolean.TRUE              → render section body once
              - non-empty List            → render section body once per element
              - Map<String,Object>        → render section body once, map becomes context
              - any other non-null value  → render section body once, current context unchanged
              - IMPORTANT: 0 (integer zero) is TRUTHY in Mustache

            LIST ITERATION:
              When the section value is a non-empty List, render the body once per element.
              Each element becomes the context for that iteration:
                - If element is a Map: use that map as context
                - Otherwise: {{.}} renders the element as a string; other keys fall through
                  to the parent context

            STANDALONE TAG REMOVAL:
              A tag is "standalone" if it is the only non-whitespace content on its line.
              Standalone tags: {{#name}}, {{/name}}, {{^name}}, {{! comment }}
              A standalone tag's entire line (including leading/trailing whitespace AND the
              terminating newline character) is removed from the output.

              Example — template (| marks line start):
                |before
                |  {{#show}}
                |content
                |  {{/show}}
                |after
              Output:
                |before
                |content
                |after
              NOT (wrong — leaves blank lines):
                |before
                |
                |content
                |
                |after

              Detection: after stripping leading/trailing whitespace from a line, if what
              remains is exactly one tag ({{#...}}, {{/...}}, {{^...}}, {{!...}}), the
              whole line + newline is standalone.

              If a section tag shares a line with other non-whitespace content, it is NOT
              standalone; surrounding whitespace is preserved.

            FALSY SECTION + STANDALONE:
              When a section is falsy its body is skipped, but the standalone open and
              close tags still consume their lines. This is the most commonly missed case:
              template "A\\n{{#s}}\\nB\\n{{/s}}\\nC" with s=false should render "A\\nC",
              not "A\\n\\nC".
            """)
    @Contract(requires = "template != null && context != null")

    // ── Variables ──────────────────────────────────────────────────────────────
    @Example(label = "variable basic",      input = "\"Hello, {{name}}!\" | {name: World}",              output = "Hello, World!")
    @Example(label = "variable missing",    input = "\"{{x}}\" | {}",                                    output = "")
    @Example(label = "variable escaped",    input = "\"{{v}}\" | {v: <b>bold</b>}",                      output = "&lt;b&gt;bold&lt;/b&gt;")
    @Example(label = "amp escaped first",   input = "\"{{v}}\" | {v: a & b}",                            output = "a &amp; b")
    @Example(label = "triple unescaped",    input = "\"{{{v}}}\" | {v: <b>bold</b>}",                    output = "<b>bold</b>")
    @Example(label = "ampersand unescaped", input = "\"{{&v}}\" | {v: <b>bold</b>}",                     output = "<b>bold</b>")
    @Example(label = "dot notation",        input = "\"{{a.b}}\" | {a: {b: deep}}",                      output = "deep")
    @Example(label = "dot missing",         input = "\"{{a.b.c}}\" | {}",                                 output = "")

    // ── Sections — falsy ───────────────────────────────────────────────────────
    @Example(label = "section false",       input = "\"{{#s}}yes{{/s}}\" | {s: false}",                  output = "")
    @Example(label = "section missing",     input = "\"{{#s}}yes{{/s}}\" | {}",                           output = "")
    @Example(label = "section empty list",  input = "\"{{#s}}yes{{/s}}\" | {s: []}",                     output = "")
    @Example(label = "section empty string",input = "\"{{#s}}yes{{/s}}\" | {s: \"\"}",                   output = "")
    @Example(label = "section zero truthy", input = "\"{{#s}}yes{{/s}}\" | {s: 0}",                      output = "yes")

    // ── Sections — list ────────────────────────────────────────────────────────
    @Example(label = "section list maps",   input = "\"{{#p}}{{name}} {{/p}}\" | {p: [{name:Alice},{name:Bob}]}", output = "Alice Bob ")
    @Example(label = "section list strings",input = "\"{{#i}}{{.}},{{/i}}\" | {i: [a,b,c]}",             output = "a,b,c,")

    // ── Inverted sections ──────────────────────────────────────────────────────
    @Example(label = "inverted false",      input = "\"{{^s}}no{{/s}}\" | {s: false}",                   output = "no")
    @Example(label = "inverted truthy",     input = "\"{{^s}}no{{/s}}\" | {s: yes}",                     output = "")

    // ── Comment ────────────────────────────────────────────────────────────────
    @Example(label = "comment",             input = "\"a{{! ignored }}b\" | {}",                          output = "ab")

    // ── Standalone tag removal ─────────────────────────────────────────────────
    @Example(label = "standalone open",    input = "\"before\\n{{#s}}\\ncontent\\n{{/s}}\\nafter\" | {s:true}", output = "before\\ncontent\\nafter")
    @Example(label = "standalone comment", input = "\"before\\n{{! c }}\\nafter\" | {}",                  output = "before\\nafter")
    @Example(label = "standalone falsy",   input = "\"A\\n{{#s}}\\nB\\n{{/s}}\\nC\" | {s:false}",        output = "A\\nC")
    @Example(label = "non-standalone",     input = "\"x {{#s}} y {{/s}} z\" | {s:true}",                 output = "x  y  z")

    @Stub("""
            Implementation plan:

            1. TOKENIZER: scan the template for {{ and }} delimiters.
               Before parsing, detect standalone tags on each line (see below).
               Produce a token list: TEXT, VARIABLE, TRIPLE, AMP_VAR, OPEN, CLOSE, INVERTED, COMMENT.

            2. STANDALONE PRE-PASS (simplest approach: process line by line):
               For each line, check if stripping leading/trailing whitespace leaves exactly
               one tag token of type OPEN, CLOSE, INVERTED, or COMMENT.
               If yes: mark that tag as standalone, and mark the entire line (including \\n) for removal.

            3. RENDER (recursive, context stack):
               render(tokens, contextStack) → StringBuilder
               - TEXT: append as-is (unless the whole-line removal applies)
               - VARIABLE: resolve key via contextStack, HTML-escape, append
               - TRIPLE / AMP_VAR: resolve key, NO escaping, append
               - COMMENT: produce no output
               - OPEN (section tag):
                   Find matching CLOSE token (handle nesting with depth counter)
                   Resolve value from contextStack
                   If falsy: skip inner tokens (but consume standalone lines)
                   If true/scalar: render inner tokens with current contextStack
                   If Map: render inner tokens with map pushed onto contextStack
                   If non-empty List: render inner tokens once per element,
                                      push element onto contextStack each iteration
               - INVERTED: render inner tokens iff value is falsy

            4. CONTEXT RESOLUTION (resolveKey(key, contextStack)):
               - If key == ".": return top of contextStack
               - Split on '.'; navigate from top of stack downward through segments
               - For each segment: if current context is a Map, look up segment; else null
               - If any lookup yields null: return null (renders as "")
               - Search ALL contexts in stack (innermost first) for the top-level key

            5. HTML ESCAPING — escape & FIRST:
               s = s.replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;")
                 .replace("\\"", "&quot;")

            6. COMMON MISTAKES:
               a. Standalone tag removal: not removing the line → blank lines in output
               b. Standalone applies even when section is FALSY: A\\n{{#s}}\\nB\\n{{/s}}\\nC
                  with s=false should be "A\\nC", not "A\\n\\nC"
               c. 0 (integer zero) treated as falsy — 0 is TRUTHY in Mustache
               d. Empty string treated as truthy — "" is FALSY in Mustache
               e. & escaped AFTER < or > → &amp; getting double-escaped to &amp;amp;
               f. {{.}} not implemented for list iteration of primitives
               g. Non-standalone tag (inline) having its surrounding whitespace removed
               h. Context lookup not searching ancestor stack frames for parent keys
                  (e.g., {{title}} inside a list section should find title from parent)
            """)
    @Override
    public String render(String template, Map<String, Object> context) {
        throw new UnsupportedOperationException();
    }
}
