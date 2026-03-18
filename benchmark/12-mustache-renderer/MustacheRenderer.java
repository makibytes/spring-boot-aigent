package de.makibytes.benchmark.mustache;

import java.util.Map;

public interface MustacheRenderer {

    /**
     * Renders a Mustache template with the given data context.
     *
     * <p>Supported tags:
     * <ul>
     *   <li>{@code {{name}}}        — variable (HTML-escaped)</li>
     *   <li>{@code {{{name}}}}      — variable (unescaped)</li>
     *   <li>{@code {{&name}}}       — variable (unescaped, same as triple-stache)</li>
     *   <li>{@code {{#name}}}…{@code {{/name}}} — section</li>
     *   <li>{@code {{^name}}}…{@code {{/name}}} — inverted section</li>
     *   <li>{@code {{! comment }}}  — comment (no output)</li>
     * </ul>
     *
     * <p>Context values:
     * <ul>
     *   <li>{@code Map<String,Object>} — lookup key in map, use map as section context</li>
     *   <li>{@code List<Object>}       — truthy non-empty, iterate (each element becomes context)</li>
     *   <li>{@code Boolean.FALSE}      — falsy; {@code Boolean.TRUE} renders section once</li>
     *   <li>{@code null} / missing key — falsy</li>
     *   <li>empty {@code List}         — falsy</li>
     *   <li>empty {@code String} ("")  — falsy</li>
     *   <li>Any other value (0, "x", …) — truthy; renders section once with current context</li>
     * </ul>
     *
     * @param template Mustache template string
     * @param context  data context
     * @return rendered string
     */
    String render(String template, Map<String, Object> context);
}
