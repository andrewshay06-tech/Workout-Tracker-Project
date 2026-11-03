import javax.swing.SwingUtilities;

/**
 * Main.java
 *
 * Application class for the Workout Tracker DMS (Phase 3: GUI, no database
 * yet). It only starts the window. All of the screen work is in WorkoutFrame
 * and all of the logic is in the same classes used in Phases 1 and 2.
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
            WorkoutFrame frame = new WorkoutFrame();
            frame.setVisible(true);
        });
    }
}