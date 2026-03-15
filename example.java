import com.aigent.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

// Illustrates the two lifecycle states of the Aigent paradigm in a realistic class.
//
//  STUB STATE    — human wrote the spec; @Stub signals the AI to implement.
//  REVIEWED STATE — AI implemented; @AiNote signals the human to review.
//                   Human either accepts (removes @AiNote) or refines spec + re-adds @Stub.

class PhoneService {

    // ── STUB STATE ────────────────────────────────────────────────────────────
    // Human has written the full spec. @Stub is the work-order for the AI.
    // The AI will: implement the body, remove @Stub, add @AiNote.

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
    @Property("$result.stream().allMatch(n -> n.matches(\"\\+[0-9]+\"))")
    @Stub("North American numbers only for now; international format TBD")
    List<String> normalize(List<String> numbers) {
        throw new UnsupportedOperationException();
    }


    // ── REVIEWED STATE ────────────────────────────────────────────────────────
    // AI implemented and added @AiNote. Human is reviewing.
    // Human options:
    //   a) Accept: remove @AiNote. Done.
    //   b) Iterate: update spec annotations + re-add @Stub("what changed").

    @Pure
    @Intent("Parse an ISO-8601 date string (yyyy-MM-dd) and return the day of week.")
    @Contract(
        requires = "dateStr != null",
        ensures  = "$result != null",
        throws_  = {DateTimeParseException.class}
    )
    @Example(label = "weekday", input = "'2024-03-15'", output = "FRIDAY")
    @Example(label = "weekend", input = "'2024-03-16'", output = "SATURDAY")
    @AiNote(
        confidence = Confidence.HIGH,
        assumed    = "Input is always yyyy-MM-dd; datetime strings with time-zone are not handled.",
        open       = "Should this return the locale-specific first day of week? Currently returns ISO DayOfWeek."
    )
    DayOfWeek dayOfWeek(String dateStr) {
        return LocalDate.parse(dateStr).getDayOfWeek();
    }
}
