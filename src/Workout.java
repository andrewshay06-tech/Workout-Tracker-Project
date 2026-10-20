/**
 * Workout.java
 *
 * Represents one workout in the Workout Tracker DMS.
 * Each Workout holds 6 pieces of data of 4 different types:
 * an int ID, a String exercise name, an int duration, a double body weight,
 * a double MET value, and a boolean completed flag.
 *
 * This class does not check its own data. The WorkoutValidator class checks
 * everything before a Workout object is created or changed.
 */
public class Workout {

    // Private fields (encapsulation). Other classes use the getters and setters.
    private final int workoutId;       // unique ID, never changes after creation
    private String exerciseName;       // for example "Running 6 mph"
    private int durationMinutes;       // how long the workout lasted
    private double bodyWeightLbs;      // the person's weight in pounds
    private double metValue;           // MET value (intensity) of the exercise
    private boolean completed;         // true if the workout was finished

    // Conversion constants used by the calorie calculation.
    private final double lbsPerKg = 2.20462;
    private final double minutesPerHour = 60.0;

    /**
     * Creates a new Workout with all 6 values.
     *
     * @param workoutId       unique workout ID
     * @param exerciseName    name of the exercise
     * @param durationMinutes length of the workout in minutes
     * @param bodyWeightLbs   body weight in pounds
     * @param metValue        MET value of the exercise
     * @param completed       whether the workout was completed
     */
    public Workout(int workoutId, String exerciseName, int durationMinutes,
                   double bodyWeightLbs, double metValue, boolean completed) {
        this.workoutId = workoutId;
        this.exerciseName = exerciseName;
        this.durationMinutes = durationMinutes;
        this.bodyWeightLbs = bodyWeightLbs;
        this.metValue = metValue;
        this.completed = completed;
    }

    // ---------- Getters ----------

    /** @return the workout ID */
    public int getWorkoutId() {
        return workoutId;
    }

    /** @return the exercise name */
    public String getExerciseName() {
        return exerciseName;
    }

    /** @return the duration in minutes */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** @return the body weight in pounds */
    public double getBodyWeightLbs() {
        return bodyWeightLbs;
    }

    /** @return the MET value */
    public double getMetValue() {
        return metValue;
    }

    /** @return true if the workout was completed */
    public boolean isCompleted() {
        return completed;
    }

    // ---------- Setters (there is no ID setter because the ID never changes) ----------

    /** @param exerciseName the new exercise name */
    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    /** @param durationMinutes the new duration in minutes */
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /** @param bodyWeightLbs the new body weight in pounds */
    public void setBodyWeightLbs(double bodyWeightLbs) {
        this.bodyWeightLbs = bodyWeightLbs;
    }

    /** @param metValue the new MET value */
    public void setMetValue(double metValue) {
        this.metValue = metValue;
    }

    /** @param completed the new completed flag */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Custom action: estimates the calories burned during this workout.
     * Formula: calories = MET x weight in kilograms x duration in hours.
     * Pounds are converted to kilograms (divide by 2.20462) and
     * minutes are converted to hours (divide by 60).
     *
     * Example: MET 9.8, 180 lbs, 45 minutes is about 600.1 calories.
     *
     * @return the estimated calories burned
     */
    public double calculateCaloriesBurned() {
        double weightKg = bodyWeightLbs / lbsPerKg;
        double hours = durationMinutes / minutesPerHour;
        return metValue * weightKg * hours;
    }

    /**
     * Builds a one line description of this workout, good for printing.
     *
     * @return the workout as a String
     */
    @Override
    public String toString() {
        return "Workout #" + workoutId + ": " + exerciseName + ", " + durationMinutes
                + " min, " + bodyWeightLbs + " lbs, MET " + metValue
                + ", completed: " + completed;
    }
}
