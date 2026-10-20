import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * WorkoutMenu.java
 *
 * The presentation layer for Phase 1: a command line menu. It asks the user
 * for input, checks it with the WorkoutValidator, and hands good data to the
 * WorkoutManager. The handle methods return a message String and run() prints
 * it, so the menu logic stays easy to follow.
 *
 * The program only ends when the user picks Exit (or the input stream is
 * closed). Typing "cancel" at any prompt goes back to the menu.
 */
public class WorkoutMenu {

    private final Scanner in;
    private final WorkoutManager manager = new WorkoutManager();
    private final WorkoutValidator validator = new WorkoutValidator();
    private boolean inputEnded = false;       // true if there is no more input to read
    private final String cancelMessage = "Cancelled. Back to the main menu.";

    // Names and prompts for the 5 fields that can be updated (the ID never changes).
    private final String[] updateFields = {"name", "duration", "weight", "met", "completed"};
    private final String[] updatePrompts = {
            "New exercise name: ", "New duration in minutes: ", "New body weight in lbs: ",
            "New MET value: ", "New completed value (true or false): "};

    /**
     * Creates the menu.
     *
     * @param in the Scanner that reads what the user types
     */
    public WorkoutMenu(Scanner in) {
        this.in = in;
    }

    /**
     * Runs the main menu loop until the user chooses Exit.
     *
     * @return true if the user chose Exit, false if the input ended first
     */
    public boolean run() {
        System.out.println("=== Workout Tracker ===");
        boolean exitChosen = false;

        while (!exitChosen && !inputEnded) {
            System.out.println(showMenu());
            String choice = readLine("Enter your choice (1-7): ");
            if (choice == null) {
                break;
            }

            String message;
            try {
                switch (choice.trim()) {
                    case "1":
                        message = handleLoad();
                        break;
                    case "2":
                        message = handleDisplay();
                        break;
                    case "3":
                        message = handleAdd();
                        break;
                    case "4":
                        message = handleUpdate();
                        break;
                    case "5":
                        message = handleRemove();
                        break;
                    case "6":
                        message = handleCalories();
                        break;
                    case "7":
                        exitChosen = true;
                        message = "Goodbye! Thanks for using Workout Tracker.";
                        break;
                    default:
                        message = "Invalid choice. Please enter a number from 1 to 7.";
                }
            } catch (RuntimeException e) {
                // Last safety net so a surprise problem never ends the program.
                message = "Something went wrong with that request. Back to the main menu.";
            }
            System.out.println(message);
        }

        if (inputEnded && !exitChosen) {
            System.out.println("\nNo more input is available, so the program is closing.");
        }
        return exitChosen;
    }

    /**
     * Builds the text of the main menu.
     *
     * @return the menu as a String
     */
    public String showMenu() {
        return "\n--- Main Menu ---\n"
                + "1. Load workouts from a text file\n"
                + "2. Display all workouts\n"
                + "3. Add a workout\n"
                + "4. Update a workout\n"
                + "5. Remove a workout\n"
                + "6. Calculate calories burned\n"
                + "7. Exit";
    }

    // ------------------------------------------------------------------
    // Menu option handlers. Each one returns the message to print.
    // ------------------------------------------------------------------

    /**
     * Option 1: asks for a file path and loads the workouts in it.
     *
     * @return a summary or an error message
     */
    private String handleLoad() {
        String path = readLine("Enter the path of the text file (or type cancel): ");
        if (path == null || path.trim().equalsIgnoreCase("cancel")) {
            return cancelMessage;
        }

        int added = manager.loadFromFile(path);
        if (added < 0) {
            return "Error: " + manager.getLastLoadError();
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Loaded ").append(added).append(added == 1 ? " workout." : " workouts.");
        if (manager.getLastLoadBadLines() > 0) {
            summary.append("\nSkipped ").append(manager.getLastLoadBadLines())
                    .append(" bad line(s) at line number(s): ")
                    .append(formatLineNumbers(manager.getLastBadLineNumbers()));
        }
        if (manager.getLastLoadDuplicates() > 0) {
            summary.append("\nSkipped ").append(manager.getLastLoadDuplicates())
                    .append(" workout(s) because the ID was already in the system.");
        }
        if (added == 0 && manager.getLastLoadBadLines() == 0 && manager.getLastLoadDuplicates() == 0) {
            summary.append(" The file had no workouts in it.");
        }
        return summary.toString();
    }

    /**
     * Option 2: builds a table of every workout.
     *
     * @return the table, or a message if there are no workouts yet
     */
    private String handleDisplay() {
        List<Workout> all = manager.getAllWorkouts();
        if (all.isEmpty()) {
            return "There are no workouts in the system yet.";
        }
        StringBuilder table = new StringBuilder();
        table.append("\n").append(headerRow()).append("\n");
        table.append("-".repeat(headerRow().length())).append("\n");
        for (Workout workout : all) {
            table.append(formatRow(workout)).append("\n");
        }
        table.append("Total workouts: ").append(all.size());
        return table.toString();
    }

    /**
     * Option 3: asks for all 6 values (checking each one) and adds the workout.
     *
     * @return the new workout or a cancel message
     */
    private String handleAdd() {
        System.out.println("Add a workout (type cancel at any prompt to go back).");

        String id = promptNewId();
        if (id == null) {
            return cancelMessage;
        }
        String name = promptValidField("name", "Exercise name: ");
        if (name == null) {
            return cancelMessage;
        }
        String duration = promptValidField("duration", "Duration in minutes: ");
        if (duration == null) {
            return cancelMessage;
        }
        String weight = promptValidField("weight", "Body weight in lbs: ");
        if (weight == null) {
            return cancelMessage;
        }
        String met = promptValidField("met", "MET value (like 9.8): ");
        if (met == null) {
            return cancelMessage;
        }
        String completed = promptValidField("completed", "Completed? (true or false): ");
        if (completed == null) {
            return cancelMessage;
        }

        Workout workout = new Workout(Integer.parseInt(id), name, Integer.parseInt(duration),
                Double.parseDouble(weight), Double.parseDouble(met), completed.equalsIgnoreCase("true"));
        if (manager.addWorkout(workout)) {
            return "Workout added:\n" + headerRow() + "\n" + formatRow(workout);
        }
        return "Error: the workout could not be added.";
    }

    /**
     * Option 4: asks for an ID, a field, and a new value, then updates the workout.
     *
     * @return the updated workout or an error message
     */
    private String handleUpdate() {
        String idText = promptValidField("id", "ID of the workout to update: ");
        if (idText == null) {
            return cancelMessage;
        }
        int id = Integer.parseInt(idText);
        Workout workout = manager.findWorkoutById(id);
        if (workout == null) {
            return "No workout was found with ID " + id + ".";
        }

        System.out.println("\nCurrent workout:\n" + headerRow() + "\n" + formatRow(workout));
        System.out.println("\nWhich field do you want to change? (The ID cannot be changed.)");
        System.out.println("1. Exercise name\n2. Duration\n3. Body weight\n4. MET value\n5. Completed");

        int fieldIndex = promptFieldChoice();
        if (fieldIndex < 0) {
            return cancelMessage;
        }

        String field = updateFields[fieldIndex];
        String newValue = promptValidField(field, updatePrompts[fieldIndex]);
        if (newValue == null) {
            return cancelMessage;
        }

        if (manager.updateWorkout(id, field, newValue)) {
            return "Workout updated:\n" + headerRow() + "\n" + formatRow(manager.findWorkoutById(id));
        }
        return "Error: the workout could not be updated.";
    }

    /**
     * Option 5: asks for an ID and removes that workout.
     *
     * @return a confirmation or a not found message
     */
    private String handleRemove() {
        String idText = promptValidField("id", "ID of the workout to remove: ");
        if (idText == null) {
            return cancelMessage;
        }
        int id = Integer.parseInt(idText);
        if (manager.removeWorkout(id)) {
            return "Workout " + id + " was removed.";
        }
        return "No workout was found with ID " + id + ", so nothing was removed.";
    }

    /**
     * Option 6 (custom action): asks for an ID and shows the calories burned.
     *
     * @return the calculation result or a not found message
     */
    private String handleCalories() {
        String idText = promptValidField("id", "ID of the workout to calculate: ");
        if (idText == null) {
            return cancelMessage;
        }
        int id = Integer.parseInt(idText);
        double calories = manager.calculateCalories(id);
        if (calories < 0) {
            return "No workout was found with ID " + id + ", so no calories were calculated.";
        }
        Workout workout = manager.findWorkoutById(id);
        double weightKg = workout.getBodyWeightLbs() / 2.20462;
        double hours = workout.getDurationMinutes() / 60.0;
        return String.format(Locale.US,
                "%s%n  MET %.2f x %.2f kg x %.2f hours%n  Estimated calories burned: %.1f",
                workout.getExerciseName(), workout.getMetValue(), weightKg, hours, calories);
    }

    // ------------------------------------------------------------------
    // Prompt helpers. These keep asking until the input is valid.
    // ------------------------------------------------------------------

    /**
     * Prints a prompt and reads one line. If there is no more input, it
     * remembers that and returns null instead of crashing.
     *
     * @param prompt the text to show
     * @return the line the user typed, or null if the input ended
     */
    private String readLine(String prompt) {
        System.out.print(prompt);
        try {
            if (!in.hasNextLine()) {
                inputEnded = true;
                return null;
            }
            return in.nextLine();
        } catch (IllegalStateException | NoSuchElementException e) {
            inputEnded = true;
            return null;
        }
    }

    /**
     * Keeps asking until the user types a valid value for a field.
     *
     * @param field  the field name the validator understands
     * @param prompt the text to show
     * @return the valid (trimmed) text, or null if the user typed cancel or the input ended
     */
    private String promptValidField(String field, String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text == null || text.trim().equalsIgnoreCase("cancel")) {
                return null;
            }
            String error = validator.getFieldError(field, text);
            if (error.isEmpty()) {
                return text.trim();
            }
            System.out.println("  Error: " + error);
        }
    }

    /**
     * Keeps asking until the user types a valid ID that is not already used.
     *
     * @return the new ID as text, or null if the user typed cancel or the input ended
     */
    private String promptNewId() {
        while (true) {
            String id = promptValidField("id", "Workout ID (a new whole number): ");
            if (id == null) {
                return null;
            }
            if (!manager.idExists(Integer.parseInt(id))) {
                return id;
            }
            System.out.println("  Error: ID " + id + " is already in use. Please pick a different ID.");
        }
    }

    /**
     * Keeps asking until the user picks a field number from 1 to 5.
     *
     * @return the index (0 to 4) into the update field arrays, or -1 if cancelled
     */
    private int promptFieldChoice() {
        while (true) {
            String text = readLine("Field number (1-5): ");
            if (text == null || text.trim().equalsIgnoreCase("cancel")) {
                return -1;
            }
            String t = text.trim();
            if (t.matches("[1-5]")) {
                return Integer.parseInt(t) - 1;
            }
            System.out.println("  Error: please enter a number from 1 to 5.");
        }
    }

    // ------------------------------------------------------------------
    // Formatting helpers
    // ------------------------------------------------------------------

    /**
     * Builds the header line of the workout table.
     *
     * @return the header row
     */
    private String headerRow() {
        return String.format(Locale.US, "%-6s %-30s %8s %12s %6s %s",
                "ID", "Exercise", "Minutes", "Weight(lbs)", "MET", "Completed");
    }

    /**
     * Builds one table row for a workout.
     *
     * @param workout the workout to show
     * @return the row as a String
     */
    private String formatRow(Workout workout) {
        return String.format(Locale.US, "%-6d %-30s %8d %12.1f %6.1f %s",
                workout.getWorkoutId(), workout.getExerciseName(), workout.getDurationMinutes(),
                workout.getBodyWeightLbs(), workout.getMetValue(), workout.isCompleted());
    }

    /**
     * Turns a list of line numbers into text. Only the first 10 are shown.
     *
     * @param numbers the line numbers
     * @return text like "4, 9, 12"
     */
    private String formatLineNumbers(List<Integer> numbers) {
        StringBuilder text = new StringBuilder();
        int shown = Math.min(numbers.size(), 10);
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(numbers.get(i));
        }
        if (numbers.size() > shown) {
            text.append(", ... and ").append(numbers.size() - shown).append(" more");
        }
        return text.toString();
    }
}
