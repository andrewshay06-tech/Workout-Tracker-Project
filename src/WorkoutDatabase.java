import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WorkoutDatabase.java
 *
 * The data layer for Phase 4. It connects to a SQLite database file that the
 * user chooses and does every create, read, update and delete (CRUD) action
 * with SQL, so the workouts now live in the database instead of in a list.
 *
 * The database has one table named workouts:
 *   workout_id        INTEGER PRIMARY KEY
 *   exercise_name     TEXT
 *   duration_minutes  INTEGER
 *   body_weight_lbs   REAL
 *   met_value         REAL
 *   completed         INTEGER (1 = true, 0 = false, since SQLite has no boolean)
 *
 * Safety rules used in this class:
 *  - The file path is never hardcoded. The user types it, and the JDBC
 *    connection string is built by joining "jdbc:sqlite:" and that path.
 *  - Every value typed by the user goes into the SQL as a ? parameter in a
 *    PreparedStatement, never joined into the SQL text.
 *  - Every method catches SQLException and returns a value (false, null, an
 *    empty list or -1.0) with a message in getLastError(), so a database
 *    problem can never crash the program.
 */
public class WorkoutDatabase {
    /**
     * Creates a WorkoutDatabase that is not connected yet. Call connect with
     * the path of a SQLite file before using any other method.
     */
    public WorkoutDatabase() {
    }


    private final WorkoutValidator validator = new WorkoutValidator();

    private Connection connection;                 // null when not connected
    private String connectedPath = "";             // the file that is open right now
    private String lastError = "";                 // message for the last problem, empty if none
    private int skippedRows = 0;                   // rows ignored in the last read because their data was invalid

    // The SQL for reading a workout. The table and column names are fixed.
    private final String selectColumns = "SELECT workout_id, exercise_name, duration_minutes, "
            + "body_weight_lbs, met_value, completed FROM workouts";

    // ==================================================================
    // Connecting
    // ==================================================================

    /**
     * Opens the SQLite database file at the path the user typed.
     * The file must already exist (a missing file is never created), must be a
     * real SQLite file, and must have the workouts table. If anything is wrong
     * the old connection (if there was one) stays open and getLastError()
     * explains the problem.
     *
     * @param filePath the path typed by the user (blank or quoted paths are handled)
     * @return true if the database was opened, false if there was a problem
     */
    public boolean connect(String filePath) {
        lastError = "";

        if (filePath == null || filePath.trim().isEmpty()) {
            lastError = "The database file path cannot be blank.";
            return false;
        }

        String cleaned = filePath.trim();
        // Remove quotes, since Windows "Copy as path" adds them.
        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.isEmpty()) {
            lastError = "The database file path cannot be blank.";
            return false;
        }

        File file;
        try {
            file = new File(cleaned).getAbsoluteFile();
            if (!file.exists()) {
                lastError = "Database file not found: " + cleaned
                        + " (looking in: " + System.getProperty("user.dir") + ")";
                return false;
            }
            if (file.isDirectory()) {
                lastError = cleaned + " is a folder, not a database file.";
                return false;
            }
        } catch (SecurityException e) {
            lastError = "You do not have permission to open that file.";
            return false;
        }

        if (!looksLikeSqliteFile(file)) {
            return false;       // looksLikeSqliteFile already set the error message
        }

        // Build the connection string by joining the driver prefix and the user's path.
        String url = "jdbc:sqlite:" + file.getPath();

        Connection newConnection = null;
        try {
            newConnection = DriverManager.getConnection(url);

            // Wait up to 3 seconds if another program (like DB Browser) is using the file.
            try (Statement statement = newConnection.createStatement()) {
                statement.execute("PRAGMA busy_timeout = 3000");
            }

            // Make sure this is the right kind of database (it must have the workouts table).
            try (Statement statement = newConnection.createStatement()) {
                // Nothing is read here. If the table or a column is missing, an exception is thrown.
                statement.executeQuery(selectColumns + " LIMIT 1").close();
            }
        } catch (SQLException | RuntimeException e) {
            closeQuietly(newConnection);
            lastError = describe(e);
            return false;
        }

        // Success: close the old connection (if any) and keep the new one.
        closeQuietly(connection);
        connection = newConnection;
        connectedPath = file.getPath();
        return true;
    }

    /**
     * Closes the database connection.
     *
     * @return true if a connection was closed, false if there was none
     */
    public boolean disconnect() {
        boolean wasConnected = connection != null;
        closeQuietly(connection);
        connection = null;
        connectedPath = "";
        return wasConnected;
    }

    /**
     * Checks whether a database is open right now.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ==================================================================
    // Create, read, update and delete
    // ==================================================================

    /**
     * Adds a workout to the database (INSERT).
     *
     * @param workout the workout to add
     * @return true if it was added, false if it was null, the ID already exists, or there was a database problem
     */
    public boolean insertWorkout(Workout workout) {
        lastError = "";
        if (workout == null) {
            lastError = "There is no workout to add.";
            return false;
        }
        if (!requireConnection()) {
            return false;
        }

        String sql = "INSERT INTO workouts (workout_id, exercise_name, duration_minutes, "
                + "body_weight_lbs, met_value, completed) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, workout.getWorkoutId());
            statement.setString(2, workout.getExerciseName());
            statement.setInt(3, workout.getDurationMinutes());
            statement.setDouble(4, workout.getBodyWeightLbs());
            statement.setDouble(5, workout.getMetValue());
            statement.setInt(6, workout.isCompleted() ? 1 : 0);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            lastError = describe(e);
            return false;
        }
    }

    /**
     * Deletes the workout with the given ID (DELETE).
     *
     * @param id the ID to delete
     * @return true if a workout was deleted, false if the ID was not found or there was a database problem
     */
    public boolean deleteWorkout(int id) {
        lastError = "";
        if (!requireConnection()) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM workouts WHERE workout_id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            lastError = describe(e);
            return false;
        }
    }

    /**
     * Changes one field of one workout (UPDATE). The new value is checked by
     * the validator again, so bad data can never reach the database.
     *
     * @param id       the ID of the workout to change
     * @param field    one of: name, duration, weight, met, completed
     * @param newValue the new value as text
     * @return true if the workout was updated, false if the ID, field or value was bad or there was a database problem
     */
    public boolean updateWorkout(int id, String field, String newValue) {
        lastError = "";
        if (field == null || newValue == null) {
            lastError = "The field and the new value cannot be empty.";
            return false;
        }
        if (field.equals("id") || !validator.getFieldError(field, newValue).isEmpty()) {
            lastError = field.equals("id") ? "The ID cannot be changed."
                    : validator.getFieldError(field, newValue);
            return false;
        }
        if (!requireConnection()) {
            return false;
        }

        // The column name comes from this fixed list, never from the user.
        String column;
        switch (field) {
            case "name":
                column = "exercise_name";
                break;
            case "duration":
                column = "duration_minutes";
                break;
            case "weight":
                column = "body_weight_lbs";
                break;
            case "met":
                column = "met_value";
                break;
            case "completed":
                column = "completed";
                break;
            default:
                lastError = "Unknown field.";
                return false;
        }

        String value = newValue.trim();
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE workouts SET " + column + " = ? WHERE workout_id = ?")) {
            switch (field) {
                case "name":
                    statement.setString(1, value);
                    break;
                case "duration":
                    statement.setInt(1, Integer.parseInt(value));
                    break;
                case "completed":
                    statement.setInt(1, validator.parseCompleted(value) ? 1 : 0);
                    break;
                default:                                   // weight and met are decimal numbers
                    statement.setDouble(1, Double.parseDouble(value));
                    break;
            }
            statement.setInt(2, id);
            boolean updated = statement.executeUpdate() > 0;
            if (!updated) {
                lastError = "No workout was found with ID " + id + ".";
            }
            return updated;
        } catch (SQLException e) {
            lastError = describe(e);
            return false;
        }
    }

    /**
     * Reads every workout from the database (SELECT), sorted by ID.
     * Rows with invalid data (for example edited by hand outside the program)
     * are skipped and counted in getSkippedRows().
     *
     * @return the workouts, or an empty list if there are none or there was a problem
     */
    public List<Workout> selectAllWorkouts() {
        lastError = "";
        skippedRows = 0;
        List<Workout> workouts = new ArrayList<>();
        if (!requireConnection()) {
            return workouts;
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(selectColumns + " ORDER BY workout_id")) {
            while (rows.next()) {
                Workout workout = rowToWorkout(rows);
                if (workout == null) {
                    skippedRows++;
                } else {
                    workouts.add(workout);
                }
            }
        } catch (SQLException e) {
            lastError = describe(e);
            return new ArrayList<>();
        }
        return workouts;
    }

    /**
     * Looks up one workout by its ID (SELECT).
     *
     * @param id the ID to look for
     * @return the Workout, or null if it was not found (or there was a problem, see getLastError())
     */
    public Workout selectWorkoutById(int id) {
        lastError = "";
        if (!requireConnection()) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                selectColumns + " WHERE workout_id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return rowToWorkout(rows);
                }
            }
        } catch (SQLException e) {
            lastError = describe(e);
        }
        return null;
    }

    /**
     * Checks whether a workout ID is already in the database.
     *
     * @param id the ID to check
     * @return true if it exists (false if it does not, or there was a problem, see getLastError())
     */
    public boolean idExists(int id) {
        lastError = "";
        if (!requireConnection()) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM workouts WHERE workout_id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            lastError = describe(e);
            return false;
        }
    }

    /**
     * Counts the workouts in the database.
     *
     * @return the number of workouts, or -1 if there was a problem
     */
    public int countWorkouts() {
        lastError = "";
        if (!requireConnection()) {
            return -1;
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM workouts")) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException e) {
            lastError = describe(e);
            return -1;
        }
    }

    /**
     * Custom action: calculates the calories burned for a workout stored in the
     * database. Formula: MET x weight in kilograms x duration in hours.
     *
     * @param id the ID of the workout
     * @return the calories burned, or -1.0 if the workout was not found
     */
    public double calculateCalories(int id) {
        Workout workout = selectWorkoutById(id);
        if (workout == null) {
            return -1.0;
        }
        return workout.calculateCaloriesBurned();
    }

    // ==================================================================
    // Status information
    // ==================================================================

    /** @return the full path of the open database file, or an empty String if not connected */
    public String getConnectedPath() {
        return connectedPath;
    }

    /** @return a message about the last problem, or an empty String if the last action worked */
    public String getLastError() {
        return lastError;
    }

    /** @return how many rows were skipped in the last read because their data was invalid */
    public int getSkippedRows() {
        return skippedRows;
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Turns the current row of a ResultSet into a Workout. Every value is
     * checked, so a row that was edited to something invalid is skipped
     * instead of crashing the program.
     *
     * @param rows a ResultSet positioned on a row
     * @return the Workout, or null if any value in the row is invalid
     * @throws SQLException if a column cannot be read
     */
    private Workout rowToWorkout(ResultSet rows) throws SQLException {
        int id = rows.getInt("workout_id");
        String name = rows.getString("exercise_name");
        int duration = rows.getInt("duration_minutes");
        double weight = rows.getDouble("body_weight_lbs");
        double met = rows.getDouble("met_value");
        int completed = rows.getInt("completed");
        if (rows.wasNull()) {
            return null;                                   // completed was NULL
        }

        // Check the values with the same rules used for typed input.
        boolean valid = validator.isValidId(String.valueOf(id))
                && validator.isValidName(name)
                && validator.isValidDuration(String.valueOf(duration))
                && validator.isValidWeight(String.format(Locale.US, "%.2f", weight))
                && validator.isValidMet(String.format(Locale.US, "%.2f", met))
                && (completed == 0 || completed == 1);
        if (!valid) {
            return null;
        }
        return new Workout(id, name.trim(), duration, weight, met, completed == 1);
    }

    /**
     * Checks that a database action can run, and sets an error message if not.
     *
     * @return true if a database is open
     */
    private boolean requireConnection() {
        if (!isConnected()) {
            lastError = "Not connected to a database. Choose a database file and connect first.";
            return false;
        }
        return true;
    }

    /**
     * Checks the first 16 bytes of the file, which every SQLite database starts
     * with the text "SQLite format 3". This stops a random file (like a .txt
     * or a .jar) from being opened as a database.
     *
     * @param file the file to check
     * @return true if it looks like a SQLite database (otherwise lastError is set)
     */
    private boolean looksLikeSqliteFile(File file) {
        byte[] header = new byte[15];
        try (FileInputStream input = new FileInputStream(file)) {
            int read = input.read(header);
            if (read == 15 && new String(header, StandardCharsets.US_ASCII).equals("SQLite format 3")) {
                return true;
            }
            lastError = "That file is not a SQLite database. Choose a .db file made for this program.";
        } catch (IOException | SecurityException e) {
            lastError = "The database file could not be read. Check that it is not open in a way that locks it.";
        }
        return false;
    }

    /**
     * Turns a database exception into a short, friendly message.
     *
     * @param e the exception
     * @return the message to show the user
     */
    private String describe(Exception e) {
        String text = e.getMessage() == null ? "" : e.getMessage();
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("no suitable driver") || lower.contains("slf4j")) {
            return "The SQLite driver is missing. Add the sqlite-jdbc library (with its dependencies) to the project.";
        }
        if (lower.contains("unique constraint") || lower.contains("primary key")) {
            return "A workout with that ID already exists.";
        }
        if (lower.contains("check constraint") || lower.contains("not null constraint")) {
            return "The database rejected a value because it breaks the rules for that column.";
        }
        if (lower.contains("no such table") || lower.contains("no such column")) {
            return "That database does not have the workouts table this program needs.";
        }
        if (lower.contains("locked") || lower.contains("busy")) {
            return "The database is busy because another program is using it. Close or save in the other program, then try again.";
        }
        if (lower.contains("readonly") || lower.contains("read-only")) {
            return "The database file is read-only, so changes cannot be saved.";
        }
        if (lower.contains("not a database") || lower.contains("malformed")) {
            return "That file is not a valid SQLite database.";
        }
        return "Database problem: " + text;
    }

    /**
     * Closes a connection without throwing an error if it fails or is null.
     *
     * @param toClose the connection to close
     * @return true if a connection was closed
     */
    private boolean closeQuietly(Connection toClose) {
        if (toClose == null) {
            return false;
        }
        try {
            toClose.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}