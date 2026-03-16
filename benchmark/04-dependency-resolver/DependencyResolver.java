package de.makibytes.benchmark.deps;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves a dependency graph into a deterministic installation order.
 */
public interface DependencyResolver {
    /**
     * Returns every package exactly once in an order where each dependency appears before the
     * package that depends on it.
     *
     * @param dependencies map of package -> direct dependency package names
     * @return deterministic topological order using alphabetical tie-breaking when multiple
     *         packages are ready at the same time
     * @throws NullPointerException if dependencies is null
     * @throws IllegalArgumentException if a dependency is missing or the graph contains a cycle
     */
    List<String> resolve(Map<String, Set<String>> dependencies);
}
