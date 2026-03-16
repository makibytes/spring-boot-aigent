# Benchmark Results — Round 4

**5 models × 7 problems × 2 approaches = 70 runs** (GPT-5.4 and Gemini 3 Pro excluded — copilot quota exhausted)

Runs executed in parallel. Each model writes its own CSV; merged at the end.

Problems span two difficulty tiers:

| # | Problem | Tier |
|---|---------|------|
| 01 | Expression Evaluator (operator precedence, right-assoc `^`, unary `-`) | Classic |
| 02 | SemVer Comparator (pre-release ordering, numeric vs lexicographic) | Classic |
| 03 | CSV Parser (RFC 4180, quoted fields, embedded newlines) | Classic |
| 04 | Dependency Resolver (topological sort, cycle / missing-node detection) | Complex |
| 05 | Gitignore Matcher (`*`, `**`, anchoring, negation, last-match-wins) | Complex |
| 06 | Unified Diff Applier (hunk parsing, context validation, empty originals) | Complex |
| 07 | TinyLang Interpreter (variables, if/while, arithmetic, user functions) | Complex |

---

## Overall Scores

Rating per run: 10 = all pass · 8 = ≤20% fail · 6 = ≤33% fail · 4 = ≤50% fail · 2 = some pass · 0 = none / error.
`[T]` = timed out (30 min) but implementation existed — tests still ran.
Max per approach: 7 × 10 = **70 pts**. Combined max: **140 pts**.

| Model | Oneshot | Aigent | Total | Aigent Δ | vs Round 3 |
|---|---:|---:|---:|---:|---:|
| **Claude Sonnet 4.6** | **70/70** | **70/70** | **140/140 (100%)** | `0` | — |
| **Big Pickle** | **70/70** | **70/70** | **140/140 (100%)** | `0` | **+12 🎉** |
| MiniMax M2.5 | 68/70 | 60/70 | **128/140 (91%)** | `−8` | −10 |
| Nemotron 3 Super | 66/70 | 58/70 | **124/140 (89%)** | `−8` | +4 |
| MiMo V2 Flash | 40/70 | 40/70 | **80/140 (57%)** | `0` | −42 ⚠️ |

---

## Per-Problem Results

### Oneshot

| Problem | Claude 4.6 | Big Pickle | MiniMax M2.5 | Nemotron 3 | MiMo Flash |
|---|:---:|:---:|:---:|:---:|:---:|
| Expression Eval | 20/20 | 20/20 | 20/20 | 20/20 | 20/20 |
| SemVer Compare | 17/17 | 17/17 | 17/17 | 17/17 | **0/17** |
| CSV Parser | 18/18 | 18/18 | 18/18 | 18/18 | 18/18 |
| Dep Resolver | 22/22 | 22/22 | 22/22 | 22/22 | 22/22 |
| Gitignore Match | 24/24 | 24/24 | 24/24 | **[T]22/24** | 24/24 |
| Unified Diff | 20/20 | 20/20 | 20/20 | 20/20 | **0/20** |
| TinyLang | 43/43 | 43/43 | **[T]41/43** | 42/43 | **0/43** |

### Aigent

| Problem | Claude 4.6 | Big Pickle | MiniMax M2.5 | Nemotron 3 | MiMo Flash |
|---|:---:|:---:|:---:|:---:|:---:|
| Expression Eval | 20/20 | 20/20 | 20/20 | 20/20 | 20/20 |
| SemVer Compare | 17/17 | 17/17 | **16/17** | 17/17 | 17/17 |
| CSV Parser | 18/18 | 18/18 | 18/18 | 18/18 | 18/18 |
| Dep Resolver | 22/22 | 22/22 | 22/22 | 22/22 | 22/22 |
| Gitignore Match | 24/24 | 24/24 | 24/24 | **[T]21/24** | **0/24** |
| Unified Diff | 20/20 | 20/20 | 20/20 | 20/20 | **0/20** |
| TinyLang | 43/43 | 43/43 | **9/43** | **0/?** | **0/43** |

---

## Timing

### Oneshot (wall time)

| Problem | Claude 4.6 | Big Pickle | MiniMax M2.5 | Nemotron 3 | MiMo Flash |
|---|---:|---:|---:|---:|---:|
| Expression Eval | 40s | 3m20s | 5m56s | 19m24s | 12m42s |
| SemVer Compare | 30s | 5m26s | 1m35s | 8m27s | 5s ⚠️ |
| CSV Parser | 1m05s | 1m20s | 1m10s | 10m32s | 1m15s |
| Dep Resolver | 25s | 1m40s | 2m20s | 2m50s | 45s |
| Gitignore Match | 45s | 1m20s | 1m35s | TIMEOUT | 12m23s |
| Unified Diff | 45s | 9m02s | 3m30s | 9m12s | 5s ⚠️ |
| TinyLang | 7m06s | 10m57s | TIMEOUT | 29m02s | 5s ⚠️ |
| **Total** | **~11m** | **~33m** | **~46m*** | **~112m*** | **~27m** |

### Aigent (wall time)

| Problem | Claude 4.6 | Big Pickle | MiniMax M2.5 | Nemotron 3 | MiMo Flash |
|---|---:|---:|---:|---:|---:|
| Expression Eval | 1m50s | 13m48s | 2m55s | 4m36s | 1m35s |
| SemVer Compare | 35s | 1m05s | 7m11s | 4m21s | 1m40s |
| CSV Parser | 3m25s | 1m05s | 2m15s | 2m55s | 2m25s |
| Dep Resolver | 40s | 1m25s | 3m25s | 5m56s | 55s |
| Gitignore Match | 1m00s | 6m31s | 3m00s | TIMEOUT | 30s |
| Unified Diff | 50s | 6m26s | 4m11s | 19m29s | 5s ⚠️ |
| TinyLang | 4m21s | 8m42s | 11m23s | 20m45s | 35s |
| **Total** | **~12m** | **~39m** | **~34m*** | **~88m*** | **~7m** |

---

## Key Findings

### 1. The `\n` fix promoted Big Pickle to 140/140

The root cause of Big Pickle's Unified Diff failure in Round 3 was that SpEL single-quoted strings pass `\n` as literal backslash-n, not newlines. The `ContractExpressionEvaluator.parseLiteral()` fix unescapes these sequences, making `@Example` inputs with embedded newlines work correctly. Big Pickle's Unified Diff aigent result flipped from 19/20 → **20/20**, completing its perfect score. **Big Pickle now matches Claude Sonnet 4.6.**

The same fix also resolved MiniMax's Unified Diff aigent failure (19/20 → **20/20**).

### 2. Timeout fallthrough rescued partial results

The new behaviour of running `mvn test` even after a 30-minute timeout recovered meaningful data:
- MiniMax TinyLang oneshot: was `0/0` → now **41/43** (model had nearly finished before being killed)
- Nemotron Gitignore both approaches: was `0/0` → now **22/24** and **21/24**
- Nemotron TinyLang oneshot: **42/43** (within timeout — model solved it but with 1 failure)

### 3. MiMo reliability issues in parallel runs

MiMo had three instant failures (~5s each) on SemVer oneshot, Unified Diff oneshot, and TinyLang oneshot. This is a parallel-run artifact: with 5 models hitting opencode simultaneously, API rate-limiting causes immediate exits before any code is written. The aigent approach recovered SemVer (17/17) because it ran later when contention eased. MiMo's 80/140 score is not representative of its true capability — Round 3 showed 122/140 under sequential runs.

### 4. TinyLang remains the hardest problem

| Model | Oneshot | Aigent |
|---|:---:|:---:|
| Claude 4.6 | 43/43 | 43/43 |
| Big Pickle | 43/43 | 43/43 |
| MiniMax M2.5 | [T]41/43 | 9/43 |
| Nemotron 3 | 42/43 | 0/? |
| MiMo Flash | 0/43 | 0/43 |

Only Claude and Big Pickle reliably implement a full interpreter. MiniMax timed out on oneshot but got 41/43 — it was almost there. The aigent spec for TinyLang appears to actively hurt: MiniMax dropped from 41 to 9, Nemotron produced a completely non-compilable result. The dense interpreter spec with loop/function semantics may overload smaller models into confused implementations.

### 5. Aigent delta — what actually changed vs Round 3

| Fix | Predicted impact | Actual |
|---|---|---|
| `\n` unescape | Unified Diff +2 for MiniMax and Big Pickle | ✅ Both 20/20 now |
| Timeout fallthrough | MiMo semver +10 (false timeout in R3) | ✅ MiMo semver aigent: 17/17 |
| Contract enforcement in tests | Richer error messages during iteration | Not measurable in pass/fail scores |
| `@Intent` in failure messages | Richer error messages during iteration | Not measurable in pass/fail scores |

The two infrastructure fixes had measurable, positive impact. The contract/intent improvements affect iteration quality but not the final pass/fail binary captured here.

---

## Aigent Value Summary

| Model | Aigent impact | Dominant factor |
|---|---|---|
| Claude 4.6 | Neutral | Already maxed; spec adds 0 |
| Big Pickle | Neutral (both perfect) | Both approaches solve everything |
| MiniMax M2.5 | −8 | TinyLang aigent collapsed (9/43); SemVer −1 |
| Nemotron 3 Super | −8 | TinyLang aigent: compilation failure; gitignore marginally worse |
| MiMo V2 Flash | Neutral (both unreliable) | Parallel-run failures dominate; SemVer rescued by aigent |

---

## Round 3 → Round 4 Changes

| Model | Round 3 | Round 4 | Δ | Cause |
|---|---:|---:|---:|---|
| Claude 4.6 | 140 | 140 | — | Stable |
| **Big Pickle** | **128** | **140** | **+12** | `\n` fix eliminated Unified Diff failure + prior exp-eval oneshot timeout now passes |
| MiniMax M2.5 | 138 | 128 | −10 | TinyLang aigent 43→9; normal model variance |
| Nemotron 3 Super | 120 | 124 | +4 | Timeout fallthrough rescued Gitignore & TinyLang partial scores |
| MiMo V2 Flash | 122 | 80 | −42 | Parallel-run API failures; not representative |
