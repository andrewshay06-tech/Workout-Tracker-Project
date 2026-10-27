import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkoutFileLoader.java
 *
 * Reads workouts from a plain text file. Each line holds one workout with 6
 * comma separated values in this order:
 *   id,exerciseName,durationMinutes,bodyWeightLbs,metValue,completed
 * Example line: 1,Running 6 mph,30,180,9.8,true
 *
 * Blank lines are ignored. Lines that are not valid are skipped and counted
 * so one bad line never stops the whole file from loading.
 * Bad file paths (blank, missing, a folder, unreadable) never crash the
 * program. They set an error message the menu can show instead.
 */
public class WorkoutFileLoader {

    private final WorkoutValidator validator = new WorkoutValidator();
    private final long maxFileBytes = 5_000_000L;      // refuse huge files
    private int errorCount;                            // bad lines in the last load
    private String lastError = "";                     // file problem from the last read
    private final List<Integer> badLineNumbers = new ArrayList<>();

    /**
     * Reads every line of a text file.
     * If anything goes wrong, an empty list is returned and getLastError()
     * explains the problem.
     *
     * @param filePath the path the user typed (blank or quoted paths are handled)
     * @return the lines of the file, or an empty list on a problem
     */
    public List<String> readLines(String filePath) {
        lastError = "";
        List<String> lines = new ArrayList<>();

        if (filePath == null || filePath.trim().isEmpty()) {
            lastError = "The file path cannot be blank.";
            return lines;
        }

        String cleaned = filePath.trim();
        // Remove quotes, since Windows "Copy as path" adds them.
        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.isEmpty()) {
            lastError = "The file path cannot be blank.";
            return lines;
        }

        try {
            Path path = Paths.get(cleaned);
            if (!Files.exists(path)) {
                lastError = "File not found: " + cleaned
                        + " (looking in: " + System.getProperty("user.dir") + ")";
                return lines;
            }
            if (Files.isDirectory(path)) {
                lastError = cleaned + " is a folder, not a text file.";
                return lines;
            }
            if (Files.size(path) > maxFileBytes) {
                lastError = "That file is too large to load (limit is 5 MB).";
                return lines;
            }
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (MalformedInputException e) {
                // Not UTF-8 (for example an older Windows file), so try again.
                lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
            }
        } catch (InvalidPathException e) {
            lastError = "That is not a valid file path.";
            lines = new ArrayList<>();
        } catch (IOException e) {
            lastError = "The file could not be read. Make sure it is a plain text file you can open.";
            lines = new ArrayList<>();
        } catch (SecurityException e) {
            lastError = "You do not have permission to read that file.";
            lines = new ArrayList<>();
        }
        return lines;
    }

    /**
     * Turns one line of text into a Workout. Every value is checked by the
     * WorkoutValidator.
     *
     * @param line one line from the file
     * @return the Workout, or null if the line is not valid
     */
    public Workout parseLine(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split(",", -1);   // -1 keeps empty values so they get rejected
        if (parts.length != 6) {
            return null;
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        if (!validator.isValidId(parts[0]) || !validator.isValidName(parts[1])
                || !validator.isValidDuration(parts[2]) || !validator.isValidWeight(parts[3])
                || !validator.isValidMet(parts[4]) || !validator.isValidCompleted(parts[5])) {
            return null;
        }
        return new Workout(Integer.parseInt(parts[0]), parts[1], Integer.parseInt(parts[2]),
                Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                validator.parseCompleted(parts[5]));
    }

    /**
     * Loads every valid workout from a file. Bad lines are skipped and counted.
     * If the file itself could not be read, the list is empty and
     * getLastError() has the reason.
     *
     * @param filePath the path to the text file
     * @return the valid workouts found in the file
     */
    public List<Workout> loadWorkouts(String filePath) {
        errorCount = 0;
        badLineNumbers.clear();
        List<Workout> workouts = new ArrayList<>();

        List<String> lines = readLines(filePath);
        if (!lastError.isEmpty()) {
            return workouts;
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith("\uFEFF")) {
                line = line.substring(1);       // remove the invisible BOM some editors add
            }
            if (line.trim().isEmpty()) {
                continue;                       // blank lines are ignored
            }
            Workout workout = parseLine(line);
            if (workout == null) {
                errorCount++;
                badLineNumbers.add(i + 1);      // file line numbers start at 1
            } else {
                workouts.add(workout);
            }
        }
        return workouts;
    }

    /** @return how many bad lines were skipped in the last load */
    public int getErrorCount() {
        return errorCount;
    }

    /** @return the line numbers of the bad lines skipped in the last load */
    public List<Integer> getBadLineNumbers() {
        return new ArrayList<>(badLineNumbers);
    }

    /** @return the file problem from the last read, or an empty String if there was none */
    public String getLastError() {
        return lastError;
    }
}
