package de.makibytes.benchmark.css;

import java.util.List;

public interface CssMatcher {

    /**
     * Finds all elements in the HTML fragment that match the CSS selector.
     *
     * <p>All test HTML is simplified and well-formed:
     * <ul>
     *   <li>Attribute values are always single-quoted.</li>
     *   <li>Every element relevant to a test has a unique {@code id} attribute.</li>
     *   <li>No self-closing tags; all elements are explicitly closed.</li>
     *   <li>No attribute values contain the single-quote character.</li>
     * </ul>
     *
     * @param html     simplified well-formed HTML string
     * @param selector CSS selector string
     * @return {@code id} attribute values of matching elements in document order;
     *         empty list if none match
     */
    List<String> select(String html, String selector);
}
