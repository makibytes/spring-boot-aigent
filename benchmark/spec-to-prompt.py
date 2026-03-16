#!/usr/bin/env python3
"""
spec-to-prompt.py  <spec.java> <iface_name> <run_dir> <src_dir>

Converts an aigent spec.java into a rich, model-friendly implementation prompt.
Extracts @Intent, @Contract, @Example, @Property, @Stub — no annotation overhead,
no @AiNote rules, no "keep annotations" restrictions.
"""
import sys, re


# ── Java source parsing ───────────────────────────────────────────────────────

def extract_paren_block(text, start):
    """Extract content between matching parens. start must point to '('. Returns (content, end)."""
    assert text[start] == '(', f"Expected '(' at {start}, got {text[start]!r}"
    depth = 0; in_str = False; in_tb = False
    i = start; buf = []
    while i < len(text):
        if not in_str and not in_tb and text[i:i+3] == '"""':
            in_tb = True; buf.append('"""'); i += 3; continue
        if in_tb and text[i:i+3] == '"""':
            in_tb = False; buf.append('"""'); i += 3; continue
        c = text[i]
        if in_tb:
            buf.append(c); i += 1; continue
        if in_str:
            if c == '\\': buf.append(text[i:i+2]); i += 2; continue
            if c == '"': in_str = False
            buf.append(c); i += 1; continue
        if c == '"': in_str = True
        elif c == '(': depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return ''.join(buf[1:]), i + 1   # strip leading '('
        buf.append(c); i += 1
    raise ValueError(f"Unmatched '(' at {start}")


def split_top_commas(content):
    """Split a string on commas that are not inside strings, braces, or parens."""
    parts = []; buf = []; depth = 0; in_str = False; in_tb = False
    i = 0
    while i < len(content):
        if not in_str and not in_tb and content[i:i+3] == '"""':
            in_tb = True; buf.append('"""'); i += 3; continue
        if in_tb and content[i:i+3] == '"""':
            in_tb = False; buf.append('"""'); i += 3; continue
        c = content[i]
        if in_tb:
            buf.append(c); i += 1; continue
        if in_str:
            if c == '\\': buf.append(content[i:i+2]); i += 2; continue
            if c == '"': in_str = False
            buf.append(c); i += 1; continue
        if c == '"': in_str = True
        elif c in '({': depth += 1
        elif c in ')}': depth -= 1
        elif c == ',' and depth == 0:
            parts.append(''.join(buf).strip()); buf = []; i += 1; continue
        buf.append(c); i += 1
    if buf: parts.append(''.join(buf).strip())
    return parts


def parse_attrs(content):
    """Parse 'key = value, key2 = value2' annotation content into a dict.
    Values keep their raw Java form (quotes included)."""
    attrs = {}
    for part in split_top_commas(content):
        m = re.match(r'(\w+)\s*=\s*(.*)', part.strip(), re.DOTALL)
        if m:
            attrs[m.group(1)] = m.group(2).strip()
        elif part.strip():
            attrs['value'] = part.strip()   # positional / single-value annotation
    return attrs


def java_str(raw, keep_newline_escapes=False):
    """Decode a raw Java string value to a Python string.

    raw may be:
      - a text block: triple-quote delimited, already has real newlines
      - one or more regular string literals possibly concatenated with +
      - an unquoted value (returned as-is)

    keep_newline_escapes=True: leave \\n as literal \\n (used for SpEL expressions
    in @Example input/output so line-break intent is visible in the prompt).
    """
    raw = raw.strip()

    # Text block """..."""
    if raw.startswith('"""'):
        end = raw.rfind('"""', 3)
        inner = raw[3:end]
        if inner.startswith('\n'): inner = inner[1:]
        lines = inner.split('\n')
        non_empty = [l for l in lines if l.strip()]
        if non_empty:
            indent = min(len(l) - len(l.lstrip()) for l in non_empty)
            lines = [l[indent:] if len(l) >= indent else l for l in lines]
        while lines and not lines[-1].strip():
            lines.pop()
        return '\n'.join(lines)

    # Regular string literals, possibly concatenated with +
    parts = re.findall(r'"((?:[^"\\]|\\.)*)"', raw)
    if not parts:
        return raw   # unquoted (e.g. bare word, number)
    result = ''.join(parts)
    result = result.replace('\\"', '"')
    if not keep_newline_escapes:
        result = result.replace('\\n', '\n').replace('\\t', '\t')
    result = result.replace('\\\\', '\\')
    return result


def simple_class_name(ref):
    """'a.b.SomeException.class' → 'SomeException'"""
    ref = ref.strip()
    if ref.endswith('.class'): ref = ref[:-6]
    return ref.split('.')[-1]


def parse_throws(val):
    """Parse '{A.class, B.class}' or 'A.class' into list of simple names."""
    val = val.strip()
    inner = val[1:-1] if val.startswith('{') else val
    return [simple_class_name(p) for p in inner.split(',') if p.strip()]


def find_annotations(src, name):
    """Return list of attr-dicts for every @name(...) in src."""
    results = []
    for m in re.finditer(rf'@{re.escape(name)}\s*\(', src):
        content, _ = extract_paren_block(src, m.end() - 1)
        results.append(parse_attrs(content.strip()))
    return results


def extract_method_sig(src):
    """Return (return_type, method_name, params_str) for the @Stub method."""
    m = re.search(r'public\s+(\S+)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+\S+\s*)?\{', src)
    if m: return m.group(1), m.group(2), m.group(3).strip()
    return None, None, None


def extract_class(src):
    """Return (ImplClassName, IfaceSimpleName)."""
    m = re.search(r'class\s+(\w+)\s+implements\s+([\w.]+)', src)
    if m: return m.group(1), m.group(2).split('.')[-1]
    return None, None


def extract_package(src):
    m = re.search(r'^package\s+([\w.]+);', src, re.MULTILINE)
    return m.group(1) if m else ''


# ── Prompt builder ────────────────────────────────────────────────────────────

def build_prompt(spec_path, iface_name, run_dir, src_dir):
    src = open(spec_path).read()

    pkg             = extract_package(src)
    impl, iface     = extract_class(src)
    ret, mname, params = extract_method_sig(src)

    intents    = find_annotations(src, 'Intent')
    contracts  = find_annotations(src, 'Contract')
    examples   = find_annotations(src, 'Example')
    properties = find_annotations(src, 'Property')
    stubs      = find_annotations(src, 'Stub')

    lines = []

    # ── Header ────────────────────────────────────────────────────────────────
    lines += [
        f"Implement `{impl}` in package `{pkg}` that satisfies the `{iface}` interface.",
        f"",
        f"  Signature:  {ret} {mname}({params})",
        f"",
    ]

    # ── Intent ────────────────────────────────────────────────────────────────
    if intents:
        intent_text = java_str(intents[0].get('value', ''))
        lines += ["## What to implement", intent_text.strip(), ""]

    # ── Required test cases ───────────────────────────────────────────────────
    if examples:
        lines.append(f"## Required test cases — all {len(examples)} must pass")
        w = len(str(len(examples)))
        for i, ex in enumerate(examples, 1):
            label = java_str(ex.get('label', '')) if 'label' in ex else str(i)
            inp   = java_str(ex.get('input', ''), keep_newline_escapes=True) if 'input' in ex else ''

            if 'throws_' in ex:
                throws = parse_throws(ex['throws_'])
                lines.append(f"  {i:{w}}. [{label:<32}]  in: {inp}  →  throws {', '.join(throws)}")
            else:
                out = java_str(ex.get('output', ''), keep_newline_escapes=True) if 'output' in ex else ''
                lines.append(f"  {i:{w}}. [{label:<32}]  in: {inp}  →  {out}")
        lines.append("")

    # ── Contract ──────────────────────────────────────────────────────────────
    if contracts:
        c = contracts[0]
        contract_lines = []
        if 'requires' in c:
            contract_lines.append(f"  Precondition:  {java_str(c['requires'])}")
        if 'ensures' in c:
            contract_lines.append(f"  Postcondition: {java_str(c['ensures'])}")
        if 'throws_' in c:
            contract_lines.append(
                f"  Throws:        {', '.join(parse_throws(c['throws_']))} when precondition violated")
        if contract_lines:
            lines += ["## Contract"] + contract_lines + [""]

    # ── Invariants ────────────────────────────────────────────────────────────
    if properties:
        lines.append("## Invariants (must hold for all valid inputs)")
        for p in properties:
            lines.append(f"  - {java_str(p.get('value', ''))}")
        lines.append("")

    # ── Implementation notes (@Stub value) ────────────────────────────────────
    if stubs:
        note = java_str(stubs[0].get('value', '')).strip()
        if note:
            lines += ["## Implementation notes", note, ""]

    # ── Footer ────────────────────────────────────────────────────────────────
    lines += [
        f"Run directory:  {run_dir}",
        f"Write all implementation files under: {src_dir}",
        f"The `{iface}.java` interface and full test suite are already in the run directory.",
        f"Run `mvn test`, read failure messages carefully — they show expected vs actual values.",
        f"Fix failures and iterate until all tests pass.",
    ]

    return '\n'.join(lines)


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == '__main__':
    if len(sys.argv) != 5:
        sys.exit(f"Usage: {sys.argv[0]} <spec.java> <iface_name> <run_dir> <src_dir>")
    print(build_prompt(*sys.argv[1:]))
