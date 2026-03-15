#!/usr/bin/env bash
# Aigent vs One-Shot Benchmark Runner (complex problems only)
# Usage: ./run-complex-benchmark.sh [model]   (omit to run all models)

set -uo pipefail
cd "$(dirname "$0")"

MODELS=("claude-sonnet-4.6" "gemini-3-pro-preview" "gpt-5.4")
if [ $# -ge 1 ]; then MODELS=("$1"); fi

PROBLEMS=(
  "04-dependency-resolver:deps:DependencyResolver"
  "05-gitignore-matcher:gitignore:GitIgnoreMatcher"
  "06-unified-diff-applier:diff:UnifiedDiffApplier"
)
APPROACHES=("oneshot" "aigent")

mkdir -p results

run_tests() {
  local run_dir="$1"
  local mvn_out
  mvn_out=$(cd "$run_dir" && mvn test 2>&1) || true
  local counts
  counts=$(echo "$mvn_out" | python3 -c '
import sys, re
text = sys.stdin.read()
matches = list(re.finditer(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+)", text))
if matches:
    m = matches[-1]
    total = int(m.group(1))
    failed = int(m.group(2))
    errors = int(m.group(3))
    passed = total - failed - errors
    print(f"{passed}:{failed + errors}")
else:
    print("0:0")
')
  local passed="${counts%%:*}"
  local bad="${counts##*:}"
  echo "${passed}:${bad}:${mvn_out}"
}

rate() {
  local passed=$1 bad=$2 total=$3
  if   [ "$total" -eq 0 ];              then echo 0
  elif [ "$bad" -eq 0 ];                then echo 10
  elif [ "$bad" -le $((total / 5)) ];   then echo 8
  elif [ "$bad" -le $((total / 3)) ];   then echo 6
  elif [ "$bad" -le $((total / 2)) ];   then echo 4
  elif [ "$passed" -gt 0 ];             then echo 2
  else                                        echo 0
  fi
}

prepare_run_dir() {
  local run_dir="$1" problem="$2" domain="$3" iface="$4"
  local pkg="com/aigent/benchmark/${domain}"

  rm -rf "$run_dir"
  mkdir -p "${run_dir}/src/main/java/${pkg}"
  mkdir -p "${run_dir}/src/test/java/${pkg}"

  cp pom-template.xml "${run_dir}/pom.xml"
  cp "${problem}/${iface}.java"     "${run_dir}/src/main/java/${pkg}/"
  cp "${problem}/${iface}Test.java" "${run_dir}/src/test/java/${pkg}/"
}

RESULTS_CSV="results/complex-raw.csv"
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
      IMPL_DEST="$(cd "$RUN_DIR" && pwd)/src/main/java/${PKG_PATH}/${IMPL}.java"

      if [ "$APPROACH" = "oneshot" ]; then
        PROMPT="$(cat "${PROBLEM}/oneshot/prompt.txt")

Write the implementation to exactly this file path (create it):
${IMPL_DEST}
Output only a single Java source file. No markdown. No explanation."
      else
        SPEC="$(cat "${PROBLEM}/aigent/spec.java")"
        PROMPT="You are the Aigent implementation engine. Below is a Java file with a @Stub-annotated method. Implement the method strictly following the @Intent, @Contract, @Example, and @Property annotations.

Rules:
- Remove @Stub. Write the full implementation body.
- Keep all spec annotations (@Intent, @Contract, @Example, @Property) exactly as-is.
- All aigent annotations are in package de.makibytes.aigent — the import is already in the file.
- Add @AiNote immediately before the method signature using ONLY these fields:
    @AiNote(confidence = Confidence.HIGH, assumed = \"...\", open = \"...\")
  (fields: confidence, assumed, open — no other field names exist)
- Use Confidence.HIGH / Confidence.MEDIUM / Confidence.LOW as appropriate.

${SPEC}

Write the complete implemented Java file to exactly this path (create it):
${IMPL_DEST}
Output only the Java source file. No markdown. No explanation."
      fi

      START_MS=$(python3 -c 'import time; print(int(time.time()*1000))')
      COPILOT_OUT=$(copilot --model "$MODEL" --allow-all-paths --allow-all-tools --add-dir "$RUN_DIR" -p "$PROMPT" 2>&1)
      END_MS=$(python3 -c 'import time; print(int(time.time()*1000))')
      TIME_MS=$((END_MS - START_MS))

      ITERATIONS=$(echo "$COPILOT_OUT" | python3 -c '
import sys, re
text = sys.stdin.read()
m = re.search(r"Total usage est:\s+(\d+)", text)
print(m.group(1) if m else "1")
' 2>/dev/null || echo 1)

      echo "$COPILOT_OUT" > "${RUN_DIR}/copilot-output.txt"

      if [ ! -f "$IMPL_DEST" ]; then
        awk '/^```java/,/^```/' "${RUN_DIR}/copilot-output.txt" | grep -v '^```' > "$IMPL_DEST" 2>/dev/null || true
      fi

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
      echo "${MODEL},${PROBLEM},${APPROACH},${TIME_MS},${ITERATIONS},${PASSED},${FAILED},${TOTAL},${RATING}" >> "$RESULTS_CSV"

      cat > "${RUN_DIR}/metadata.json" <<JSON
{
  "model": "${MODEL}",
  "problem": "${PROBLEM}",
  "approach": "${APPROACH}",
  "time_ms": ${TIME_MS},
  "iterations": ${ITERATIONS},
  "passed": ${PASSED},
  "failed": ${FAILED},
  "total": ${TOTAL},
  "rating": ${RATING}
}
JSON
    done
  done
done

echo ""
echo "Done. Raw results written to ${RESULTS_CSV}."
