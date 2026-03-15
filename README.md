# Aigent Spring Boot Starter

A lightweight Spring Boot starter for **Contract-Driven Development** — a human-AI collaboration
pattern where you write the *spec* directly in your Java source as annotations, and the AI writes
the *implementation* against it.

Everything lives in the `.java` file itself. No external documents, no separate spec sheets.
Standard Java 21+ annotations with `RUNTIME` retention — no language extensions, no annotation
processors, no bytecode weaving. Works with any Spring Boot 3.x project.

---

## Getting Started

### 1. Build and install the starter

Clone this repository, then install it to your local Maven repository:

```bash
git clone git@github.com:makibytes/spring-boot-aigent.git
cd spring-boot-aigent
mvn install
```

### 2. Add the dependency

In your Spring Boot project's `pom.xml`:

```xml
<dependency>
    <groupId>de.makibytes.aigent</groupId>
    <artifactId>aigent-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

That's it. No `@Enable*` annotation needed — auto-configuration activates automatically.

### 3. Annotate a method you want the AI to implement

Write the spec, mark the method as a stub, and leave the body throwing `UnsupportedOperationException`:

```java
import de.makibytes.aigent.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PhoneService {

    @Intent("Normalize a list of raw phone numbers to E.164 format (+[country][number]).")
    @Contract(
        requires = "numbers != null",
        ensures  = "$result.stream().allMatch(n -> n.startsWith('+'))",
        throws_  = {NullPointerException.class}
    )
    @Example(label = "typical",       input = "['555-0199', '(555) 0123']", output = "['+15550199', '+15550123']")
    @Example(label = "already E.164", input = "['+15550199']",              output = "['+15550199']")
    @Example(label = "empty list",    input = "[]",                         output = "[]")
    @Property("$result.size() == $input.size()")
    @Stub("North American numbers only for now")
    public List<String> normalize(List<String> numbers) {
        throw new UnsupportedOperationException();
    }
}
```

### 4. Run `/aigent` in Claude Code

Open this project in Claude Code and run:

```
/aigent
```

The AI finds every `@Stub`-annotated method, reads the spec, implements the body, removes
`@Stub`, and adds an `@AiNote` with its confidence level and any open questions:

```java
@AiNote(
    confidence = Confidence.HIGH,
    assumed    = "All inputs are North American numbers requiring a +1 country code.",
    open       = "Should already-E.164 inputs be returned unchanged? Currently they are."
)
public List<String> normalize(List<String> numbers) {
    if (numbers == null) throw new NullPointerException("numbers must not be null");
    return numbers.stream()
        .map(n -> n.startsWith("+") ? n : "+1" + n.replaceAll("[^0-9]", ""))
        .collect(java.util.stream.Collectors.toList());
}
```

### 5. Review and iterate

Read the `@AiNote`. If you're satisfied, remove it — done. If something needs changing,
refine the spec annotations and re-add `@Stub` with a note explaining what changed:

```java
@Stub("fix: already-E.164 inputs must be returned unchanged, not re-prefixed")
```

Run `/aigent` again. Repeat until the implementation is correct.

---

## The Annotation Vocabulary

### You write these (spec)

| Annotation | What it expresses |
|---|---|
| `@Intent("…")` | What the method should do, in plain language |
| `@Contract(requires, ensures, throws_)` | Formal precondition, postcondition, and exception behavior |
| `@Example(input, output, label)` | A concrete input→output case — repeatable, all must pass |
| `@Property("…")` | A universal invariant that holds for any valid input — repeatable |
| `@Stub("…")` | Marks the method for AI implementation; the value is your note to the AI |
| `@Pure` | Declares no side effects (for utility methods — not Spring beans) |

### The AI writes this (feedback)

| Annotation | What it expresses |
|---|---|
| `@AiNote(confidence, assumed, open)` | What the AI assumed, how confident it is, and open questions for you |

### `@Contract` expression syntax

```java
@Contract(
    requires = "input != null",                          // precondition
    ensures  = "$result.length() == input.length()",    // postcondition; $result = return value
    throws_  = {IllegalArgumentException.class}         // thrown when requires is violated
)
```

`$result` binds to the return value in `ensures`. `$input` binds to the first parameter.
These are pseudo-Java expressions read by the AI — they are not evaluated at runtime.

---

## The Iteration Loop

```
You write:  @Intent + @Contract + @Example + @Property + @Stub
                              ↓
AI writes:  implementation body · removes @Stub · adds @AiNote
                              ↓
You review: read implementation + @AiNote
            ├─ Happy?       remove @AiNote. Done.
            └─ Not quite?   refine spec + re-add @Stub("what to fix"). Loop.
```

| What you see in the file | What it means |
|---|---|
| `@Stub` present | AI's turn — method needs implementation |
| `@AiNote` present, no `@Stub` | Your turn — review the implementation |
| Neither present | Done — accepted or human-authored |

---

## Startup Safety Net

The starter automatically scans all Spring beans at startup for methods still carrying `@Stub`.
By default it logs a warning. Configure the behavior in `application.properties`:

```properties
# WARN  — log a warning and continue (default, good for development)
aigent.on-stub=WARN

# FAIL  — throw and abort startup (recommended for production)
aigent.on-stub=FAIL

# IGNORE — disable the scan entirely
aigent.on-stub=IGNORE
```

Recommended pattern: set `aigent.on-stub=FAIL` in `application-prod.properties` so an
unimplemented stub can never reach production.

---

## Tips for Writing Good Specs

**`@Intent` is the north star.** Write it first, as if explaining to a colleague. The AI reads
it before anything else. Ambiguous intent leads to ambiguous implementations.

**`@Example` edge cases matter most.** The "happy path" is rarely where bugs hide. Add examples
for empty collections, already-valid inputs, boundary values, and null-adjacent cases.

**`@Property` expresses what's always true.** Use it for invariants that examples can't fully
cover: `"$result.size() == $input.size()"` or `"$result.stream().noneMatch(Objects::isNull)"`.
The AI can turn these into property-based tests.

**`@Contract.requires` is a promise, not a validator.** It says "the caller guarantees this."
`throws_` says what happens if they break the promise. Together they tell the AI whether to
add a guard clause or let the exception propagate naturally.

**`@Stub` value = targeted instruction.** On a re-implementation, put the specific change in
the value: `@Stub("now must handle null elements in the list gracefully")`. The AI reads this
as a constraint narrowing the spec.

**Don't use `@Pure` on Spring beans.** `@Pure` means no side effects at all. Spring
`@Service` and `@Component` methods have injected dependencies and are inherently stateful.
Use `@Pure` only on static utility methods or helper methods with no external access.

---

## Benchmark: Aigent vs One-Shot

We ran a tournament across **3 problems × 2 approaches × 3 LLMs = 18 runs** to measure
whether annotated specs actually improve AI-generated code quality.

**TL;DR — aigent scored 60/60 (100%). One-shot scored 52/60 (87%).**

The gap came from a single problem: the **expression evaluator**, where both Gemini 3 Pro and
Claude Sonnet 4.6 silently produced wrong answers with a vague one-liner prompt —
`2^3^2 = 64` (should be `512`) and `-2^2 = 4` (should be `-4`). Both are plausible-looking
wrong answers that pass any casual test. With the aigent spec's `@Example` annotations spelling
out the correct values and explicitly noting the wrong ones (`// NOT 64.0`), both models
produced correct implementations on the first try.

| Approach | Score | Pass rate |
|----------|------:|----------:|
| **aigent**   | 60/60 | **100%**  |
| one-shot | 52/60 | 87%       |

> See the full results with per-model tables, failure analysis, and timing data:
> **[benchmark/results/results.md](benchmark/results/results.md)**

---

## Project Layout

```
src/main/java/com/aigent/
├── Intent.java            @Intent annotation
├── Contract.java          @Contract annotation
├── Example.java           @Example annotation (repeatable)
├── Examples.java          container for @Example
├── Property.java          @Property annotation (repeatable)
├── Properties.java        container for @Property
├── Stub.java              @Stub annotation
├── Pure.java              @Pure annotation
├── Confidence.java        HIGH / MEDIUM / LOW enum
├── AiNote.java            @AiNote annotation (written by AI)
└── autoconfigure/
    ├── AigentAutoConfiguration.java   Spring Boot auto-configuration
    ├── AigentProperties.java          aigent.on-stub property
    └── StubDetector.java              startup bean scanner
```
