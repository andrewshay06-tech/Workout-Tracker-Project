import javax.swing.SwingUtilities;

/**
 * Main.java
 *
 * Application class for the Workout Tracker DMS (Phase 4: GUI with a database).
 * It only starts the window. All of the screen work is in WorkoutDatabaseFrame,
 * all of the database work is in WorkoutDatabase, and the validation is in the
 * same WorkoutValidator class used in Phases 1 to 3.
 *
 * Java requires the main method to be static, so it is the one and only
 * static method in the whole project.
 */
public class Main {

    /**
     * Starts the Workout Tracker window.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Swing windows must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            WorkoutDatabaseFrame frame = new WorkoutDatabaseFrame();
            frame.setVisible(true);
        });
    }
}