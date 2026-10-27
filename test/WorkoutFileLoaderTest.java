import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WorkoutFileLoaderTest.java
 *
 * JUnit tests that check a text file can be opened and read. Each test makes
 * its own small file in a temporary folder, so the tests work on any computer
 * and never depend on a file path from one person's machine.
 *
 * Positive tests use good files. Negative tests use files that are missing,
 * blank paths, folders, or lines that are not valid.
 */
public class WorkoutFileLoaderTest {

    private WorkoutFileLoader loader;   // a fresh loader for each test

    @TempDir
    Path tempDir;                       // JUnit gives each test its own temporary folder

    /** Runs before every test and creates a new loader. */
    @BeforeEach
    public void setUp() {
        loader = new WorkoutFileLoader();
    }

    /**
     * Helper: writes lines into a new file in the temporary folder.
     *
     * @param name  the file name
     * @param lines the lines to write
     * @return the path of the new file
     */
    private Path makeFile(String name, String... lines) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, List.of(lines));
        return file;
    }

    // =====================================================================
    // OPENING A FILE
    // =====================================================================

    /** Positive: a real file opens and every line is read. */
    @Test
    @DisplayName("OPEN FILE (positive): a real text file opens and its lines are read")
    public void readLines_realFile_opensAndReadsLines() throws IOException {
        Path file = makeFile("workouts.txt",
                "1,Running 6 mph,30,180,9.8,true",
                "2,Yoga,60,130,2.5,false",
                "3,Hiking,90,175,6.0,true");

        List<String> lines = loader.readLines(file.toString());

        assertEquals(3, lines.size(), "the file has 3 lines");
        assertEquals("", loader.getLastError(), "there should be no error");
        assertEquals("2,Yoga,60,130,2.5,false", lines.get(1));
    }

    /** Positive: a path wrapped in quotes (like Windows "Copy as path") still opens. */
    @Test
    @DisplayName("OPEN FILE (positive): a path wrapped in quotes still opens")
    public void readLines_quotedPath_opens() throws IOException {
        Path file = makeFile("quoted.txt", "1,Running 6 mph,30,180,9.8,true");

        List<String> lines = loader.readLines("\"" + file + "\"");

        assertEquals(1, lines.size());
        assertEquals("", loader.getLastError());
    }

    /** Negative: a file that does not exist gives an error message, not a crash. */
    @Test
    @DisplayName("OPEN FILE (negative): a file that does not exist gives an error")
    public void readLines_missingFile_givesError() {
        List<String> lines = loader.readLines(tempDir.resolve("missing.txt").toString());

        assertTrue(lines.isEmpty(), "no lines should be returned");
        assertTrue(loader.getLastError().startsWith("File not found"),
                "the error should say the file was not found");
    }

    /** Negative: blank, spaces-only and null paths are rejected. */
    @Test
    @DisplayName("OPEN FILE (negative): blank, spaces-only and null paths are rejected")
    public void readLines_blankPaths_areRejected() {
        assertTrue(loader.readLines("").isEmpty());
        assertEquals("The file path cannot be blank.", loader.getLastError());

        assertTrue(loader.readLines("     ").isEmpty());
        assertEquals("The file path cannot be blank.", loader.getLastError());

        assertTrue(loader.readLines(null).isEmpty());
        assertEquals("The file path cannot be blank.", loader.getLastError());

        assertTrue(loader.readLines("\"\"").isEmpty(), "just two quote marks is blank too");
        assertEquals("The file path cannot be blank.", loader.getLastError());
    }

    /** Negative: a folder is not a text file. */
    @Test
    @DisplayName("OPEN FILE (negative): a folder is rejected")
    public void readLines_folder_isRejected() {
        List<String> lines = loader.readLines(tempDir.toString());

        assertTrue(lines.isEmpty());
        assertTrue(loader.getLastError().contains("is a folder"));
    }

    /** Negative: a path with characters that can never be valid does not crash. */
    @Test
    @DisplayName("OPEN FILE (negative): an impossible path does not crash")
    public void readLines_weirdPath_doesNotCrash() {
        List<String> lines = loader.readLines("bad\u0000path.txt");

        assertTrue(lines.isEmpty());
        assertFalse(loader.getLastError().isEmpty(), "there should be an error message");
    }

    /** A good read after a bad read clears the old error. */
    @Test
    @DisplayName("OPEN FILE: the error message is cleared after a good file is opened")
    public void readLines_goodAfterBad_clearsError() throws IOException {
        loader.readLines(tempDir.resolve("missing.txt").toString());
        assertFalse(loader.getLastError().isEmpty());

        Path file = makeFile("good.txt", "1,Running 6 mph,30,180,9.8,true");
        loader.readLines(file.toString());

        assertEquals("", loader.getLastError());
    }

    // =====================================================================
    // LOADING WORKOUTS FROM A FILE
    // =====================================================================

    /** Positive: every valid line becomes a Workout with the right values. */
    @Test
    @DisplayName("LOAD (positive): valid lines become Workout objects with the right values")
    public void loadWorkouts_validFile_returnsWorkouts() throws IOException {
        Path file = makeFile("good.txt",
                "1,Running 6 mph,30,180,9.8,true",
                "2,Yoga,60,130.5,2.5,F");

        List<Workout> workouts = loader.loadWorkouts(file.toString());

        assertEquals(2, workouts.size());
        assertEquals(0, loader.getErrorCount());

        Workout first = workouts.get(0);
        assertEquals(1, first.getWorkoutId());
        assertEquals("Running 6 mph", first.getExerciseName());
        assertEquals(30, first.getDurationMinutes());
        assertEquals(180.0, first.getBodyWeightLbs(), 0.001);
        assertEquals(9.8, first.getMetValue(), 0.001);
        assertTrue(first.isCompleted());

        Workout second = workouts.get(1);
        assertEquals(130.5, second.getBodyWeightLbs(), 0.001);
        assertFalse(second.isCompleted(), "F should load as false");
    }

    /** Negative: bad lines are skipped and counted, good lines still load. */
    @Test
    @DisplayName("LOAD (negative): bad lines are skipped and counted while good lines still load")
    public void loadWorkouts_badLines_areSkippedAndCounted() throws IOException {
        Path file = makeFile("mixed.txt",
                "1,Running 6 mph,30,180,9.8,true",     // line 1 good
                "2,Yoga,abc,130,2.5,false",            // line 2 bad duration
                "3,Hiking,90,175",                     // line 3 missing values
                "",                                    // line 4 blank (ignored, not an error)
                "4,Swim,30,140,5.8,maybe",             // line 5 bad completed
                "5,Rowing,20,185,7.0,t");              // line 6 good

        List<Workout> workouts = loader.loadWorkouts(file.toString());

        assertEquals(2, workouts.size(), "only lines 1 and 6 are valid");
        assertEquals(3, loader.getErrorCount(), "lines 2, 3 and 5 are bad");
        assertEquals(List.of(2, 3, 5), loader.getBadLineNumbers());
    }

    /** Negative: loading a missing file gives an empty list and an error. */
    @Test
    @DisplayName("LOAD (negative): a missing file gives an empty list and an error message")
    public void loadWorkouts_missingFile_givesEmptyList() {
        List<Workout> workouts = loader.loadWorkouts(tempDir.resolve("nope.txt").toString());

        assertTrue(workouts.isEmpty());
        assertFalse(loader.getLastError().isEmpty());
    }

    /** An empty file is not an error, it just has no workouts. */
    @Test
    @DisplayName("LOAD: an empty file loads zero workouts with no error")
    public void loadWorkouts_emptyFile_givesNothing() throws IOException {
        Path file = tempDir.resolve("empty.txt");
        Files.write(file, new byte[0]);

        List<Workout> workouts = loader.loadWorkouts(file.toString());

        assertTrue(workouts.isEmpty());
        assertEquals(0, loader.getErrorCount());
        assertEquals("", loader.getLastError());
    }

    // =====================================================================
    // PARSING ONE LINE
    // =====================================================================

    /** Positive: a good line becomes a Workout. */
    @Test
    @DisplayName("PARSE LINE (positive): a valid line becomes a Workout")
    public void parseLine_validLine_returnsWorkout() {
        Workout w = loader.parseLine("7, Weight Lifting ,45,200,6.0, t ");

        assertNotNull(w, "extra spaces around values should be allowed");
        assertEquals(7, w.getWorkoutId());
        assertEquals("Weight Lifting", w.getExerciseName());
        assertTrue(w.isCompleted());
    }

    /** Negative: lines with too few, too many, empty or bad values return null. */
    @Test
    @DisplayName("PARSE LINE (negative): invalid lines return null")
    public void parseLine_invalidLines_returnNull() {
        assertNull(loader.parseLine(null), "null line");
        assertNull(loader.parseLine(""), "empty line");
        assertNull(loader.parseLine("1,Run,30,180,9.8"), "only 5 values");
        assertNull(loader.parseLine("1,Run,30,180,9.8,true,extra"), "7 values");
        assertNull(loader.parseLine("0,Run,30,180,9.8,true"), "ID of 0");
        assertNull(loader.parseLine("1,,30,180,9.8,true"), "blank name");
        assertNull(loader.parseLine("1,Run,0,180,9.8,true"), "duration of 0");
        assertNull(loader.parseLine("1,Run,30,heavy,9.8,true"), "letters for weight");
        assertNull(loader.parseLine("1,Run,30,180,99,true"), "MET too high");
        assertNull(loader.parseLine("1,Run,30,180,9.8,yes"), "yes is not T or F");
    }
}