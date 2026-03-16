package de.makibytes.benchmark.diff;

import de.makibytes.aigent.*;

/**
 * Aigent spec for UnifiedDiffApplierImpl.
 * All annotations are enforced at runtime via SpEL.
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
        requires = "$original != null && $unifiedDiff != null",
        ensures  = "$unifiedDiff.trim().isEmpty() ? $result.equals($original) : $result != null",
        throws_  = {NullPointerException.class, IllegalArgumentException.class}
    )
    @Example(label = "empty diff",       input = "{'a\nb', ''}",                                                                     output = "'a\nb'")
    @Example(label = "add at beginning", input = "{'a\nb', '@@ -1,2 +1,3 @@\n+zero\n a\n b'}",                                      output = "'zero\na\nb'")
    @Example(label = "replace middle",   input = "{'a\nold\nc', '@@ -1,3 +1,3 @@\n a\n-old\n+new\n c'}",                           output = "'a\nnew\nc'")
    @Example(label = "delete all",       input = "{'a\nb', '@@ -1,2 +0,0 @@\n-a\n-b'}",                                             output = "''")
    @Example(label = "create from empty",input = "{'', '@@ -0,0 +1,2 @@\n+hello\n+world'}",                                         output = "'hello\nworld'")
    @Example(label = "context mismatch", input = "{'a\nb', '@@ -1,2 +1,2 @@\n x\n b'}",                                            throws_ = IllegalArgumentException.class)
    @Example(label = "body without hdr", input = "{'a', '+b'}",                                                                      throws_ = IllegalArgumentException.class)
    @Property("$unifiedDiff.trim().isEmpty() ? $result.equals($original) : true")
    @Property("$result != null")
    @Stub("Parse hunks explicitly. Never guess through a mismatch: throw IllegalArgumentException. " +
          "Preserve untouched lines between hunks. The empty-diff postcondition is runtime-verified.")
    @Override
    public String apply(String original, String unifiedDiff) {
        throw new UnsupportedOperationException();
    }
}
