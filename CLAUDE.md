# Aigent Spring Boot Starter

**Artifact:** `de.makibytes.aigent:aigent-spring-boot-starter:1.0.0-SNAPSHOT`

Contract-Driven Development with **executable contracts** for the Aigent human-AI collaboration
paradigm. Annotations are not just documentation — `@Contract` preconditions and postconditions
are enforced at runtime via Spring AOP + SpEL, `@Example` cases are runnable as tests, and
`@Property` invariants are verifiable against sample inputs.

**Non-invasive:** standard Java 21+ annotations with `RUNTIME` retention only.
No language extensions, no annotation processors at build time, no bytecode weaving.
AOP enforcement activates automatically when `spring-boot-starter-aop` is on the classpath.

---

## Using the Starter

```xml
<dependency>
    <groupId>de.makibytes.aigent</groupId>
    <artifactId>aigent-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Install to local Maven repository first: `mvn install`

---

## Annotation Vocabulary

Two channels of communication live in the same `.java` file:

### Human → AI (Specification)

| Annotation | Purpose |
|---|---|
| `@Intent(String)` | What the method does, in plain language |
| `@Contract` | Formal pre/postconditions and exception behavior |
| `@Example` | Concrete input→output test case — repeatable, all must pass |
| `@Property` | Universal invariant for any valid input — repeatable |
| `@Stub` | Work order: AI must implement this method; re-add to trigger re-implementation |

### AI → Human (Feedback)

| Annotation | Purpose |
|---|---|
| `@AiNote` | AI records confidence level, assumptions made, and open questions for review |

### Shared

| Annotation | Purpose |
|---|---|
| `@Pure` | No side effects, no IO, no random, no external state. **Utility methods only.** |

---

## `@Contract` Expression Syntax

```java
@Contract(
    requires = "numbers != null",                                   // precondition
    ensures  = "$result.stream().allMatch(n -> n.startsWith('+'))", // postcondition
    throws_  = {NullPointerException.class}                         // thrown when requires violated
)
```

- `$result` — binds to the return value in `ensures`
- `$input`  — binds to the first parameter in `requires`/`ensures`
- Named parameters (e.g., `$divisor`, `$name`) are also available as variables
- **Expressions are SpEL (Spring Expression Language)**, evaluated at runtime by `ContractAspect`
- `$variable` syntax is translated to SpEL's `#variable` automatically
- For Spring services, `ensures`/`requires` may reference injected state:
  `"!userRepository.existsById($input)"`
- `throws_` documents what the method throws when callers violate the precondition — not enforced by the aspect

---

## The Iteration Loop

```
Human writes spec (@Intent + @Contract + @Example + @Property + @Stub)
  ↓
AI implements:
  · removes @Stub
  · writes implementation body
  · adds @AiNote (confidence, assumptions, open questions)
  ↓
Human reviews implementation + @AiNote:
  ├─ Satisfied?  → remove @AiNote (or leave as docs). Done.
  └─ Needs work? → refine spec annotations + re-add @Stub("what changed"). Loop.
```

### Lifecycle Signals

| State | Meaning |
|---|---|
| `@Stub` present | Work order: AI must implement (or re-implement) |
| `@AiNote` present, no `@Stub` | Review request: human must inspect the implementation |
| Neither present | Done: human-authored or accepted |

---

## Runtime Contract Enforcement

The auto-configuration registers a `ContractAspect` (Spring AOP) that intercepts all
`@Contract`-annotated methods:

- **`requires`** is evaluated **before** the method executes (precondition)
- **`ensures`** is evaluated **after** the method returns, with `$result` bound (postcondition)
- Violations throw `PreconditionViolationException` / `PostconditionViolationException`
- In MONITOR mode, violations are logged with re-stub suggestions instead of throwing

```properties
aigent.contracts=ENFORCE    # ENFORCE (default) | MONITOR | OFF
```

`@Pure`-annotated methods are checked for input mutation via deep-copy comparison:
```properties
aigent.pure-check=true      # true (default) | false
```

---

## Testing Module (`de.makibytes.aigent.testing`)

### `ExampleRunner` — Run `@Example` Annotations as Tests

```java
var runner = new ExampleRunner();
List<ExampleResult> results = runner.runAll(myServiceInstance);
// Each @Example becomes a concrete input→output assertion
```

### `AbstractExampleTest<T>` — JUnit 5 Integration

```java
class MyServiceTest extends AbstractExampleTest<MyService> {
    @Override protected MyService createTarget() { return new MyService(); }
    @Test void examples() { verifyAll(); } // asserts all @Example annotations pass
}
```

### `PropertyVerifier` — Verify `@Property` Invariants

```java
PropertyVerifier.verify(myService, "average",
    List.of(1.0, 2.0),   // sample input 1
    List.of(5.0),         // sample input 2
    List.of()             // edge case
);
// Checks all @Property expressions against each sample input
```

---

## `StubDetector` — Startup Safety Net

The auto-configuration registers a `StubDetector` bean that scans all Spring beans for
`@Stub`-annotated methods after the context is fully loaded.

```properties
aigent.on-stub=WARN         # WARN (default) | FAIL | IGNORE
```

Recommended: `aigent.on-stub=FAIL` in `application-prod.properties`.

---

## Spring Boot Guidance

- **Do NOT use `@Pure` on `@Service` / `@Component` methods.** Spring beans have injected
  dependencies — they are inherently stateful. `@Pure` is for pure utility methods only.
- The AI infers which beans are available from the class's injected fields. No need to list them.
- `@Contract.ensures` can reference observable state changes:
  ```java
  @Contract(ensures = "!userRepository.existsById($input)")
  ```

---

## Coding Standards

- Use Records for pure data types; avoid manual getters/setters.
- Prefer the Stream API for data transformation unless `@Intent` specifies otherwise.
- Implementations must be readable: a human should verify the `@Intent` → code mapping in 30s.
- The AI must not change spec annotations (`@Intent`, `@Contract`, `@Example`, `@Property`).
  Only `@AiNote` (added/updated) and `@Stub` (removed) are AI-owned.

---

## Commands

| Command | Action |
|---|---|
| `mvn compile` | Compile |
| `mvn test` | Run tests |
| `mvn install` | Build and install to local Maven repository |
| `/aigent` | Find all `@Stub` methods and implement them |

---

## Project Structure

```
src/main/java/de/makibytes/aigent/
├── Intent.java, Contract.java, Example.java, Examples.java
├── Property.java, Properties.java, Stub.java, Pure.java
├── Confidence.java, AiNote.java
├── PreconditionViolationException.java, PostconditionViolationException.java
├── ContractEvaluationException.java, PurityViolationException.java
├── autoconfigure/
│   ├── AigentAutoConfiguration.java, AigentProperties.java
│   ├── ContractAspect.java          ← runtime @Contract enforcement (AOP)
│   ├── ContractExpressionEvaluator.java  ← SpEL evaluation with $ → # translation
│   ├── PureAspect.java              ← @Pure input-mutation detection (AOP)
│   └── StubDetector.java
└── testing/
    ├── ExampleRunner.java            ← runs @Example annotations as assertions
    ├── AbstractExampleTest.java      ← JUnit 5 base class for @Example tests
    └── PropertyVerifier.java         ← verifies @Property invariants against samples

src/test/java/de/makibytes/aigent/
├── AigentAnnotationsTest.java
├── autoconfigure/
│   ├── StubDetectorTest.java
│   ├── ContractAspectTest.java
│   └── PureAspectTest.java
└── testing/
    ├── ExampleRunnerTest.java
    └── PropertyVerifierTest.java
```

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **spring-boot-aigent** (4024 symbols, 8847 relationships, 187 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## When Debugging

1. `gitnexus_query({query: "<error or symptom>"})` — find execution flows related to the issue
2. `gitnexus_context({name: "<suspect function>"})` — see all callers, callees, and process participation
3. `READ gitnexus://repo/spring-boot-aigent/process/{processName}` — trace the full execution flow step by step
4. For regressions: `gitnexus_detect_changes({scope: "compare", base_ref: "main"})` — see what your branch changed

## When Refactoring

- **Renaming**: MUST use `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` first. Review the preview — graph edits are safe, text_search edits need manual review. Then run with `dry_run: false`.
- **Extracting/Splitting**: MUST run `gitnexus_context({name: "target"})` to see all incoming/outgoing refs, then `gitnexus_impact({target: "target", direction: "upstream"})` to find all external callers before moving code.
- After any refactor: run `gitnexus_detect_changes({scope: "all"})` to verify only expected files changed.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Tools Quick Reference

| Tool | When to use | Command |
|------|-------------|---------|
| `query` | Find code by concept | `gitnexus_query({query: "auth validation"})` |
| `context` | 360-degree view of one symbol | `gitnexus_context({name: "validateUser"})` |
| `impact` | Blast radius before editing | `gitnexus_impact({target: "X", direction: "upstream"})` |
| `detect_changes` | Pre-commit scope check | `gitnexus_detect_changes({scope: "staged"})` |
| `rename` | Safe multi-file rename | `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` |
| `cypher` | Custom graph queries | `gitnexus_cypher({query: "MATCH ..."})` |

## Impact Risk Levels

| Depth | Meaning | Action |
|-------|---------|--------|
| d=1 | WILL BREAK — direct callers/importers | MUST update these |
| d=2 | LIKELY AFFECTED — indirect deps | Should test |
| d=3 | MAY NEED TESTING — transitive | Test if critical path |

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/spring-boot-aigent/context` | Codebase overview, check index freshness |
| `gitnexus://repo/spring-boot-aigent/clusters` | All functional areas |
| `gitnexus://repo/spring-boot-aigent/processes` | All execution flows |
| `gitnexus://repo/spring-boot-aigent/process/{name}` | Step-by-step execution trace |

## Self-Check Before Finishing

Before completing any code modification task, verify:
1. `gitnexus_impact` was run for all modified symbols
2. No HIGH/CRITICAL risk warnings were ignored
3. `gitnexus_detect_changes()` confirms changes match expected scope
4. All d=1 (WILL BREAK) dependents were updated

## CLI

- Re-index: `npx gitnexus analyze`
- Check freshness: `npx gitnexus status`
- Generate docs: `npx gitnexus wiki`

<!-- gitnexus:end -->
