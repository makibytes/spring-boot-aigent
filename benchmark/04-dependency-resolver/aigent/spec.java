package com.aigent.benchmark.deps;

import de.makibytes.aigent.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aigent spec for DependencyResolverImpl.
 */
public class DependencyResolverImpl implements DependencyResolver {

    @Intent("""
            Resolve a package dependency graph into a deterministic installation order.

            Requirements:
              - Return every package key from the input map exactly once.
              - Every dependency must appear before the package that depends on it.
              - If multiple packages become installable at the same time, choose them in
                alphabetical order so the output is deterministic.
              - Reject invalid graphs:
                  * missing dependency names that are not present as keys in the input map
                  * self-dependencies
                  * cycles of any length
              - Do not mutate the input map or its sets.

            A standard Kahn topological sort with alphabetical tie-breaking is ideal.
            """)
    @Contract(
        requires = "dependencies != null",
        ensures  = "$result.size() == dependencies.size() and every key from dependencies appears exactly once in $result and every dependency appears before each dependent",
        throws_  = {NullPointerException.class, IllegalArgumentException.class}
    )
    @Example(label = "empty", input = "{}", output = "[]")
    @Example(label = "single", input = "{app -> []}", output = "[app]")
    @Example(label = "linear chain", input = "{app -> [service], service -> [core], core -> []}", output = "[core, service, app]")
    @Example(label = "diamond", input = "{app -> [api, cli], api -> [core], cli -> [core], core -> []}", output = "[core, api, cli, app]")
    @Example(label = "alphabetical roots", input = "{zeta -> [], alpha -> [], mid -> []}", output = "[alpha, mid, zeta]")
    @Example(label = "alphabetical after unlock", input = "{api -> [core], cli -> [core], core -> []}", output = "[core, api, cli]")
    @Example(label = "missing dependency", input = "{app -> [core]}", output = "throws IllegalArgumentException")
    @Example(label = "cycle", input = "{app -> [service], service -> [app]}", output = "throws IllegalArgumentException")
    @Example(label = "self cycle", input = "{app -> [app]}", output = "throws IllegalArgumentException")
    @Property("$result contains no duplicates")
    @Property("for every edge package -> dependency, indexOf(dependency) < indexOf(package) in $result")
    @Stub("Use an alphabetical ready-queue. Reject missing nodes and cycles explicitly instead of silently dropping them.")
    @Override
    public List<String> resolve(Map<String, Set<String>> dependencies) {
        throw new UnsupportedOperationException();
    }
}
