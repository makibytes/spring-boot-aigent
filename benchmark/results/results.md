# Benchmark Results — Final (12 Problems)

**5 models × 12 problems × 2 approaches = 120 runs**

Rating per run: 10 = all pass · 8 = ≤20% fail · 6 = ≤33% fail · 4 = ≤50% fail · 2 = some pass · 0 = none/compile error.
`[T]` = timed out (30 min) but implementation existed — tests still ran.
Max per approach: 12 × 10 = **120 pts**. Combined max: **240 pts**.

---

## Problem Set

| # | Problem | Why it's interesting |
|---|---------|---------------------|
| 01 | Expression Evaluator | Classic recursive descent / operator precedence |
| 02 | SemVer Comparator | Spec-precise version ordering with pre-release rules |
| 03 | CSV Parser | RFC 4180 quoting, escaping, multiline fields |
| 04 | Dependency Resolver | Topological sort, cycle detection |
| 05 | Gitignore Matcher | Pattern language with negation, `**`, anchored vs rooted rules |
| 06 | Unified Diff Applier | Line-addressed patch format, hunk arithmetic |
| 07 | TinyLang Interpreter | Interpreter with closures, first-class functions, recursion |
| 08 | Shell Splitter | POSIX quoting rules, backslash handling, line continuation |
| 09 | Cron Matcher | DOM+DOW OR semantics, `7`=Sunday, step ranges, named fields |
| 10 | TOML Parser | `0x`/`0o`/`0b` integers, `_` separators, `inf`/`nan`, multiline |
| 11 | CSS Selector Engine | 3-component architecture (HTML parser + selector parser + matcher); `:nth-child(An+B)`, combinators, attribute operators |
| 12 | Mustache Renderer | Standalone tag removal, `0` truthy/`""` falsy, HTML escape order, `{{.}}` in iterations |

---

## Overall Scores

| Model | Oneshot | Aigent | Aigent Δ |
|---|---:|---:|---:|
| **Claude Sonnet 4.6** | **120/120** | **120/120** | `0` |
| Big Pickle | 106/120 | 106/120 | `0` |
| MiMo V2 Flash | 114/120 | 106/120 | `−8` |
| MiniMax M2.5 | 108/120 | 98/120 | `−10` |
| Nemotron 3 Super | 94/120 | 82/120 | `−12` |

**Aggregate: oneshot 542 · aigent 512 — oneshot leads by 30 points.**

---

## Per-Problem Scores

### Oneshot

| Problem | Claude 4.6 | Big Pickle | MiniMax M2.5 | MiMo Flash | Nemotron 3 | Sum |
|---|:---:|:---:|:---:|:---:|:---:|---:|
| 01 Expression Eval | 10 | 10 | 10 | 10 | 10 | **50** |
| 02 SemVer Compare | 10 | 10 | 10 | 10 | 10 | **50** |
| 03 CSV Parser | 10 | 10 | 10 | 10 | 10 | **50** |
| 04 Dep Resolver | 10 | 10 | 10 | 10 | 10 | **50** |
| 05 Gitignore Match | 10 | 10 | 10 | 10 | 10 | **50** |
| 06 Unified Diff | 10 | **2** | 10 | 10 | 10 | **42** |
| 07 TinyLang | 10 | 10 | 10 | **8** | **0** | **38** |
| 08 Shell Splitter | 10 | 10 | 10 | 10 | 10 | **50** |
| 09 Cron Matcher | 10 | 10 | 10 | 10 | 10 | **50** |
| 10 TOML Parser | 10 | 10 | 10 | 10 | **8** | **48** |
| 11 CSS Selector | 10 | 10 | **0** | **8** | **2** | **30** |
| 12 Mustache | 10 | **4** | **8** | **8** | **4** | **34** |
| **Total** | **120** | **106** | **108** | **114** | **94** | **542** |

### Aigent (spec-to-prompt compiler)

| Problem | Claude 4.6 | Big Pickle | MiniMax M2.5 | MiMo Flash | Nemotron 3 | Sum |
|---|:---:|:---:|:---:|:---:|:---:|---:|
| 01 Expression Eval | 10 | 10 | 10 | 10 | 10 | **50** |
| 02 SemVer Compare | 10 | 10 | 10 | 10 | 10 | **50** |
| 03 CSV Parser | 10 | 10 | 10 | 10 | 10 | **50** |
| 04 Dep Resolver | 10 | 10 | 10 | 10 | 10 | **50** |
| 05 Gitignore Match | 10 | 10 | 10 | 10 | 10 | **50** |
| 06 Unified Diff | 10 | **10 ✅** | 10 | 10 | 10 | **50** |
| 07 TinyLang | 10 | **[T] 8** | **[T] 8** | **10 ✅** | **0** | **36** |
| 08 Shell Splitter | 10 | 10 | 10 | 10 | 10 | **50** |
| 09 Cron Matcher | 10 | 10 | 10 | 10 | **[T] 0** | **40** |
| 10 TOML Parser | 10 | 10 | 10 | 10 | **8** | **48** |
| 11 CSS Selector | 10 | **8** | **0** | **6** | **0** | **24** |
| 12 Mustache | 10 | **0** | **0** | **0** | **4** | **14** |
| **Total** | **120** | **106** | **98** | **106** | **82** | **512** |

---

## Per-Model Analysis

### Claude Sonnet 4.6 — 120/120 both approaches

Perfect scores across all 12 problems in both approaches. The structured spec adds no signal when
the model already possesses the knowledge needed to implement correctly from a short description.
Aigent often runs slower (more prompt to process) but produces identical results.

### Big Pickle — 106/106 (tied)

The only model where aigent rescued a failing oneshot: Unified Diff scored 1/20 oneshot and 20/20
aigent — the spec's explicit hunk format description fixed a systematic misunderstanding. However
Mustache aigent catastrophically failed (0/35) while oneshot managed 21/35, exactly cancelling the
gain. Net delta: zero. Both approaches hit the same ceiling.

### MiMo V2 Flash — 114 oneshot, 106 aigent (Δ−8)

MiMo's oneshot is the strongest of the three mid-tier opencode models. Aigent helped TinyLang
(42/43 → 43/43) but hurt CSS selector (8 → 6) and destroyed Mustache (29/35 → 0/35). The Mustache
aigent prompt apparently overrode MiMo's correct pre-trained knowledge with a complex algorithmic
description that the model followed incorrectly.

### MiniMax M2.5 — 108 oneshot, 98 aigent (Δ−10)

MiniMax failed CSS selector in both approaches (both timed out without producing compilable code —
the 3-component architecture requires HTML parser + selector parser + matcher, which exceeded the
30-minute limit). Mustache: strong oneshot (34/35) but aigent timed out and produced nothing (0/0).
TinyLang aigent timed out and produced a partial implementation (35/43 vs 43/43 oneshot). Every
aigent regression is a timeout — the longer, more structured prompt consistently pushed MiniMax
over the time limit on the harder problems.

### Nemotron 3 Super — 94 oneshot, 82 aigent (Δ−12)

The weakest model by a significant margin. TinyLang is a hard ceiling for Nemotron: both approaches
failed (0/0 oneshot timeout, killed aigent). Cron matcher aigent timed out completely (0/1) while
oneshot succeeded in ~25 minutes. CSS selector: oneshot got 17/43 (partial) but aigent scored 0/43
(compiled but fully wrong). Mustache is the one exception — Nemotron performed similarly in both
approaches (22/35 oneshot, 23/35 aigent). The aigent prompt length consistently hurts Nemotron
more than it helps.

---

## The Discriminating Problems

### 11 — CSS Selector Engine

The most architecturally complex problem: requires writing an HTML parser, a CSS selector parser,
and a tree-walking matcher — roughly 400–600 lines of Java.

| Model | Oneshot | Aigent | Observation |
|---|:---:|:---:|----|
| Claude 4.6 | 43/43 | 43/43 | Perfect both |
| Big Pickle | 43/43 `[T]` | 41/43 `[T]` | Both timed out but generated working code |
| MiMo Flash | 36/43 | 34/43 | Partial in both; aigent slightly worse |
| MiniMax M2.5 | 0/0 `[T]` | 0/0 | Both timed out with no compilable output |
| Nemotron 3 | 17/43 | 0/43 | Aigent compiled but produced all-wrong output |

**Finding:** The limiting factor is model capability and time budget, not prompt structure. The
detailed aigent spec (including the `:nth-child` An+B formula and combinator semantics) did not
help any model that couldn't already build a CSS engine — and actively hurt Nemotron.

### 12 — Mustache Renderer

Mustache is a widely documented format with extensive training coverage. The aigent spec details
the standalone-tag removal algorithm, the `0`-truthy/`""`-falsy rule, and HTML escape ordering.

| Model | Oneshot | Aigent | Observation |
|---|:---:|:---:|----|
| Claude 4.6 | 35/35 | 35/35 | Perfect both |
| Big Pickle | 21/35 `[T]` | 0/35 `[T]` | Aigent worse — implemented wrong algorithm |
| MiMo Flash | 29/35 | 0/35 | Aigent overrode correct pre-trained knowledge |
| MiniMax M2.5 | 34/35 `[T]` | 0/0 `[T]` | Aigent timed out; outstanding oneshot |
| Nemotron 3 | 22/35 | 23/35 | Similar in both |

**Finding:** Aigent catastrophically *hurt* 3 of 4 opencode models on Mustache. The detailed spec
provided a step-by-step algorithmic description that the models followed literally — overriding
their pre-existing (largely correct) Mustache knowledge and leading them to implement a broken
custom parser. This is the clearest evidence that structured specs can interfere with model
pre-training rather than augmenting it.

---

## Key Findings

**1. Strong models are already saturated.** Claude Sonnet 4.6 scored 100% in both approaches on
all 12 problems. The structured spec adds zero signal.

**2. Aigent can actively hurt on well-known specs.** Mustache is the most dramatic case: 3 of 4
opencode models scored *worse* with aigent (combined score 14 aigent vs 34 oneshot on that problem
alone). The longer a spec, the more likely it overrides correct pre-trained knowledge.

**3. Aigent occasionally rescues a bad oneshot.** Big Pickle's Unified Diff (1/20 → 20/20) is the
clearest rescue. But these wins are rare and are cancelled by the losses at scale.

**4. Timeouts are a hidden cost.** The aigent prompt is materially longer than oneshot. For weaker
models (MiniMax, Nemotron), this consistently pushed runs over the 30-minute limit — sometimes
converting a partial result into a total failure.

**5. The ceiling is the model, not the prompt.** Nemotron could not solve TinyLang in either
approach; neither Nemotron nor MiniMax could produce a working CSS selector engine regardless of
how detailed the brief was. Model capability is the binding constraint.

---

## Conclusion

> **One-shotting AI prompts is not worse than structured prompt engineering with requirements,
> examples, and specifications — and with strong models it is often better.**

After 120 runs across 12 problems and 5 models (542 oneshot points vs 512 aigent points), the
structured aigent approach did not outperform the plain one-shot in aggregate. The model's
pre-trained knowledge is frequently more reliable than a hand-crafted spec, and a shorter prompt
leaves the model room to apply that knowledge without interference.

The annotation framework (`@Intent`, `@Contract`, `@Example`, `@Property`) remains genuinely
useful as **living documentation and runtime enforcement** — just not as a lever for extracting
better code from a model that doesn't already know how to solve the problem.
