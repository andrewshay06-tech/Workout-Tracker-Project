import java.util.Scanner;

/**
 * Main.java
 *
 * Application class for the Workout Tracker DMS (Phase 1: logic and data
 * validation, command line only). It only creates the menu and starts it.
 *
 * Java requires the main method to be static, so it is the one and only
 * static method in the whole project..
 */
public class Main {

    /**
     * Starts the Workout Tracker.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        WorkoutMenu menu = new WorkoutMenu(scanner);
        menu.run();
    }
}
