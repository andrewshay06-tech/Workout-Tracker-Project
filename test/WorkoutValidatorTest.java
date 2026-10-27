import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WorkoutValidatorTest.java
 *
 * JUnit tests for the input validation. These check that good values are
 * accepted and that bad values (letters in a number field, out of range
 * numbers, blanks, odd Java number formats) are always rejected, which is
 * what keeps the user from crashing the program.
 */
public class WorkoutValidatorTest {

    private WorkoutValidator validator;   // a fresh validator for each test

    /** Runs before every test and creates a new validator. */
    @BeforeEach
    public void setUp() {
        validator = new WorkoutValidator();
    }

    /** IDs must be whole numbers from 1 to 999999999. */
    @Test
    @DisplayName("VALIDATE ID: whole numbers are accepted, everything else is rejected")
    public void isValidId() {
        assertTrue(validator.isValidId("1"));
        assertTrue(validator.isValidId(" 42 "));
        assertTrue(validator.isValidId("999999999"));

        assertFalse(validator.isValidId("abc"));
        assertFalse(validator.isValidId("0"));
        assertFalse(validator.isValidId("-5"));
        assertFalse(validator.isValidId("1.5"));
        assertFalse(validator.isValidId(""));
        assertFalse(validator.isValidId("   "));
        assertFalse(validator.isValidId(null));
        assertFalse(validator.isValidId("1000000000"), "too big for the 9 digit limit");
        assertFalse(validator.isValidId("99999999999999999999"), "would overflow an int");
    }

    /** Names must be 1 to 30 characters, have a letter, and use safe characters. */
    @Test
    @DisplayName("VALIDATE NAME: normal names are accepted, blank or unsafe names are rejected")
    public void isValidName() {
        assertTrue(validator.isValidName("Running 6 mph"));
        assertTrue(validator.isValidName("HIIT Circuit"));
        assertTrue(validator.isValidName("Push-ups (wide)"));

        assertFalse(validator.isValidName(""));
        assertFalse(validator.isValidName("    "));
        assertFalse(validator.isValidName(null));
        assertFalse(validator.isValidName("12345"), "needs at least one letter");
        assertFalse(validator.isValidName("Run,Fast"), "commas would break the text file");
        assertFalse(validator.isValidName("Bike!!"), "! is not allowed");
        assertFalse(validator.isValidName("A".repeat(31)), "over 30 characters");
    }

    /** Durations must be whole minutes from 1 to 1440. */
    @Test
    @DisplayName("VALIDATE DURATION: 1 to 1440 whole minutes only")
    public void isValidDuration() {
        assertTrue(validator.isValidDuration("1"));
        assertTrue(validator.isValidDuration("45"));
        assertTrue(validator.isValidDuration("1440"));

        assertFalse(validator.isValidDuration("0"));
        assertFalse(validator.isValidDuration("1441"));
        assertFalse(validator.isValidDuration("-10"));
        assertFalse(validator.isValidDuration("forty"));
        assertFalse(validator.isValidDuration("1.5"));
        assertFalse(validator.isValidDuration(""));
        assertFalse(validator.isValidDuration(null));
    }

    /** Weights must be 0.1 to 1000 pounds with at most 2 decimals. */
    @Test
    @DisplayName("VALIDATE WEIGHT: 0.1 to 1000 lbs only, no odd number formats")
    public void isValidWeight() {
        assertTrue(validator.isValidWeight("0.1"));
        assertTrue(validator.isValidWeight("180"));
        assertTrue(validator.isValidWeight("180.5"));
        assertTrue(validator.isValidWeight("1000"));
        assertTrue(validator.isValidWeight("29"));

        assertFalse(validator.isValidWeight("1001"));
        assertFalse(validator.isValidWeight("-150"));
        assertFalse(validator.isValidWeight("heavy"));
        assertFalse(validator.isValidWeight("NaN"), "Java would parse NaN, we must not");
        assertFalse(validator.isValidWeight("Infinity"));
        assertFalse(validator.isValidWeight("1e3"), "scientific notation is rejected");
        assertFalse(validator.isValidWeight("12d"), "Java would parse 12d, we must not");
        assertFalse(validator.isValidWeight("180.123"), "more than 2 decimals");
        assertFalse(validator.isValidWeight(""));
        assertFalse(validator.isValidWeight(null));
    }

    /** MET values must be 0.5 to 25 with at most 2 decimals. */
    @Test
    @DisplayName("VALIDATE MET: 0.5 to 25 only")
    public void isValidMet() {
        assertTrue(validator.isValidMet("0.5"));
        assertTrue(validator.isValidMet("9.8"));
        assertTrue(validator.isValidMet("25"));

        assertFalse(validator.isValidMet("0.4"));
        assertFalse(validator.isValidMet("26"));
        assertFalse(validator.isValidMet("high"));
        assertFalse(validator.isValidMet("-3"));
        assertFalse(validator.isValidMet("NaN"));
        assertFalse(validator.isValidMet(""));
        assertFalse(validator.isValidMet(null));
    }

    /** Completed accepts T, F, true and false in any capitalization. */
    @Test
    @DisplayName("VALIDATE COMPLETED: T, F, true and false (any case) are accepted")
    public void isValidCompleted() {
        assertTrue(validator.isValidCompleted("T"));
        assertTrue(validator.isValidCompleted("t"));
        assertTrue(validator.isValidCompleted("F"));
        assertTrue(validator.isValidCompleted("f"));
        assertTrue(validator.isValidCompleted("true"));
        assertTrue(validator.isValidCompleted("FALSE"));
        assertTrue(validator.isValidCompleted(" True "));

        assertFalse(validator.isValidCompleted("yes"));
        assertFalse(validator.isValidCompleted("maybe"));
        assertFalse(validator.isValidCompleted("1"));
        assertFalse(validator.isValidCompleted("tt"));
        assertFalse(validator.isValidCompleted(""));
        assertFalse(validator.isValidCompleted(null));
    }

    /** parseCompleted turns T/true into true and F/false into false. */
    @Test
    @DisplayName("PARSE COMPLETED: T and true give true, F and false give false")
    public void parseCompleted() {
        assertTrue(validator.parseCompleted("T"));
        assertTrue(validator.parseCompleted("true"));
        assertTrue(validator.parseCompleted(" t "));

        assertFalse(validator.parseCompleted("F"));
        assertFalse(validator.parseCompleted("false"));
        assertFalse(validator.parseCompleted(null));
    }

    /** getFieldError gives an empty String for good input and a message for bad input. */
    @Test
    @DisplayName("FIELD ERROR: empty for good input, a message for bad input")
    public void getFieldError() {
        assertEquals("", validator.getFieldError("id", "5"));
        assertEquals("", validator.getFieldError("name", "Yoga"));
        assertEquals("", validator.getFieldError("duration", "30"));
        assertEquals("", validator.getFieldError("weight", "150"));
        assertEquals("", validator.getFieldError("met", "5.5"));
        assertEquals("", validator.getFieldError("completed", "T"));

        assertFalse(validator.getFieldError("id", "abc").isEmpty());
        assertFalse(validator.getFieldError("name", "").isEmpty());
        assertFalse(validator.getFieldError("duration", "0").isEmpty());
        assertFalse(validator.getFieldError("weight", "heavy").isEmpty());
        assertFalse(validator.getFieldError("met", "99").isEmpty());
        assertFalse(validator.getFieldError("completed", "maybe").isEmpty());
        assertEquals("Unknown field.", validator.getFieldError("color", "red"));
        assertEquals("Unknown field.", validator.getFieldError(null, "red"));
    }
}