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
`@Stub`, and adds an `@AiNote` with its confidence level and any open questions.

### 5. Review and iterate

Read the `@AiNote`. If you're satisfied, remove it — done. If something needs changing,
refine the spec annotations and re-add `@Stub` with a note explaining what changed.

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
    requires = "input != null",
    ensures  = "$result.length() == input.length()",
    throws_  = {IllegalArgumentException.class}
)
```

`$result` binds to the return value in `ensures`. `$input` binds to the first parameter.
Expressions use **SpEL (Spring Expression Language)**, evaluated at runtime by `ContractAspect`.

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

| What you see | What it means |
|---|---|
| `@Stub` present | AI's turn — method needs implementation |
| `@AiNote` present, no `@Stub` | Your turn — review the implementation |
| Neither | Done — accepted or human-authored |

---

## Startup Safety Net

The starter scans all Spring beans at startup for methods still carrying `@Stub`.

```properties
aigent.on-stub=WARN    # WARN (default) | FAIL | IGNORE
```

Recommended: set `aigent.on-stub=FAIL` in `application-prod.properties`.

---

## Runtime Contract Enforcement

When `spring-boot-starter-aop` is on the classpath, `ContractAspect` evaluates `@Contract`
expressions at runtime:

```properties
aigent.contracts=ENFORCE    # ENFORCE (default) | MONITOR | OFF
aigent.pure-check=true      # true (default) | false
```

---

## Benchmark: Does Structured Spec Engineering Beat One-Shotting?

> **Short answer: not really — especially with strong models.**

We ran an extensive benchmark to measure whether structured, annotation-driven prompts
(the "aigent" approach) produce better AI-generated code than a plain one-shot description.

### Setup

- **12 problems** spanning algorithmic parsers, interpreters, and format processors
- **5 models**: Claude Sonnet 4.6, MiniMax M2.5, Big Pickle, Nemotron 3 Super, MiMo V2 Flash
- **2 approaches per problem per model**:
  - **Oneshot** — a short, plain-language description of what to implement
  - **Aigent** — a rich prompt compiled from `@Intent`, all `@Example` test cases, `@Contract`
    rules, and `@Stub` implementation notes via `benchmark/spec-to-prompt.py`

Problems range from classic algorithms (expression evaluator, SemVer comparator, CSV parser)
through complex dev-tooling implementations (gitignore matcher, unified diff applier, TinyLang
interpreter, TOML parser) to deliberately tricky problems designed to expose the gap between
approaches (POSIX shell splitter, cron expression matcher, CSS selector engine, Mustache renderer).

### Results

| Model | Oneshot | Aigent | Aigent Δ |
|---|---:|---:|---:|
| Claude Sonnet 4.6 | 120/120 | 120/120 | `0` |
| Big Pickle | 106/120 | 106/120 | `0` |
| MiMo V2 Flash | 114/120 | 106/120 | `−8` |
| MiniMax M2.5 | 108/120 | 98/120 | `−10` |
| Nemotron 3 Super | 94/120 | 82/120 | `−12` |

**Aggregate: oneshot 542 · aigent 512 — oneshot leads by 30 points.**

Rating scale: 10 = all tests pass · 8 = ≤20% fail · 6 = ≤33% fail · 4 = ≤50% fail · 2 = any pass · 0 = none.
`[T]` = hit the 30-minute timeout per run.

### What we found

**Structured prompt engineering does not reliably beat one-shotting**, even on problems
specifically designed to reward careful spec reading. Across 12 problems and 5 models, the
oneshot approach outperformed aigent in aggregate. Several patterns stood out:

**1. Strong models are already saturated.** Claude Sonnet 4.6 scored 100% in both approaches
on all 12 problems. The structured spec adds no signal when the model already possesses the
knowledge needed to implement correctly from a short description.

**2. Aigent can hurt on well-known specs.** For Mustache rendering — a widely documented
format with extensive online coverage — 3 of 4 opencode models scored *worse* with the aigent
prompt than with oneshot. The detailed specification overrode the models' pre-existing (largely
correct) knowledge with a complex algorithmic description that led them astray. MiniMax went
from 34/35 oneshot to 0 compilable tests aigent (timeout); MiMo from 29/35 to 0/35.

**3. Aigent occasionally rescues a bad oneshot.** Big Pickle's Unified Diff applier scored
1/20 oneshot and 20/20 aigent. MiMo's TinyLang went from 42/43 to 43/43. These wins exist,
but they are outnumbered by the losses at scale.

**4. Timeouts are a hidden cost.** Even when aigent produced the same test scores, it
frequently took longer — sometimes hitting the 30-minute ceiling with a slightly worse result
than oneshot completed in minutes. Additional prompt length increases iteration time without
proportional quality gain.

**5. The ceiling is the model, not the prompt.** Nemotron could not solve TinyLang in either
approach; neither Nemotron nor MiniMax could produce a working CSS selector engine regardless
of how detailed the brief was. The limiting factor is model capability, not prompt structure.

### Conclusion

This project set out to test whether codifying requirements as executable annotations and
compiling them into rich implementation prompts would systematically improve AI-generated code
quality. After 120 runs across 12 problems and 5 models (542 oneshot points vs 512 aigent points), the evidence points the other way:

> **One-shotting AI prompts is not worse than structured prompt engineering with
> requirements, examples, and specifications — and with strong models it is often better.**

The model's pre-trained knowledge is frequently more reliable than a hand-crafted spec, and a
shorter prompt leaves the model room to apply that knowledge without interference. The annotation
framework (`@Intent`, `@Contract`, `@Example`, `@Property`) remains genuinely useful as
**living documentation and runtime enforcement** — just not as a lever for extracting
better code from a model that doesn't already know how to solve the problem.

> Full per-problem tables, per-model breakdowns, failure analysis, and timing data:
> **[benchmark/results/results.md](benchmark/results/results.md)**

---

## Running the Benchmark Yourself

```bash
cd benchmark

# All models, all 12 problems, both approaches
./run-benchmark.sh

# One model only
./run-benchmark.sh claude-sonnet-4-6

# One model, one approach
./run-benchmark.sh claude-sonnet-4-6 oneshot

# One model, one approach, one problem
./run-benchmark.sh claude-sonnet-4-6 oneshot 11-css-selector

# Run models in parallel, then merge per-model CSVs
./run-benchmark.sh claude-sonnet-4-6 &
./run-benchmark.sh opencode/big-pickle &
wait
./run-benchmark.sh --merge
```

Registered models (requires the respective CLI tool installed and authenticated):

| Model ID | CLI |
|---|---|
| `claude-sonnet-4-6` | `claude` (Claude Code) |
| `opencode/minimax-m2.5-free` | `opencode` |
| `opencode/big-pickle` | `opencode` |
| `opencode/nemotron-3-super-free` | `opencode` |
| `opencode/mimo-v2-flash-free` | `opencode` |

Each problem directory under `benchmark/` contains:

- `<Interface>.java` — the interface to implement (never modified by the model)
- `<Interface>Test.java` — the authoritative JUnit 5 test suite (never modified by the model)
- `oneshot/prompt.txt` — the plain-language prompt used for the oneshot approach
- `aigent/spec.java` — the annotated spec compiled into the aigent prompt by `spec-to-prompt.py`

Per-run timeout: 30 minutes (override with `MODEL_TIMEOUT=600 ./run-benchmark.sh`).
Results accumulate in `benchmark/results/raw-<model>.csv`; merge with `--merge`.

---

## Project Layout

```
src/main/java/de/makibytes/aigent/
├── Intent.java, Contract.java, Example.java, Examples.java
├── Property.java, Properties.java, Stub.java, Pure.java
├── Confidence.java, AiNote.java
├── PreconditionViolationException.java, PostconditionViolationException.java
├── ContractEvaluationException.java, PurityViolationException.java
├── autoconfigure/
│   ├── AigentAutoConfiguration.java        Spring Boot auto-configuration
│   ├── AigentProperties.java               aigent.* properties
│   ├── ContractAspect.java                 runtime @Contract enforcement (AOP)
│   ├── ContractExpressionEvaluator.java    SpEL evaluation with $ → # translation
│   ├── PureAspect.java                     @Pure input-mutation detection (AOP)
│   └── StubDetector.java                   startup bean scanner
└── testing/
    ├── ExampleRunner.java                  runs @Example annotations as assertions
    ├── AbstractExampleTest.java            JUnit 5 base class
    ├── ContractJUnitExtension.java         proxy-based contract enforcement (no Spring)
    └── PropertyVerifier.java               verifies @Property invariants against samples

benchmark/
├── run-benchmark.sh                        runner (all models × problems × approaches)
├── spec-to-prompt.py                       compiles aigent spec.java → implementation prompt
├── pom-template.xml                        Maven project template for each run directory
├── 01-expression-evaluator/              ┐
├── 02-semver-comparator/                  │
├── 03-csv-parser/                         │
├── 04-dependency-resolver/                │  12 problems, each containing:
├── 05-gitignore-matcher/                  │  - <Interface>.java
├── 06-unified-diff-applier/               │  - <Interface>Test.java
├── 07-tinylang-interpreter/               │  - oneshot/prompt.txt
├── 08-shell-splitter/                     │  - aigent/spec.java
├── 09-cron-matcher/                       │
├── 10-toml-parser/                        │
├── 11-css-selector/                       │
├── 12-mustache-renderer/                 ┘
└── results/
    ├── results.md                          full benchmark findings and analysis
    └── raw.csv                             merged results (model, problem, approach, scores)
```
