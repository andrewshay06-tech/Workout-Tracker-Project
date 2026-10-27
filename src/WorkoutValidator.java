/**
 * WorkoutValidator.java
 *
 * Checks every piece of user input (typed in the menu or read from a file)
 * BEFORE it is used. Every method takes the raw text and returns a boolean,
 * so bad data like letters in a number field is rejected and can never crash
 * the program.
 *
 * Numbers are checked with simple patterns first (digits only, with an
 * optional decimal part), so odd Java number formats like "NaN", "1e5" or
 * "12d" are rejected. Then the range is checked.
 */
public class WorkoutValidator {

    // Allowed ranges and sizes (kept as instance constants, no static members).
    private final int maxNameLength = 30;
    private final int maxDurationMinutes = 1440;   // 24 hours
    private final double minWeightLbs = 0.1;
    private final double maxWeightLbs = 1000.0;
    private final double minMet = 0.5;
    private final double maxMet = 25.0;

    /**
     * Checks a workout ID: a whole number from 1 to 999999999.
     *
     * @param text the text the user typed
     * @return true if the text is a valid ID
     */
    public boolean isValidId(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        if (!t.matches("\\d{1,9}")) {      // digits only, at most 9 so it fits in an int
            return false;
        }
        return Integer.parseInt(t) > 0;
    }

    /**
     * Checks an exercise name: not blank, at most 30 characters, has at least
     * one letter, and only uses letters, numbers, spaces and . ' - ( ) / & +
     * (no commas, because commas separate the values in the text file).
     *
     * @param text the text the user typed
     * @return true if the text is a valid name
     */
    public boolean isValidName(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        if (t.isEmpty() || t.length() > maxNameLength) {
            return false;
        }
        if (!t.matches("[A-Za-z0-9 .'()/&+\\-]+")) {
            return false;
        }
        return t.matches(".*[A-Za-z].*");
    }

    /**
     * Checks a duration: a whole number of minutes from 1 to 1440.
     *
     * @param text the text the user typed
     * @return true if the text is a valid duration
     */
    public boolean isValidDuration(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        if (!t.matches("\\d{1,4}")) {
            return false;
        }
        int minutes = Integer.parseInt(t);
        return minutes >= 1 && minutes <= maxDurationMinutes;
    }

    /**
     * Checks a body weight in pounds: a number (up to 2 decimal places)
     * from 30 to 1000.
     *
     * @param text the text the user typed
     * @return true if the text is a valid weight
     */
    public boolean isValidWeight(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        if (!t.matches("\\d{1,4}(\\.\\d{1,2})?")) {
            return false;
        }
        double weight = Double.parseDouble(t);
        return weight >= minWeightLbs && weight <= maxWeightLbs;
    }

    /**
     * Checks a MET value: a number (up to 2 decimal places) from 0.5 to 25.
     *
     * @param text the text the user typed
     * @return true if the text is a valid MET value
     */
    public boolean isValidMet(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        if (!t.matches("\\d{1,2}(\\.\\d{1,2})?")) {
            return false;
        }
        double met = Double.parseDouble(t);
        return met >= minMet && met <= maxMet;
    }

    /**
     * Checks the completed flag: T, F, true or false (any capitalization).
     *
     * @param text the text the user typed
     * @return true if the text is "T", "F", "true" or "false"
     */
    public boolean isValidCompleted(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        return t.equalsIgnoreCase("t") || t.equalsIgnoreCase("f")
                || t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false");
    }

    /**
     * Turns a valid completed flag into a boolean. T and true mean true,
     * everything else (F and false) means false. Check the text with
     * isValidCompleted first.
     *
     * @param text the text the user typed, like "T" or "false"
     * @return true for "T" or "true", false otherwise
     */
    public boolean parseCompleted(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        return t.equalsIgnoreCase("t") || t.equalsIgnoreCase("true");
    }

    /**
     * Checks one field and gives back an error message to show the user.
     *
     * @param field one of: id, name, duration, weight, met, completed
     * @param value the text the user typed
     * @return an empty String if the value is valid, otherwise the error message
     */
    public String getFieldError(String field, String value) {
        if (field == null) {
            return "Unknown field.";
        }
        switch (field) {
            case "id":
                return isValidId(value) ? ""
                        : "The ID must be a whole number from 1 to 999999999.";
            case "name":
                return isValidName(value) ? ""
                        : "The name must be 1 to " + maxNameLength + " characters, include a letter, "
                        + "and use only letters, numbers, spaces and . ' - ( ) / & + (no commas).";
            case "duration":
                return isValidDuration(value) ? ""
                        : "The duration must be a whole number of minutes from 1 to " + maxDurationMinutes + ".";
            case "weight":
                return isValidWeight(value) ? ""
                        : "The weight must be a number from 30 to 1000 pounds (like 180 or 180.5).";
            case "met":
                return isValidMet(value) ? ""
                        : "The MET value must be a number from 0.5 to 25 (like 9.8).";
            case "completed":
                return isValidCompleted(value) ? ""
                        : "Completed must be T or F (or the word true or false).";
            default:
                return "Unknown field.";
        }
    }
}
