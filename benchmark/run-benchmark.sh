#!/usr/bin/env bash
# Aigent vs One-Shot Benchmark Runner
# Usage: ./run-benchmark.sh [model]   (omit to run all models)
# Produces: results/ directory with per-run outputs and a final results.md

set -uo pipefail
cd "$(dirname "$0")"

MODELS=("claude-sonnet-4.6" "gemini-3-pro-preview" "gpt-5.4")
if [ $# -ge 1 ]; then MODELS=("$1"); fi

PROBLEMS=(
  "01-expression-evaluator:expression:ExpressionEvaluator"
  "02-semver-comparator:semver:SemVerComparator"
  "03-csv-parser:csv:CsvParser"
)
APPROACHES=("oneshot" "aigent")

mkdir -p results

# ── Helper: compile + run tests, emit pass/fail counts ───────────────────────
run_tests() {
  local run_dir="$1"
  local mvn_out
  mvn_out=$(cd "$run_dir" && mvn test 2>&1) || true
  # Use Python to parse surefire output — use only the LAST aggregate summary line
  local counts
  counts=$(echo "$mvn_out" | python3 -c "
import sys, re
text = sys.stdin.read()
# Use the final summary line (last 'Tests run:' occurrence — the aggregate)
matches = list(re.finditer(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+)', text))
if matches:
    m = matches[-1]
    total  = int(m.group(1))
    failed = int(m.group(2))
    errors = int(m.group(3))
    passed = total - failed - errors
    print(f'{passed}:{failed+errors}')
else:
    print('0:0')
")
  local passed="${counts%%:*}"
  local bad="${counts##*:}"
  echo "${passed}:${bad}:${mvn_out}"
}

# ── Helper: rate implementation 0-10 based on test results ───────────────────
rate() {
  local passed=$1 bad=$2 total=$3
  if   [ "$total" -eq 0 ];                          then echo 0
  elif [ "$bad"   -eq 0 ];                          then echo 10
  elif [ "$bad"   -le $((total / 5)) ];             then echo 8
  elif [ "$bad"   -le $((total / 3)) ];             then echo 6
  elif [ "$bad"   -le $((total / 2)) ];             then echo 4
  elif [ "$passed" -gt 0 ];                         then echo 2
  else                                                   echo 0
  fi
}

# ── Helper: build one run directory ──────────────────────────────────────────
prepare_run_dir() {
  local run_dir="$1" problem="$2" domain="$3" iface="$4"
  local pkg="com/aigent/benchmark/${domain}"

  mkdir -p "${run_dir}/src/main/java/${pkg}"
  mkdir -p "${run_dir}/src/test/java/${pkg}"

  cp pom-template.xml "${run_dir}/pom.xml"
  cp "${problem}/${iface}.java"     "${run_dir}/src/main/java/${pkg}/"
  cp "${problem}/${iface}Test.java" "${run_dir}/src/test/java/${pkg}/"
}

# ── Main benchmark loop ───────────────────────────────────────────────────────
RESULTS_CSV="results/raw.csv"
echo "model,problem,approach,time_ms,iterations,passed,failed,total,rating" > "$RESULTS_CSV"

for MODEL in "${MODELS[@]}"; do
  echo ""
  echo "════════════════════════════════════════════════════"
  echo "  Model: $MODEL"
  echo "════════════════════════════════════════════════════"

  for PROBLEM_SPEC in "${PROBLEMS[@]}"; do
    PROBLEM="${PROBLEM_SPEC%%:*}"
    REST="${PROBLEM_SPEC#*:}"
    DOMAIN="${REST%%:*}"
    IFACE="${REST##*:}"
    IMPL="${IFACE}Impl"
    PKG_PATH="com/aigent/benchmark/${DOMAIN}"

    echo ""
    echo "  ── Problem: $PROBLEM ──"

    for APPROACH in "${APPROACHES[@]}"; do
      RUN_DIR="results/${MODEL}/${PROBLEM}-${APPROACH}"
      echo -n "     [${APPROACH}] generating... "

      prepare_run_dir "$RUN_DIR" "$PROBLEM" "$DOMAIN" "$IFACE"

      IMPL_DEST="${RUN_DIR}/src/main/java/${PKG_PATH}/${IMPL}.java"

      # ── Build the copilot prompt ───────────────────────────────────────────
      if [ "$APPROACH" = "oneshot" ]; then
        PROMPT="$(cat "${PROBLEM}/oneshot/prompt.txt")

Write the implementation to exactly this file path (create it):
${IMPL_DEST}
Output only a single Java source file. No markdown. No explanation."
      else
        SPEC="$(cat "${PROBLEM}/aigent/spec.java")"
        PROMPT="You are the Aigent implementation engine. Below is a Java file with a @Stub-annotated method. Implement the method strictly following the @Intent, @Contract, @Example, and @Property annotations.

${SPEC}

Write the complete implemented Java file to exactly this path (create it):
${IMPL_DEST}
Output only the Java source file. No markdown. No explanation."
      fi

      # ── Run copilot, measure wall time ────────────────────────────────────
      START_MS=$(python3 -c "import time; print(int(time.time()*1000))")
      COPILOT_OUT=$(copilot --model "$MODEL" --allow-all-paths --allow-all-tools \
                            --add-dir "$RUN_DIR" \
                            -p "$PROMPT" 2>&1)
      END_MS=$(python3 -c "import time; print(int(time.time()*1000))")
      TIME_MS=$((END_MS - START_MS))

      # Extract iteration count from copilot usage summary (macOS-compatible)
      ITERATIONS=$(echo "$COPILOT_OUT" | python3 -c "
import sys, re
text = sys.stdin.read()
m = re.search(r'Total usage est:\s+(\d+)', text)
print(m.group(1) if m else '1')
" 2>/dev/null || echo 1)

      echo "$COPILOT_OUT" > "${RUN_DIR}/copilot-output.txt"

      # ── Check implementation was written ──────────────────────────────────
      if [ ! -f "$IMPL_DEST" ]; then
        echo "WARN: copilot did not write $IMPL_DEST — trying to extract from output"
        # Extract Java code block from copilot output as fallback
        awk '/^```java/,/^```/' "${RUN_DIR}/copilot-output.txt" \
          | grep -v '^```' > "$IMPL_DEST" 2>/dev/null || true
      fi

      # ── Compile + test ─────────────────────────────────────────────────────
      if [ ! -f "$IMPL_DEST" ] || [ ! -s "$IMPL_DEST" ]; then
        PASSED=0; FAILED=0; TOTAL=0
        echo "SKIP (no implementation generated)"
      else
        TEST_RESULT=$(run_tests "$RUN_DIR")
        PASSED="${TEST_RESULT%%:*}"
        REST2="${TEST_RESULT#*:}"
        FAILED="${REST2%%:*}"
        MVN_OUT="${REST2#*:}"
        TOTAL=$((PASSED + FAILED))
        echo "$MVN_OUT" > "${RUN_DIR}/test-output.txt"
        echo "done (${PASSED}/${TOTAL} tests passed, ${TIME_MS}ms)"
      fi

      RATING=$(rate "$PASSED" "$FAILED" "$TOTAL")

      # ── Append to CSV ──────────────────────────────────────────────────────
      echo "${MODEL},${PROBLEM},${APPROACH},${TIME_MS},${ITERATIONS},${PASSED},${FAILED},${TOTAL},${RATING}" >> "$RESULTS_CSV"

      # ── Save run metadata ──────────────────────────────────────────────────
      cat > "${RUN_DIR}/metadata.json" <<JSON
{
  "model":      "${MODEL}",
  "problem":    "${PROBLEM}",
  "approach":   "${APPROACH}",
  "time_ms":    ${TIME_MS},
  "iterations": ${ITERATIONS},
  "passed":     ${PASSED},
  "failed":     ${FAILED},
  "total":      ${TOTAL},
  "rating":     ${RATING}
}
JSON
    done
  done
done

echo ""
echo "════════════════════════════════════════════════════"
echo "  Generating results/results.md ..."
echo "════════════════════════════════════════════════════"

# ── Generate markdown results table ──────────────────────────────────────────
python3 - "$RESULTS_CSV" <<'PYEOF'
import sys, csv

src = sys.argv[1]
rows = list(csv.DictReader(open(src)))

models    = list(dict.fromkeys(r["model"]    for r in rows))
problems  = list(dict.fromkeys(r["problem"]  for r in rows))
approaches = ["oneshot", "aigent"]

lines = []
lines.append("# Benchmark Results\n")
lines.append(f"Models: {', '.join(models)}\n")
lines.append(f"Problems: {', '.join(problems)}\n\n")

# Summary table
lines.append("## Rating Summary (0 = all wrong, 10 = all correct)\n")
header = "| Problem | Approach |" + "".join(f" {m} |" for m in models)
sep    = "|---------|----------|" + "".join("---------:|" for _ in models)
lines.append(header + "\n")
lines.append(sep + "\n")

for prob in problems:
    for appr in approaches:
        cells = f"| `{prob}` | **{appr}** |"
        for model in models:
            match = [r for r in rows if r["model"]==model and r["problem"]==prob and r["approach"]==appr]
            if match:
                r = match[0]
                rating = r["rating"]
                passed = r["passed"]
                total  = r["total"]
                time_s = int(r["time_ms"]) / 1000
                cells += f" {rating}/10 ({passed}/{total} ✓, {time_s:.1f}s) |"
            else:
                cells += " — |"
        lines.append(cells + "\n")

lines.append("\n")

# Per-problem detail
for prob in problems:
    lines.append(f"## {prob}\n\n")
    lines.append("| Model | Approach | Passed | Failed | Total | Time | Iterations | Rating |\n")
    lines.append("|-------|----------|-------:|-------:|------:|-----:|-----------:|-------:|\n")
    for model in models:
        for appr in approaches:
            match = [r for r in rows if r["model"]==model and r["problem"]==prob and r["approach"]==appr]
            if match:
                r = match[0]
                time_s = int(r["time_ms"]) / 1000
                lines.append(f"| {model} | {appr} | {r['passed']} | {r['failed']} | {r['total']} | {time_s:.1f}s | {r['iterations']} | **{r['rating']}/10** |\n")
    lines.append("\n")

with open("results/results.md", "w") as f:
    f.writelines(lines)
print("results/results.md written.")
PYEOF

echo ""
echo "Done. See results/results.md for the full report."
cat results/results.md
