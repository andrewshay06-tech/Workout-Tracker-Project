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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WorkoutManagerTest.java
 *
 * JUnit tests for the logic layer (WorkoutManager). The tests use "dummy"
 * Workout objects to simulate the CRUD operations and the custom action
 * (calories burned), with no menu and no user typing involved.
 *
 * Every feature has a positive test (it works with good data) and a
 * negative test (it is rejected or handled with bad data).
 */
public class WorkoutManagerTest {

    private WorkoutManager manager;   // a fresh, empty manager for each test
    private Workout running;          // dummy workout #1
    private Workout yoga;             // dummy workout #2

    @TempDir
    Path tempDir;                     // JUnit gives each test its own temporary folder

    /**
     * Runs before every test. Builds a new manager that already holds two
     * dummy workouts so each test starts from the same known state.
     */
    @BeforeEach
    public void setUp() {
        manager = new WorkoutManager();
        running = new Workout(1, "Running 6 mph", 45, 180.0, 9.8, true);
        yoga = new Workout(2, "Yoga", 60, 130.0, 2.5, false);
        manager.addWorkout(running);
        manager.addWorkout(yoga);
    }

    // =====================================================================
    // ADD
    // =====================================================================

    /** Positive: a new workout with a new ID is added and can be found. */
    @Test
    @DisplayName("ADD (positive): a new workout is added to the system")
    public void addWorkout_newWorkout_isAdded() {
        Workout swim = new Workout(3, "Swimming Laps", 30, 140.0, 5.8, true);

        boolean added = manager.addWorkout(swim);

        assertTrue(added, "addWorkout should return true for a new ID");
        assertTrue(manager.idExists(3), "the new ID should exist in the system");
        assertEquals(3, manager.getWorkoutCount(), "count should go from 2 to 3");
        assertEquals("Swimming Laps", manager.findWorkoutById(3).getExerciseName());
    }

    /** Negative: a workout that reuses an existing ID is rejected. */
    @Test
    @DisplayName("ADD (negative): a duplicate ID is rejected and the count stays the same")
    public void addWorkout_duplicateId_isRejected() {
        Workout copy = new Workout(1, "Different Name", 20, 150.0, 5.0, false);

        boolean added = manager.addWorkout(copy);

        assertFalse(added, "addWorkout should return false for a duplicate ID");
        assertEquals(2, manager.getWorkoutCount(), "count should still be 2");
        assertEquals("Running 6 mph", manager.findWorkoutById(1).getExerciseName(),
                "the original workout must not be replaced");
    }

    /** Negative: a null workout is rejected instead of crashing. */
    @Test
    @DisplayName("ADD (negative): a null workout is rejected")
    public void addWorkout_null_isRejected() {
        assertFalse(manager.addWorkout(null));
        assertEquals(2, manager.getWorkoutCount());
    }

    // =====================================================================
    // REMOVE
    // =====================================================================

    /** Positive: an existing workout is removed and can no longer be found. */
    @Test
    @DisplayName("REMOVE (positive): an existing workout is removed")
    public void removeWorkout_existingId_isRemoved() {
        boolean removed = manager.removeWorkout(1);

        assertTrue(removed, "removeWorkout should return true when the ID exists");
        assertFalse(manager.idExists(1), "ID 1 should be gone");
        assertNull(manager.findWorkoutById(1), "finding ID 1 should now return null");
        assertEquals(1, manager.getWorkoutCount(), "count should go from 2 to 1");
    }

    /** Negative: removing an ID that does not exist is handled and changes nothing. */
    @Test
    @DisplayName("REMOVE (negative): a missing ID returns false and nothing is removed")
    public void removeWorkout_missingId_returnsFalse() {
        boolean removed = manager.removeWorkout(999);

        assertFalse(removed, "removeWorkout should return false when the ID is not found");
        assertEquals(2, manager.getWorkoutCount(), "count should still be 2");
    }

    /** Negative: removing the same workout twice only works the first time. */
    @Test
    @DisplayName("REMOVE (negative): removing the same workout twice fails the second time")
    public void removeWorkout_twice_secondFails() {
        assertTrue(manager.removeWorkout(2));
        assertFalse(manager.removeWorkout(2));
    }

    // =====================================================================
    // UPDATE (every attribute except the ID)
    // =====================================================================

    /** Positive: the exercise name can be updated. */
    @Test
    @DisplayName("UPDATE (positive): name can be changed")
    public void updateWorkout_name_changes() {
        assertTrue(manager.updateWorkout(1, "name", "Trail Running"));
        assertEquals("Trail Running", manager.findWorkoutById(1).getExerciseName());
    }

    /** Positive: the duration can be updated. */
    @Test
    @DisplayName("UPDATE (positive): duration can be changed")
    public void updateWorkout_duration_changes() {
        assertTrue(manager.updateWorkout(1, "duration", "60"));
        assertEquals(60, manager.findWorkoutById(1).getDurationMinutes());
    }

    /** Positive: the body weight can be updated. */
    @Test
    @DisplayName("UPDATE (positive): body weight can be changed")
    public void updateWorkout_weight_changes() {
        assertTrue(manager.updateWorkout(1, "weight", "175.5"));
        assertEquals(175.5, manager.findWorkoutById(1).getBodyWeightLbs(), 0.001);
    }

    /** Positive: the MET value can be updated. */
    @Test
    @DisplayName("UPDATE (positive): MET value can be changed")
    public void updateWorkout_met_changes() {
        assertTrue(manager.updateWorkout(1, "met", "10.5"));
        assertEquals(10.5, manager.findWorkoutById(1).getMetValue(), 0.001);
    }

    /** Positive: the completed flag can be updated with T/F or true/false. */
    @Test
    @DisplayName("UPDATE (positive): completed can be changed with F, T, false and true")
    public void updateWorkout_completed_changes() {
        assertTrue(manager.updateWorkout(1, "completed", "F"));
        assertFalse(manager.findWorkoutById(1).isCompleted(), "F should make it false");

        assertTrue(manager.updateWorkout(1, "completed", "t"));
        assertTrue(manager.findWorkoutById(1).isCompleted(), "lowercase t should make it true");

        assertTrue(manager.updateWorkout(1, "completed", "false"));
        assertFalse(manager.findWorkoutById(1).isCompleted(), "false should make it false");

        assertTrue(manager.updateWorkout(1, "completed", "TRUE"));
        assertTrue(manager.findWorkoutById(1).isCompleted(), "TRUE should make it true");
    }

    /** Positive: updating one field leaves the other fields alone. */
    @Test
    @DisplayName("UPDATE (positive): only the chosen field changes")
    public void updateWorkout_onlyChosenFieldChanges() {
        manager.updateWorkout(1, "duration", "90");

        Workout w = manager.findWorkoutById(1);
        assertEquals(90, w.getDurationMinutes());
        assertEquals("Running 6 mph", w.getExerciseName());
        assertEquals(180.0, w.getBodyWeightLbs(), 0.001);
        assertEquals(9.8, w.getMetValue(), 0.001);
        assertTrue(w.isCompleted());
    }

    /** Negative: bad values are rejected and the old values are kept. */
    @Test
    @DisplayName("UPDATE (negative): bad values are rejected and the old value is kept")
    public void updateWorkout_badValues_areRejected() {
        assertFalse(manager.updateWorkout(1, "duration", "abc"), "letters for duration");
        assertFalse(manager.updateWorkout(1, "duration", "-10"), "negative duration");
        assertFalse(manager.updateWorkout(1, "weight", "heavy"), "letters for weight");
        assertFalse(manager.updateWorkout(1, "met", "99"), "MET above 25");
        assertFalse(manager.updateWorkout(1, "completed", "maybe"), "completed must be T or F");
        assertFalse(manager.updateWorkout(1, "name", ""), "blank name");
        assertFalse(manager.updateWorkout(1, "name", "Run,Fast"), "comma in name");

        Workout w = manager.findWorkoutById(1);
        assertEquals(45, w.getDurationMinutes());
        assertEquals(180.0, w.getBodyWeightLbs(), 0.001);
        assertEquals(9.8, w.getMetValue(), 0.001);
        assertEquals("Running 6 mph", w.getExerciseName());
        assertTrue(w.isCompleted());
    }

    /** Negative: a missing ID, the ID field, an unknown field, and null values are all rejected. */
    @Test
    @DisplayName("UPDATE (negative): missing ID, ID field, unknown field and nulls are rejected")
    public void updateWorkout_badTargets_areRejected() {
        assertFalse(manager.updateWorkout(999, "name", "Anything"), "ID not found");
        assertFalse(manager.updateWorkout(1, "id", "50"), "the ID can never be changed");
        assertFalse(manager.updateWorkout(1, "color", "red"), "unknown field");
        assertFalse(manager.updateWorkout(1, null, "x"), "null field");
        assertFalse(manager.updateWorkout(1, "name", null), "null value");
        assertTrue(manager.idExists(1), "ID 1 must still exist");
        assertFalse(manager.idExists(50), "ID 50 must not have been created");
    }

    // =====================================================================
    // CUSTOM ACTION: calories burned = MET x kilograms x hours
    // =====================================================================

    /** Positive: MET 9.8, 180 lbs, 45 minutes is about 600.1 calories. */
    @Test
    @DisplayName("CUSTOM ACTION (positive): MET 9.8, 180 lbs, 45 min is about 600 calories")
    public void calculateCalories_knownWorkout_isAbout600() {
        double calories = manager.calculateCalories(1);

        assertEquals(600.1, calories, 0.1);
    }

    /** Positive: a second hand-checked example (MET 2.5, 130 lbs, 60 min). */
    @Test
    @DisplayName("CUSTOM ACTION (positive): MET 2.5, 130 lbs, 60 min is about 147.4 calories")
    public void calculateCalories_secondExample_isCorrect() {
        // 130 / 2.20462 = 58.967 kg, then 2.5 x 58.967 x 1 hour = 147.4
        assertEquals(147.4, manager.calculateCalories(2), 0.1);
    }

    /** Positive: doubling the duration doubles the calories, so the math uses time correctly. */
    @Test
    @DisplayName("CUSTOM ACTION (positive): doubling the duration doubles the calories")
    public void calculateCalories_doubleDuration_doublesResult() {
        double before = manager.calculateCalories(1);
        manager.updateWorkout(1, "duration", "90");
        double after = manager.calculateCalories(1);

        assertEquals(before * 2, after, 0.001);
    }

    /** Negative: an ID that does not exist gives -1.0 instead of crashing. */
    @Test
    @DisplayName("CUSTOM ACTION (negative): a missing ID returns -1.0")
    public void calculateCalories_missingId_returnsMinusOne() {
        assertEquals(-1.0, manager.calculateCalories(999), 0.0);
    }

    // =====================================================================
    // FIND, LIST, AND FILE LOADING THROUGH THE MANAGER
    // =====================================================================

    /** Positive and negative: finding by ID. */
    @Test
    @DisplayName("FIND: an existing ID is found and a missing ID returns null")
    public void findWorkoutById_foundAndNotFound() {
        assertNotNull(manager.findWorkoutById(2));
        assertEquals("Yoga", manager.findWorkoutById(2).getExerciseName());
        assertNull(manager.findWorkoutById(999));
    }

    /** The list handed out by getAllWorkouts is read-only. */
    @Test
    @DisplayName("DISPLAY: getAllWorkouts returns every workout and cannot be changed from outside")
    public void getAllWorkouts_isReadOnly() {
        List<Workout> all = manager.getAllWorkouts();

        assertEquals(2, all.size());
        assertThrows(UnsupportedOperationException.class, () -> all.add(running));
        assertEquals(2, manager.getWorkoutCount(), "the real list must be unchanged");
    }

    /** Positive: loading a good file through the manager adds the workouts. */
    @Test
    @DisplayName("LOAD FILE (positive): workouts from a text file are added, duplicates and bad lines skipped")
    public void loadFromFile_goodAndBadLines() throws IOException {
        Path file = tempDir.resolve("mixed.txt");
        Files.write(file, List.of(
                "10,Cycling,40,165,8.0,true",      // good
                "11,Rowing,20,185,7.0,F",          // good (F is allowed)
                "1,Duplicate Of Running,30,180,9.8,true",  // duplicate ID
                "12,Bad Line,abc,180,7.0,true",    // bad duration
                "13,Too Few,30,180"));             // missing values

        int added = manager.loadFromFile(file.toString());

        assertEquals(2, added, "two good new workouts");
        assertEquals(4, manager.getWorkoutCount(), "2 dummy + 2 loaded");
        assertEquals(1, manager.getLastLoadDuplicates());
        assertEquals(2, manager.getLastLoadBadLines());
        assertEquals("", manager.getLastLoadError());
    }

    /** Negative: a file that does not exist returns -1 and an error message. */
    @Test
    @DisplayName("LOAD FILE (negative): a missing file returns -1 and an error message")
    public void loadFromFile_missingFile_returnsMinusOne() {
        int added = manager.loadFromFile(tempDir.resolve("nope.txt").toString());

        assertEquals(-1, added);
        assertFalse(manager.getLastLoadError().isEmpty(), "there should be an error message");
        assertEquals(2, manager.getWorkoutCount(), "nothing should be added");
    }
}