#!/usr/bin/env bash
# Aigent Benchmark Runner — all 7 problems, all models
#
# Usage:
#   ./run-benchmark.sh                              # all models, all problems, both approaches
#   ./run-benchmark.sh [model]                      # one model, all problems, both approaches
#   ./run-benchmark.sh [model] [approach]           # one model, all problems, one approach
#   ./run-benchmark.sh [model] [approach] [problem] # one model, one problem, one approach
#
# Registered models (MODEL_ID:CLI_TOOL):
#   claude-sonnet-4-6                  → claude   CLI
#   opencode/nemotron-3-super-free     → opencode CLI
#   opencode/minimax-m2.5-free         → opencode CLI
#   opencode/big-pickle                → opencode CLI
#   opencode/mimo-v2-flash-free        → opencode CLI
#   gpt-5.4                            → copilot  CLI
#   gemini-3-pro-preview               → copilot  CLI

set -euo pipefail
cd "$(dirname "$0")"

# ── Model registry ─────────────────────────────────────────────────────────────
ALL_MODELS=(
  "claude-sonnet-4-6:claude"
  "opencode/minimax-m2.5-free:opencode"
  "opencode/big-pickle:opencode"
  "opencode/nemotron-3-super-free:opencode"
  "opencode/mimo-v2-flash-free:opencode"
  "gpt-5.4:copilot"
  "gemini-3-pro-preview:copilot"
)

# ── Problem registry: "dir:domain:Interface" ───────────────────────────────────
ALL_PROBLEMS=(
  "01-expression-evaluator:expression:ExpressionEvaluator"
  "02-semver-comparator:semver:SemVerComparator"
  "03-csv-parser:csv:CsvParser"
  "04-dependency-resolver:deps:DependencyResolver"
  "05-gitignore-matcher:gitignore:GitIgnoreMatcher"
  "06-unified-diff-applier:diff:UnifiedDiffApplier"
  "07-tinylang-interpreter:tinylang:TinyLang"
)

# ── Argument parsing ───────────────────────────────────────────────────────────

# --merge: combine all per-model CSVs into results/raw.csv and exit
if [ $# -ge 1 ] && [ "$1" = "--merge" ]; then
  OUT="results/raw.csv"
  echo "model,problem,approach,time_ms,passed,failed,total,rating" > "$OUT"
  for f in results/raw-*.csv; do
    [ -f "$f" ] && tail -n +2 "$f" >> "$OUT"
  done
  echo "Merged into $OUT ($(( $(wc -l < "$OUT") - 1 )) rows)"
  exit 0
fi

MODELS=("${ALL_MODELS[@]}")
APPROACHES=("oneshot" "aigent")
PROBLEMS=("${ALL_PROBLEMS[@]}")

if [ $# -ge 1 ] && [[ "$1" != "oneshot" && "$1" != "aigent" ]]; then
  FILTER_MODEL="$1"; shift
  MODELS=()
  for entry in "${ALL_MODELS[@]}"; do
    [[ "${entry%%:*}" == "$FILTER_MODEL" ]] && MODELS+=("$entry")
  done
  if [ ${#MODELS[@]} -eq 0 ]; then
    echo "Unknown model: $FILTER_MODEL"
    echo "Available: $(printf '%s  ' "${ALL_MODELS[@]%%:*}")"
    exit 1
  fi
fi

if [ $# -ge 1 ]; then
  APPROACHES=("$1"); shift
fi

if [ $# -ge 1 ]; then
  FILTER_PROBLEM="$1"; shift
  PROBLEMS=()
  for entry in "${ALL_PROBLEMS[@]}"; do
    [[ "${entry%%:*}" == "$FILTER_PROBLEM" ]] && PROBLEMS+=("$entry")
  done
  if [ ${#PROBLEMS[@]} -eq 0 ]; then
    echo "Unknown problem: $FILTER_PROBLEM"
    echo "Available: $(printf '%s  ' "${ALL_PROBLEMS[@]%%:*}")"
    exit 1
  fi
fi

# ── Per-run timeout: kill model if still running after this many seconds ────────
MODEL_TIMEOUT=${MODEL_TIMEOUT:-1800}   # 30 min default; override: MODEL_TIMEOUT=600 ./run-benchmark.sh

# ── Helpers ────────────────────────────────────────────────────────────────────
model_dir() { echo "$1" | tr '/' '-'; }

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
    total = int(m.group(1)); failed = int(m.group(2)); errors = int(m.group(3))
    print(f"{total - failed - errors}:{failed + errors}")
else:
    print("0:0")
')
  echo "${counts%%:*}:${counts##*:}:${mvn_out}"
}

rate() {
  local passed=$1 bad=$2 total=$3
  if   [ "$total" -eq 0 ];              then echo 0
  elif [ "$bad"   -eq 0 ];              then echo 10
  elif [ "$bad"   -le $((total / 5)) ]; then echo 8
  elif [ "$bad"   -le $((total / 3)) ]; then echo 6
  elif [ "$bad"   -le $((total / 2)) ]; then echo 4
  elif [ "$passed" -gt 0 ];             then echo 2
  else                                       echo 0
  fi
}

prepare_run_dir() {
  local run_dir="$1" problem="$2" pkg_path="$3" iface="$4"
  rm -rf "$run_dir"
  mkdir -p "${run_dir}/src/main/java/${pkg_path}"
  mkdir -p "${run_dir}/src/test/java/${pkg_path}"
  cp pom-template.xml "${run_dir}/pom.xml"
  cp "${problem}/${iface}.java"     "${run_dir}/src/main/java/${pkg_path}/"
  cp "${problem}/${iface}Test.java" "${run_dir}/src/test/java/${pkg_path}/"
  # ExampleTest runs @Example annotations as JUnit tests via aigent testing module
  if [ -f "${problem}/${iface}ExampleTest.java" ]; then
    cp "${problem}/${iface}ExampleTest.java" "${run_dir}/src/test/java/${pkg_path}/"
  fi
}

# Invoke the model CLI, streaming output to out_file.
# Wrapped in `timeout $MODEL_TIMEOUT` — exits 124 on timeout.
invoke_model_to_file() {
  local cli="$1" model="$2" run_dir="$3" prompt="$4" out_file="$5"
  case "$cli" in
    claude)
      timeout "$MODEL_TIMEOUT" env -u ANTHROPIC_API_KEY claude \
        --model "$model" --dangerously-skip-permissions \
        --add-dir "$run_dir" -p "$prompt" > "$out_file" 2>&1
      ;;
    opencode)
      timeout "$MODEL_TIMEOUT" opencode run "$prompt" -m "$model" \
        --dir "$run_dir" > "$out_file" 2>&1
      ;;
    copilot)
      timeout "$MODEL_TIMEOUT" copilot --model "$model" \
        --allow-all --no-ask-user --no-auto-update \
        --add-dir "$run_dir" -p "$prompt" > "$out_file" 2>&1
      ;;
    *) echo "Unknown CLI: $cli" >&2; return 1 ;;
  esac
}

# Run invoke_model_to_file in the background and show a live progress line:
#   ↳  42s  <last line of model output>
# Clears the line when done. Returns the model's exit code.
run_with_progress() {
  local cli="$1" model="$2" run_dir="$3" prompt="$4" out_file="$5"
  : > "$out_file"   # create file so tail -1 works immediately
  invoke_model_to_file "$cli" "$model" "$run_dir" "$prompt" "$out_file" &
  local pid=$! elapsed=0 last_line=""
  while kill -0 "$pid" 2>/dev/null; do
    sleep 5; elapsed=$((elapsed + 5))
    # Strip ANSI escape codes; show last non-empty line
    last_line=$(tail -5 "$out_file" 2>/dev/null \
      | sed 's/\x1B\[[0-9;]*[mGKHF]//g; s/\r/\n/g' \
      | grep -v '^[[:space:]]*$' | tail -1 | cut -c1-110)
    printf "\r     \033[2m↳ %4ds  %s\033[0m%-20s" \
      "$elapsed" "${last_line:-…}" "" >&2
  done
  wait "$pid"; local exit_code=$?
  printf "\r%140s\r" "" >&2   # clear progress line
  return "$exit_code"
}

# ── CSV ────────────────────────────────────────────────────────────────────────
# When running a single model (FILTER_MODEL set), write to results/raw-<model>.csv
# so multiple models can run in parallel without CSV write conflicts.
# Merge all per-model CSVs into results/raw.csv with: ./run-benchmark.sh --merge
mkdir -p results
if [ "${FILTER_MODEL:-}" != "" ]; then
  RESULTS_CSV="results/raw-$(model_dir "$FILTER_MODEL").csv"
else
  RESULTS_CSV="results/raw.csv"
fi
if [ ! -f "$RESULTS_CSV" ]; then
  echo "model,problem,approach,time_ms,passed,failed,total,rating" > "$RESULTS_CSV"
fi

BENCHMARK_DIR="$(pwd)"

# ── Main loop ──────────────────────────────────────────────────────────────────
for MODEL_ENTRY in "${MODELS[@]}"; do
  MODEL="${MODEL_ENTRY%%:*}"
  CLI="${MODEL_ENTRY##*:}"
  MDIR="$(model_dir "$MODEL")"

  echo ""
  echo "════════════════════════════════════════════════════"
  echo "  Model: ${MODEL}  (CLI: ${CLI})"
  echo "════════════════════════════════════════════════════"

  for PROBLEM_SPEC in "${PROBLEMS[@]}"; do
    PROBLEM="${PROBLEM_SPEC%%:*}"
    REST="${PROBLEM_SPEC#*:}"
    DOMAIN="${REST%%:*}"
    IFACE="${REST##*:}"
    PKG_PATH="de/makibytes/benchmark/${DOMAIN}"

    echo ""
    echo "  ── Problem: ${PROBLEM} ──"

    for APPROACH in "${APPROACHES[@]}"; do
      RUN_DIR="results/${MDIR}/${PROBLEM}-${APPROACH}"
      SRC_DIR="${BENCHMARK_DIR}/${RUN_DIR}/src/main/java/${PKG_PATH}"

      echo -n "     [${APPROACH}] generating..."

      prepare_run_dir "$RUN_DIR" "$PROBLEM" "$PKG_PATH" "$IFACE"

      if [ "$APPROACH" = "oneshot" ]; then
        PROMPT="$(cat "${PROBLEM}/oneshot/prompt.txt")

The run directory is: ${BENCHMARK_DIR}/${RUN_DIR}
Write all Java implementation files under: ${SRC_DIR}/

The ${IFACE}.java interface and ${IFACE}Test.java are already present in the run directory.
Use all available tools: write files, run 'mvn test' in the run directory, fix failures, iterate."

      else
        SPEC="$(cat "${PROBLEM}/aigent/spec.java")"
        PROMPT="You are the Aigent implementation engine. Below is a Java spec file with
@Stub-annotated method(s). Implement the spec so that 'mvn test' passes in the run directory.

${SPEC}

The run directory is: ${BENCHMARK_DIR}/${RUN_DIR}
Write the implementation under: ${SRC_DIR}/

Rules:
- Remove @Stub. Write the full implementation.
- Keep all spec annotations (@Intent, @Contract, @Example, @Property) exactly as-is.
- All aigent annotations are in package de.makibytes.aigent — the import is already in the file.
- Add @AiNote immediately before the method signature using ONLY these fields:
    @AiNote(confidence = Confidence.HIGH, assumed = \"...\", open = \"...\")
  (valid fields: confidence, assumed, open — no other field names exist)
- Use all available tools: write files, run 'mvn test' in the run directory, fix failures, iterate."
      fi

      MODEL_LOG="${RUN_DIR}/model-output.txt"
      START_MS=$(python3 -c 'import time; print(int(time.time()*1000))')
      MODEL_STATUS=0
      run_with_progress "$CLI" "$MODEL" "$RUN_DIR" "$PROMPT" "$MODEL_LOG" || MODEL_STATUS=$?
      END_MS=$(python3 -c 'import time; print(int(time.time()*1000))')
      TIME_MS=$((END_MS - START_MS))

      if [ "$MODEL_STATUS" -eq 124 ]; then
        echo "TIMEOUT after ${MODEL_TIMEOUT}s — checking if implementation exists..."
      elif [ "$MODEL_STATUS" -ne 0 ]; then
        echo "FAILED (exit ${MODEL_STATUS}) — skipping tests"
        echo "${MODEL},${PROBLEM},${APPROACH},${TIME_MS},0,0,0,0" >> "$RESULTS_CSV"
        continue
      fi

      if [ "$MODEL_STATUS" -eq 124 ]; then
        echo -n "     [TIMEOUT] done (${TIME_MS}ms)"
      else
        echo "done (${TIME_MS}ms)"
      fi
      echo -n "     running tests... "

      TEST_RESULT=$(run_tests "$RUN_DIR")
      PASSED="${TEST_RESULT%%:*}"
      REST2="${TEST_RESULT#*:}"
      FAILED="${REST2%%:*}"
      MVN_OUT="${REST2#*:}"
      TOTAL=$((PASSED + FAILED))
      echo "$MVN_OUT" > "${RUN_DIR}/test-output.txt"
      echo "${PASSED}/${TOTAL} passed"

      RATING=$(rate "$PASSED" "$FAILED" "$TOTAL")
      echo "${MODEL},${PROBLEM},${APPROACH},${TIME_MS},${PASSED},${FAILED},${TOTAL},${RATING}" >> "$RESULTS_CSV"

      cat > "${RUN_DIR}/metadata.json" <<JSON
{
  "model":    "${MODEL}",
  "problem":  "${PROBLEM}",
  "approach": "${APPROACH}",
  "time_ms":  ${TIME_MS},
  "passed":   ${PASSED},
  "failed":   ${FAILED},
  "total":    ${TOTAL},
  "rating":   ${RATING}
}
JSON

    done
  done
done

echo ""
echo "════════════════════════════════════════════════════"
echo "  Results written to ${RESULTS_CSV}"
echo "════════════════════════════════════════════════════"
