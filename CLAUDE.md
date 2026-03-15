# Aigent Spring Boot Starter

**Artifact:** `com.aigent:aigent-spring-boot-starter:1.0.0-SNAPSHOT`

Contract-Driven Development annotations and Spring Boot auto-configuration for the Aigent
human-AI collaboration paradigm. Add the dependency and every `@Stub`-annotated Spring bean
method will be detected at startup — no configuration needed.

**Non-invasive:** standard Java 21+ annotations with `RUNTIME` retention only.
No language extensions, no annotation processors at build time, no bytecode weaving.

---

## Using the Starter

```xml
<dependency>
    <groupId>com.aigent</groupId>
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
- Expressions are pseudo-Java read by the AI — not evaluated at runtime
- For Spring services, `ensures`/`requires` may reference injected state:
  `"!userRepository.existsById($input)"`

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

## `StubDetector` — Startup Safety Net

The auto-configuration registers a `StubDetector` bean that scans all Spring beans for
`@Stub`-annotated methods after the context is fully loaded.

Configure in `application.properties`:

```properties
# WARN  — log a warning and continue (default; good for development)
# FAIL  — throw IllegalStateException and abort startup (recommended for production profiles)
# IGNORE — disable the detector
aigent.on-stub=WARN
```

Recommended pattern: set `aigent.on-stub=FAIL` in `application-prod.properties` so unimplemented
stubs are caught before any production deployment.

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
src/
├── main/java/com/aigent/
│   ├── Intent.java, Contract.java, Example.java, Examples.java
│   ├── Property.java, Properties.java, Stub.java, Pure.java
│   ├── Confidence.java, AiNote.java
│   └── autoconfigure/
│       ├── AigentAutoConfiguration.java
│       ├── AigentProperties.java
│       └── StubDetector.java
├── main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── test/java/com/aigent/
    ├── AigentAnnotationsTest.java
    └── autoconfigure/StubDetectorTest.java

aigent.java   ← single-file annotation reference (not compiled by Maven)
example.java  ← lifecycle example (not compiled by Maven)
```
