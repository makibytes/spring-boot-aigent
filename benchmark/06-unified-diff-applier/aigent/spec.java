package com.aigent.benchmark.diff;

import de.makibytes.aigent.*;

/**
 * Aigent spec for UnifiedDiffApplierImpl.
 */
public class UnifiedDiffApplierImpl implements UnifiedDiffApplier {

    @Intent("""
            Apply a simplified unified diff to a text document.

            Benchmark assumptions:
              - original and unifiedDiff use \\n line endings only
              - optional file headers beginning with --- and +++ should be ignored
              - each hunk header has the exact form @@ -oldStart,oldCount +newStart,newCount @@
              - hunk body lines begin with one of:
                    ' '  context line that must match original exactly
                    '-'  deletion line that must match original exactly and be removed
                    '+'  addition line to insert into the output
              - line numbers are 1-based; a hunk starting at oldStart=0 is valid only when oldCount=0
              - malformed diffs or context mismatches must throw IllegalArgumentException
              - do not silently skip invalid hunks
            """)
    @Contract(
        requires = "original != null && unifiedDiff != null",
        ensures  = "$result is the original text with every valid hunk applied in order",
        throws_  = {NullPointerException.class, IllegalArgumentException.class}
    )
    @Example(label = "empty diff", input = "original='a\nb', diff=''", output = "a\nb")
    @Example(label = "add at beginning", input = "original='a\nb', diff='@@ -1,2 +1,3 @@\n+zero\n a\n b'", output = "zero\na\nb")
    @Example(label = "replace middle", input = "original='a\nold\nc', diff='@@ -1,3 +1,3 @@\n a\n-old\n+new\n c'", output = "a\nnew\nc")
    @Example(label = "delete all", input = "original='a\nb', diff='@@ -1,2 +0,0 @@\n-a\n-b'", output = "")
    @Example(label = "create from empty", input = "original='', diff='@@ -0,0 +1,2 @@\n+hello\n+world'", output = "hello\nworld")
    @Example(label = "multiple hunks", input = "original='a\nb\nc\nd\ne', diff='@@ -1,2 +1,2 @@\n a\n-b\n+beta\n@@ -4,2 +4,3 @@\n d\n e\n+f'", output = "a\nbeta\nc\nd\ne\nf")
    @Example(label = "context mismatch", input = "original='a\nb', diff='@@ -1,2 +1,2 @@\n x\n b'", output = "throws IllegalArgumentException")
    @Example(label = "body without header", input = "original='a', diff='+b'", output = "throws IllegalArgumentException")
    @Property("when unifiedDiff is empty or only file headers, $result == original")
    @Property("every context or deletion line in a valid hunk must match the original text at that position exactly")
    @Stub("Parse hunks explicitly. Never guess through a mismatch: throw IllegalArgumentException instead. Preserve untouched lines between hunks.")
    @Override
    public String apply(String original, String unifiedDiff) {
        throw new UnsupportedOperationException();
    }
}
