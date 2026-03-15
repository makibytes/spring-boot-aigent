package com.aigent.benchmark.deps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Authoritative tests for DependencyResolver.
 * The implementation under test must be named DependencyResolverImpl in this package.
 */
class DependencyResolverTest {

    private DependencyResolver resolver;

    @BeforeEach
    void setUp() throws Exception {
        resolver = (DependencyResolver) Class
                .forName("com.aigent.benchmark.deps.DependencyResolverImpl")
                .getDeclaredConstructor()
                .newInstance();
    }

    private static Map.Entry<String, Set<String>> node(String name, String... deps) {
        return Map.entry(name, new LinkedHashSet<>(List.of(deps)));
    }

    @SafeVarargs
    private static Map<String, Set<String>> graph(Map.Entry<String, Set<String>>... entries) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : entries) {
            graph.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        return graph;
    }

    @Test
    void emptyGraph() {
        assertThat(resolver.resolve(Map.of())).isEmpty();
    }

    @Test
    void singlePackage() {
        assertThat(resolver.resolve(graph(node("app")))).containsExactly("app");
    }

    @Test
    void linearChain() {
        assertThat(resolver.resolve(graph(
                node("app", "service"),
                node("service", "core"),
                node("core")
        ))).containsExactly("core", "service", "app");
    }

    @Test
    void diamondDependency() {
        assertThat(resolver.resolve(graph(
                node("app", "api", "cli"),
                node("api", "core"),
                node("cli", "core"),
                node("core")
        ))).containsExactly("core", "api", "cli", "app");
    }

    @Test
    void alphabeticalOrderForIndependentPackages() {
        assertThat(resolver.resolve(graph(
                node("zeta"),
                node("alpha"),
                node("mid")
        ))).containsExactly("alpha", "mid", "zeta");
    }

    @Test
    void alphabeticalOrderWhenNodesUnlockTogether() {
        assertThat(resolver.resolve(graph(
                node("api", "core"),
                node("cli", "core"),
                node("core")
        ))).containsExactly("core", "api", "cli");
    }

    @Test
    void deterministicAcrossDisconnectedComponents() {
        assertThat(resolver.resolve(graph(
                node("ui", "web"),
                node("web"),
                node("cli", "core"),
                node("core")
        ))).containsExactly("core", "cli", "web", "ui");
    }

    @Test
    void inputMapOrderDoesNotChangeResult() {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        dependencies.put("service", Set.of("core"));
        dependencies.put("app", Set.of("service", "web"));
        dependencies.put("web", Set.of("core"));
        dependencies.put("core", Set.of());

        assertThat(resolver.resolve(dependencies)).containsExactly("core", "service", "web", "app");
    }

    @Test
    void sharedDependencyAppearsOnlyOnce() {
        List<String> order = resolver.resolve(graph(
                node("app", "api", "web"),
                node("api", "core"),
                node("web", "core"),
                node("core")
        ));

        assertThat(order).containsExactly("core", "api", "web", "app");
        assertThat(order.stream().filter("core"::equals).count()).isEqualTo(1);
    }

    @Test
    void packageMayDependOnManyPackages() {
        assertThat(resolver.resolve(graph(
                node("app", "api", "db", "web"),
                node("api", "core"),
                node("db", "core"),
                node("web", "core"),
                node("core")
        ))).containsExactly("core", "api", "db", "web", "app");
    }

    @Test
    void deepChain() {
        assertThat(resolver.resolve(graph(
                node("e", "d"),
                node("d", "c"),
                node("c", "b"),
                node("b", "a"),
                node("a")
        ))).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void emptyDependencySetsAreValid() {
        assertThat(resolver.resolve(graph(
                node("api"),
                node("db"),
                node("web")
        ))).containsExactly("api", "db", "web");
    }

    @Test
    void returnsEveryPackageExactlyOnce() {
        List<String> order = resolver.resolve(graph(
                node("billing", "core", "db"),
                node("core"),
                node("db"),
                node("gateway", "core")
        ));

        assertThat(order).containsExactly("core", "db", "billing", "gateway");
        assertThat(new LinkedHashSet<>(order)).hasSize(order.size());
    }

    @Test
    void doesNotMutateInput() {
        Map<String, Set<String>> input = graph(
                node("app", "service"),
                node("service", "core"),
                node("core")
        );
        Map<String, Set<String>> snapshot = new LinkedHashMap<>();
        input.forEach((key, value) -> snapshot.put(key, new LinkedHashSet<>(value)));

        resolver.resolve(input);

        assertThat(input).isEqualTo(snapshot);
    }

    @Test
    void nullInputThrows() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void missingDirectDependencyThrows() {
        assertThatThrownBy(() -> resolver.resolve(graph(
                node("app", "core")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingTransitiveDependencyThrows() {
        assertThatThrownBy(() -> resolver.resolve(graph(
                node("app", "service"),
                node("service", "core")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selfDependencyThrows() {
        assertThatThrownBy(() -> resolver.resolve(graph(
                node("app", "app")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void simpleCycleThrows() {
        assertThatThrownBy(() -> resolver.resolve(graph(
                node("app", "service"),
                node("service", "app")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longerCycleThrows() {
        assertThatThrownBy(() -> resolver.resolve(graph(
                node("a", "b"),
                node("b", "c"),
                node("c", "d"),
                node("d", "b")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void largerGraphStillDeterministic() {
        Map<String, Set<String>> graph = graph(
                node("app", "auth", "ui"),
                node("auth", "core", "db"),
                node("ui", "core"),
                node("db", "core"),
                node("core"),
                node("metrics", "core"),
                node("ops", "metrics")
        );

        assertThat(resolver.resolve(graph)).containsExactly(
                "core", "db", "auth", "metrics", "ops", "ui", "app"
        );
    }
}
