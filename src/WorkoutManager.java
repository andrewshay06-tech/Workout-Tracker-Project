import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WorkoutManager.java
 *
 * The logic layer of the Workout Tracker. It holds the list of Workout
 * objects (in memory, since there is no database in Phase 1) and does the
 * CRUD operations plus the calorie calculation. It never prints anything.
 * Every method returns a value so it is easy to test.
 */
public class WorkoutManager {

    private final List<Workout> workouts = new ArrayList<>();
    private final WorkoutFileLoader loader = new WorkoutFileLoader();
    private final WorkoutValidator validator = new WorkoutValidator();

    // Results of the most recent file load, so the menu can report them.
    private String lastLoadError = "";
    private int lastLoadBadLines = 0;
    private int lastLoadDuplicates = 0;
    private List<Integer> lastBadLineNumbers = new ArrayList<>();

    /**
     * Adds a workout to the list if its ID is not already in use.
     *
     * @param workout the workout to add
     * @return true if it was added, false if it was null or the ID already exists
     */
    public boolean addWorkout(Workout workout) {
        if (workout == null || idExists(workout.getWorkoutId())) {
            return false;
        }
        workouts.add(workout);
        return true;
    }

    /**
     * Removes the workout with the given ID.
     *
     * @param id the ID to remove
     * @return true if a workout was removed, false if the ID was not found
     */
    public boolean removeWorkout(int id) {
        Workout workout = findWorkoutById(id);
        if (workout == null) {
            return false;
        }
        return workouts.remove(workout);
    }

    /**
     * Updates one field of one workout. The new value is checked again by the
     * validator, so bad data can never get into the list.
     *
     * @param id       the ID of the workout to change
     * @param field    one of: name, duration, weight, met, completed
     * @param newValue the new value as text
     * @return true if the workout was updated, false if the ID, field or value was bad
     */
    public boolean updateWorkout(int id, String field, String newValue) {
        Workout workout = findWorkoutById(id);
        if (workout == null || field == null || newValue == null) {
            return false;
        }
        if (field.equals("id") || !validator.getFieldError(field, newValue).isEmpty()) {
            return false;   // IDs never change, and the value must be valid
        }
        String value = newValue.trim();
        switch (field) {
            case "name":
                workout.setExerciseName(value);
                return true;
            case "duration":
                workout.setDurationMinutes(Integer.parseInt(value));
                return true;
            case "weight":
                workout.setBodyWeightLbs(Double.parseDouble(value));
                return true;
            case "met":
                workout.setMetValue(Double.parseDouble(value));
                return true;
            case "completed":
                workout.setCompleted(value.equalsIgnoreCase("true"));
                return true;
            default:
                return false;
        }
    }

    /**
     * Looks up a workout by its ID.
     *
     * @param id the ID to look for
     * @return the Workout, or null if there is none with that ID
     */
    public Workout findWorkoutById(int id) {
        for (Workout workout : workouts) {
            if (workout.getWorkoutId() == id) {
                return workout;
            }
        }
        return null;
    }

    /**
     * Checks whether an ID is already in use.
     *
     * @param id the ID to check
     * @return true if a workout already has this ID
     */
    public boolean idExists(int id) {
        return findWorkoutById(id) != null;
    }

    /**
     * Gives back all workouts. The list is read-only so other classes can't
     * change the real list by accident.
     *
     * @return a read-only copy of the list
     */
    public List<Workout> getAllWorkouts() {
        return Collections.unmodifiableList(new ArrayList<>(workouts));
    }

    /** @return how many workouts are in the list */
    public int getWorkoutCount() {
        return workouts.size();
    }

    /**
     * Loads workouts from a text file and adds them to the list.
     * Bad lines and duplicate IDs are skipped and counted.
     *
     * @param filePath the path the user typed
     * @return how many workouts were added, or -1 if the file could not be read
     */
    public int loadFromFile(String filePath) {
        lastLoadError = "";
        lastLoadBadLines = 0;
        lastLoadDuplicates = 0;

        List<Workout> loaded = loader.loadWorkouts(filePath);
        if (!loader.getLastError().isEmpty()) {
            lastLoadError = loader.getLastError();
            return -1;
        }

        lastLoadBadLines = loader.getErrorCount();
        lastBadLineNumbers = loader.getBadLineNumbers();

        int added = 0;
        for (Workout workout : loaded) {
            if (addWorkout(workout)) {
                added++;
            } else {
                lastLoadDuplicates++;   // the ID was already in the list
            }
        }
        return added;
    }

    /**
     * Custom action: calculates the calories burned for one workout.
     *
     * @param id the ID of the workout
     * @return the calories burned, or -1.0 if the ID was not found
     */
    public double calculateCalories(int id) {
        Workout workout = findWorkoutById(id);
        if (workout == null) {
            return -1.0;
        }
        return workout.calculateCaloriesBurned();
    }

    /** @return the problem with the last file load, or an empty String if there was none */
    public String getLastLoadError() {
        return lastLoadError;
    }

    /** @return how many bad lines were skipped in the last file load */
    public int getLastLoadBadLines() {
        return lastLoadBadLines;
    }

    /** @return how many workouts were skipped in the last file load because their ID already existed */
    public int getLastLoadDuplicates() {
        return lastLoadDuplicates;
    }

    /** @return the line numbers of the bad lines in the last file load */
    public List<Integer> getLastBadLineNumbers() {
        return new ArrayList<>(lastBadLineNumbers);
    }
}
