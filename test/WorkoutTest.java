import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WorkoutTest.java
 *
 * JUnit tests for the Workout class: the constructor and getters, the
 * setters, and the calorie calculation (the custom action).
 */
public class WorkoutTest {

    /** Positive: the constructor stores all 6 values and the getters return them. */
    @Test
    @DisplayName("WORKOUT (positive): constructor stores all 6 values")
    public void constructor_storesAllSixValues() {
        Workout w = new Workout(5, "Jump Rope", 15, 170.0, 11.8, true);

        assertEquals(5, w.getWorkoutId());
        assertEquals("Jump Rope", w.getExerciseName());
        assertEquals(15, w.getDurationMinutes());
        assertEquals(170.0, w.getBodyWeightLbs(), 0.001);
        assertEquals(11.8, w.getMetValue(), 0.001);
        assertTrue(w.isCompleted());
    }

    /** Positive: every setter changes its value (there is no ID setter). */
    @Test
    @DisplayName("WORKOUT (positive): setters change each value")
    public void setters_changeValues() {
        Workout w = new Workout(5, "Jump Rope", 15, 170.0, 11.8, true);

        w.setExerciseName("Skipping");
        w.setDurationMinutes(20);
        w.setBodyWeightLbs(165.5);
        w.setMetValue(12.3);
        w.setCompleted(false);

        assertEquals("Skipping", w.getExerciseName());
        assertEquals(20, w.getDurationMinutes());
        assertEquals(165.5, w.getBodyWeightLbs(), 0.001);
        assertEquals(12.3, w.getMetValue(), 0.001);
        assertFalse(w.isCompleted());
        assertEquals(5, w.getWorkoutId(), "the ID never changes");
    }

    /** Positive: MET 9.8, 180 lbs, 45 min is about 600.1 calories. */
    @Test
    @DisplayName("CALORIES (positive): MET 9.8, 180 lbs, 45 min is about 600.1")
    public void calculateCaloriesBurned_knownExample() {
        Workout w = new Workout(1, "Running 6 mph", 45, 180.0, 9.8, true);

        // 180 / 2.20462 = 81.647 kg and 45 / 60 = 0.75 hours
        // 9.8 x 81.647 x 0.75 = 600.1
        assertEquals(600.1, w.calculateCaloriesBurned(), 0.1);
    }

    /** Positive: a changed value changes the result (checks the formula uses every input). */
    @Test
    @DisplayName("CALORIES (positive): changing MET, weight or time changes the result the right way")
    public void calculateCaloriesBurned_reactsToEachInput() {
        Workout w = new Workout(1, "Test", 60, 150.0, 5.0, true);
        double base = w.calculateCaloriesBurned();     // 5 x 68.04 kg x 1 hour = 340.2
        assertEquals(340.2, base, 0.1);

        w.setMetValue(10.0);
        assertEquals(base * 2, w.calculateCaloriesBurned(), 0.001, "double the MET, double the calories");

        w.setMetValue(5.0);
        w.setDurationMinutes(30);
        assertEquals(base / 2, w.calculateCaloriesBurned(), 0.001, "half the time, half the calories");

        w.setDurationMinutes(60);
        w.setBodyWeightLbs(300.0);
        assertEquals(base * 2, w.calculateCaloriesBurned(), 0.001, "double the weight, double the calories");
    }

    /** Negative: a heavier-than-expected or lighter result is caught by the check. */
    @Test
    @DisplayName("CALORIES (negative): the result is not the pounds-not-converted number")
    public void calculateCaloriesBurned_poundsAreConvertedToKg() {
        Workout w = new Workout(1, "Running 6 mph", 45, 180.0, 9.8, true);

        double wrongIfPoundsUsed = 9.8 * 180.0 * 0.75;    // 1323.0, what you'd get with no conversion
        assertFalse(Math.abs(w.calculateCaloriesBurned() - wrongIfPoundsUsed) < 1.0,
                "pounds must be converted to kilograms first");
    }

    /** The toString text includes the ID and name. */
    @Test
    @DisplayName("WORKOUT: toString shows the ID and exercise name")
    public void toString_containsIdAndName() {
        Workout w = new Workout(9, "Elliptical", 30, 160.0, 5.0, true);

        assertTrue(w.toString().contains("9"));
        assertTrue(w.toString().contains("Elliptical"));
    }
}