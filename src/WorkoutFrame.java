import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * WorkoutFrame.java
 *
 * The presentation layer for Phase 3: a Java Swing window that replaces the
 * command line menu. The user does everything here with text boxes, a check
 * box, buttons and a table: load a text file, add, update, remove, display
 * all workouts, and calculate calories burned.
 *
 * This class only handles the screen. All of the real work is done by the
 * same WorkoutManager and WorkoutValidator classes from Phases 1 and 2, so
 * the logic did not have to change.
 *
 * Every piece of input is checked with the WorkoutValidator before it is
 * used, and problems show up as a red message at the bottom of the window,
 * so bad input can never crash the program.
 *
 * The handle methods return a message String (just like WorkoutMenu) and the
 * button listeners show it in the status bar. Swing itself forces a few
 * methods to be void (like the button listeners), but all of the logic
 * methods in this class return a value.
 */
public class WorkoutFrame extends JFrame {

    // ------------------------------------------------------------------
    // Logic layer objects (the same classes used in Phases 1 and 2)
    // ------------------------------------------------------------------
    private final WorkoutManager manager = new WorkoutManager();
    private final WorkoutValidator validator = new WorkoutValidator();

    // ------------------------------------------------------------------
    // Look and feel: colors and fonts (kept as instance fields, no static members)
    // ------------------------------------------------------------------
    private final Color navy = new Color(0x14213D);        // header and table header
    private final Color teal = new Color(0x1F7A8C);        // main accent
    private final Color orange = new Color(0xFF6B35);      // highlight accent
    private final Color green = new Color(0x1E7D46);       // success
    private final Color red = new Color(0xB8323A);         // errors and delete
    private final Color gray = new Color(0x5C6B80);        // neutral buttons and hints
    private final Color pageBackground = new Color(0xEEF2F8);
    private final Color cardBackground = Color.WHITE;
    private final Color stripe = new Color(0xF6F8FB);      // alternating table rows
    private final Color selectedRow = new Color(0xCDE6EC); // selected table row
    private final Color textColor = new Color(0x1F2937);

    private final Font baseFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font boldFont = new Font("Segoe UI", Font.BOLD, 14);
    private final Font headingFont = new Font("Segoe UI", Font.BOLD, 16);
    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 28);
    private final Font smallFont = new Font("Segoe UI", Font.PLAIN, 12);

    // Borders for the text boxes: normal and "this box has a problem" (red).
    private final Border normalBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xB8C2D0), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8));
    private final Border errorBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(red, 2),
            BorderFactory.createEmptyBorder(4, 7, 4, 7));

    // ------------------------------------------------------------------
    // Screen components
    // ------------------------------------------------------------------
    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField durationField = new JTextField();
    private final JTextField weightField = new JTextField();
    private final JTextField metField = new JTextField();
    private final JCheckBox completedBox = new JCheckBox("Completed");
    private final JTextField pathField = new JTextField("workouts.txt");

    private final JLabel countLabel = new JLabel("Workouts: 0");
    private final JLabel resultLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();

    // The table. Numbers are stored as numbers so sorting by a column works.
    private final String[] columnNames = {"ID", "Exercise", "Minutes", "Weight (lbs)", "MET", "Completed"};
    private final DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
        /** The table is display only. The user edits workouts with the form. */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /** Tells the table what type each column holds so it sorts correctly. */
        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
                case 0:
                case 2:
                    return Integer.class;
                case 3:
                case 4:
                    return Double.class;
                default:
                    return String.class;
            }
        }
    };
    private final JTable table = new JTable(tableModel);

    // The color the next status message will use (red = error, green = success, navy = info).
    private Color messageColor = navy;

    /**
     * Builds the whole window and shows an empty table, ready for the user.
     */
    public WorkoutFrame() {
        super("Workout Tracker");

        // The window never closes by accident. The user must confirm first.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        setIconImage(createIcon(64));
        getContentPane().setBackground(pageBackground);
        setLayout(new BorderLayout());

        // Style the table and the text boxes before they are placed.
        styleTable();
        for (JTextField field : new JTextField[]{idField, nameField, durationField, weightField, metField, pathField}) {
            field.setFont(baseFont);
            field.setForeground(textColor);
            field.setBorder(normalBorder);
        }
        completedBox.setFont(baseFont);
        completedBox.setForeground(textColor);
        completedBox.setBackground(cardBackground);
        completedBox.setFocusPainted(false);

        // Assemble the three areas of the window.
        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setBackground(pageBackground);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        content.add(buildLeftColumn(), BorderLayout.WEST);
        content.add(buildCenterColumn(), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);

        // Starting messages and an empty table.
        refreshTable();
        clearForm();
        messageColor = navy;
        showStatus("Welcome! Load a text file or add a workout to get started.");

        setSize(1180, 740);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);   // center the window on the screen
    }

    // ==================================================================
    // Building the screen
    // ==================================================================

    /**
     * Builds the colored banner across the top with the icon, title and
     * workout count.
     *
     * @return the header panel
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(navy);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel icon = new JLabel(new ImageIcon(createIcon(52)));
        header.add(icon, BorderLayout.WEST);

        JLabel title = new JLabel("Workout Tracker");
        title.setFont(titleFont);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Log your workouts and estimate the calories you burned");
        subtitle.setFont(baseFont);
        subtitle.setForeground(new Color(0xC9D3E3));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(subtitle);
        header.add(titles, BorderLayout.CENTER);

        countLabel.setFont(headingFont);
        countLabel.setForeground(orange);
        header.add(countLabel, BorderLayout.EAST);
        return header;
    }

    /**
     * Builds the left column: the form card and the calorie result card.
     *
     * @return the left column panel
     */
    private JPanel buildLeftColumn() {
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(340, 400));
        left.add(buildFormCard());
        left.add(Box.createVerticalStrut(14));
        left.add(buildResultCard());
        return left;
    }

    /**
     * Builds the white card with the six workout fields and the Add, Update
     * and Clear buttons.
     *
     * @return the form card
     */
    private JPanel buildFormCard() {
        JPanel card = newCard();
        card.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        JLabel heading = new JLabel("Workout Details");
        heading.setFont(headingFont);
        heading.setForeground(navy);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 2, 0);
        card.add(heading, c);

        JLabel hint = new JLabel("<html>Type an ID or click a row in the table to update, remove or calculate.</html>");
        hint.setFont(smallFont);
        hint.setForeground(gray);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 8, 0);
        card.add(hint, c);

        row = addFormRow(card, c, row, "Workout ID", idField);
        row = addFormRow(card, c, row, "Exercise name", nameField);
        row = addFormRow(card, c, row, "Duration (minutes)", durationField);
        row = addFormRow(card, c, row, "Body weight (lbs)", weightField);
        row = addFormRow(card, c, row, "MET value (intensity)", metField);

        c.gridy = row++;
        c.insets = new Insets(8, 0, 10, 0);
        card.add(completedBox, c);

        // Add, Update and Clear buttons share one row.
        JButton addButton = makeButton("Add", green);
        addButton.setToolTipText("Add a new workout using the values above");
        addButton.addActionListener(e -> showStatus(handleAdd()));

        JButton updateButton = makeButton("Update", teal);
        updateButton.setToolTipText("Change the workout with this ID to the values above");
        updateButton.addActionListener(e -> showStatus(handleUpdate()));

        JButton clearButton = makeButton("Clear", gray);
        clearButton.setToolTipText("Empty the form and unselect the table row");
        clearButton.addActionListener(e -> {
            clearForm();
            messageColor = navy;
            showStatus("Form cleared.");
        });

        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(clearButton);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(buttons, c);

        return card;
    }

    /**
     * Adds one label and text box to the form card, with the label above the box.
     *
     * @param card  the form card
     * @param c     the layout settings to reuse
     * @param row   the next free row number
     * @param label the label text
     * @param field the text box
     * @return the next free row number
     */
    private int addFormRow(JPanel card, GridBagConstraints c, int row, String label, JTextField field) {
        JLabel text = new JLabel(label);
        text.setFont(boldFont);
        text.setForeground(textColor);
        c.gridy = row++;
        c.insets = new Insets(6, 0, 3, 0);
        card.add(text, c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(field, c);
        return row;
    }

    /**
     * Builds the card that shows the result of the calorie calculator.
     *
     * @return the result card
     */
    private JPanel buildResultCard() {
        JPanel card = newCard();
        card.setLayout(new BorderLayout(0, 6));

        JLabel heading = new JLabel("Calories Burned");
        heading.setFont(headingFont);
        heading.setForeground(navy);
        card.add(heading, BorderLayout.NORTH);

        resultLabel.setFont(baseFont);
        resultLabel.setForeground(textColor);
        resultLabel.setVerticalAlignment(SwingConstants.TOP);
        card.add(resultLabel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the center column: the file loader on top, the table in the
     * middle, and the Remove, Calculate and Exit buttons below.
     *
     * @return the center column panel
     */
    private JPanel buildCenterColumn() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // ----- File loader card -----
        JPanel fileCard = newCard();
        fileCard.setLayout(new BorderLayout(10, 0));
        JLabel fileLabel = new JLabel("Text file:");
        fileLabel.setFont(boldFont);
        fileLabel.setForeground(textColor);
        fileCard.add(fileLabel, BorderLayout.WEST);
        fileCard.add(pathField, BorderLayout.CENTER);

        JButton browseButton = makeButton("Browse...", gray);
        browseButton.setToolTipText("Pick a text file from your computer");
        browseButton.addActionListener(e -> showStatus(handleBrowse()));
        JButton loadButton = makeButton("Load File", teal);
        loadButton.setToolTipText("Load the workouts from the file typed in the box");
        loadButton.addActionListener(e -> showStatus(handleLoad()));
        JPanel fileButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        fileButtons.setOpaque(false);
        fileButtons.add(browseButton);
        fileButtons.add(loadButton);
        fileCard.add(fileButtons, BorderLayout.EAST);
        center.add(fileCard, BorderLayout.NORTH);

        // ----- Table -----
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xB8C2D0), 1));
        scroll.getViewport().setBackground(cardBackground);
        center.add(scroll, BorderLayout.CENTER);

        // ----- Action buttons under the table -----
        JButton removeButton = makeButton("Remove Workout", red);
        removeButton.setToolTipText("Delete the workout with the ID in the form (asks first)");
        removeButton.addActionListener(e -> showStatus(handleRemove()));
        JButton calcButton = makeButton("Calculate Calories", orange);
        calcButton.setToolTipText("Estimate the calories burned for the workout with this ID");
        calcButton.addActionListener(e -> showStatus(handleCalories()));
        JButton exitButton = makeButton("Exit", navy);
        exitButton.setToolTipText("Close the program");
        exitButton.addActionListener(e -> handleExit());

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(removeButton);
        leftButtons.add(calcButton);
        actions.add(leftButtons, BorderLayout.WEST);
        actions.add(exitButton, BorderLayout.EAST);
        center.add(actions, BorderLayout.SOUTH);

        return center;
    }

    /**
     * Builds the status bar along the bottom of the window.
     *
     * @return the status bar panel
     */
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xB8C2D0)),
                BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        statusLabel.setFont(boldFont);
        bar.add(statusLabel, BorderLayout.CENTER);
        return bar;
    }

    /**
     * Makes an empty white card with padding, used as a background for each section.
     *
     * @return a new card panel
     */
    private JPanel newCard() {
        JPanel card = new JPanel();
        card.setBackground(cardBackground);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD5DCE6), 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return card;
    }

    /**
     * Makes a flat, colored button that gets darker when the mouse is over it
     * or it is pressed.
     *
     * @param text the button label
     * @param base the normal button color
     * @return the new button
     */
    private JButton makeButton(String text, Color base) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());          // flat look so the color shows on any computer
        button.setFont(boldFont);
        button.setForeground(Color.WHITE);
        button.setBackground(base);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.getModel().addChangeListener(e -> {
            if (button.getModel().isPressed()) {
                button.setBackground(base.darker().darker());
            } else if (button.getModel().isRollover()) {
                button.setBackground(base.darker());
            } else {
                button.setBackground(base);
            }
        });
        return button;
    }

    /**
     * Sets up the table's colors, fonts, column sizes and what happens when
     * a row is clicked.
     *
     * @return true when the table has been styled
     */
    private boolean styleTable() {
        table.setFont(baseFont);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xE3E8EF));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);          // click a column header to sort
        table.setFillsViewportHeight(true);

        // Colored header row.
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(100, 36));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setBackground(navy);
                setForeground(Color.WHITE);
                setFont(boldFont);
                setOpaque(true);
                setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        // Striped rows, centered numbers, and a green Yes / gray No in the last column.
        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, false, row, column);
                if (value instanceof Double) {
                    setText(String.format(Locale.US, "%.1f", (Double) value));
                }
                setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    setBackground(selectedRow);
                    setForeground(navy);
                } else {
                    setBackground(row % 2 == 0 ? cardBackground : stripe);
                    setForeground(textColor);
                }
                if (column == 5) {
                    setFont(boldFont);
                    if (!isSelected) {
                        setForeground("Yes".equals(value) ? green : gray);
                    }
                } else {
                    setFont(baseFont);
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, bodyRenderer);
        table.setDefaultRenderer(Number.class, bodyRenderer);   // Integer columns use this too
        table.setDefaultRenderer(Double.class, bodyRenderer);   // Swing has its own Double renderer, so replace it

        int[] widths = {60, 280, 90, 120, 80, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Clicking a row copies that workout into the form.
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelection();
            }
        });
        return true;
    }

    /**
     * Draws a small dumbbell icon in memory, so no image file is needed.
     *
     * @param size the width and height in pixels
     * @return the icon image
     */
    private BufferedImage createIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(orange);                                   // round orange badge
        g.fillOval(0, 0, size, size);

        g.setColor(Color.WHITE);                              // the dumbbell
        g.fillRoundRect((int) (size * 0.20), (int) (size * 0.46), (int) (size * 0.60), (int) (size * 0.08), 4, 4);
        g.fillRoundRect((int) (size * 0.27), (int) (size * 0.30), (int) (size * 0.09), (int) (size * 0.40), 6, 6);
        g.fillRoundRect((int) (size * 0.64), (int) (size * 0.30), (int) (size * 0.09), (int) (size * 0.40), 6, 6);
        g.fillRoundRect((int) (size * 0.17), (int) (size * 0.36), (int) (size * 0.08), (int) (size * 0.28), 6, 6);
        g.fillRoundRect((int) (size * 0.75), (int) (size * 0.36), (int) (size * 0.08), (int) (size * 0.28), 6, 6);

        g.dispose();
        return image;
    }

    // ==================================================================
    // Button handlers. Each one returns the message to show in the status bar
    // and sets messageColor (red for a problem, green for success, navy for info).
    // ==================================================================

    /**
     * Load File button: loads the workouts from the file path in the text box.
     *
     * @return a summary or an error message
     */
    private String handleLoad() {
        messageColor = red;
        clearFieldMarks();
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            pathField.setBorder(errorBorder);
            pathField.requestFocusInWindow();
            return "Please type the path of a text file (or click Browse) before loading.";
        }

        int added = manager.loadFromFile(path);
        if (added < 0) {
            pathField.setBorder(errorBorder);
            return "Error: " + manager.getLastLoadError();
        }

        refreshTable();
        StringBuilder summary = new StringBuilder();
        summary.append("Loaded ").append(added).append(added == 1 ? " workout." : " workouts.");
        if (manager.getLastLoadBadLines() > 0) {
            summary.append(" Skipped ").append(manager.getLastLoadBadLines())
                    .append(" bad line(s) at line number(s) ")
                    .append(formatLineNumbers(manager.getLastBadLineNumbers())).append(".");
        }
        if (manager.getLastLoadDuplicates() > 0) {
            summary.append(" Skipped ").append(manager.getLastLoadDuplicates())
                    .append(" workout(s) with an ID already in the system.");
        }
        if (added == 0 && manager.getLastLoadBadLines() == 0 && manager.getLastLoadDuplicates() == 0) {
            summary.append(" The file had no workouts in it.");
        }
        messageColor = green;
        return summary.toString();
    }

    /**
     * Browse button: opens a file picker and puts the chosen path in the text box.
     *
     * @return a message about what was chosen
     */
    private String handleBrowse() {
        messageColor = navy;
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setDialogTitle("Choose a workout text file");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            pathField.setText(chosen.getAbsolutePath());
            pathField.setBorder(normalBorder);
            return "File chosen: " + chosen.getName() + ". Click Load File to load it.";
        }
        return "No file was chosen.";
    }

    /**
     * Add button: checks all five text boxes, then adds the new workout.
     *
     * @return a success or error message
     */
    private String handleAdd() {
        messageColor = red;
        clearFieldMarks();

        // Check each box in order. The first problem found is shown to the user.
        String error = firstFieldError();
        if (!error.isEmpty()) {
            return error;
        }

        int id = Integer.parseInt(idField.getText().trim());
        if (manager.idExists(id)) {
            idField.setBorder(errorBorder);
            idField.requestFocusInWindow();
            return "ID " + id + " is already in use. Please pick a different ID, or use Update to change that workout.";
        }

        Workout workout = new Workout(id, nameField.getText().trim(),
                Integer.parseInt(durationField.getText().trim()),
                Double.parseDouble(weightField.getText().trim()),
                Double.parseDouble(metField.getText().trim()),
                completedBox.isSelected());
        if (!manager.addWorkout(workout)) {
            return "The workout could not be added.";
        }

        refreshTable();
        clearForm();
        scrollToId(id);
        messageColor = green;
        return "Workout " + id + " (" + workout.getExerciseName() + ") was added.";
    }

    /**
     * Update button: changes the workout with the ID in the form to the
     * values in the other boxes. The ID itself never changes.
     *
     * @return a success or error message
     */
    private String handleUpdate() {
        messageColor = red;
        clearFieldMarks();

        String error = firstFieldError();
        if (!error.isEmpty()) {
            return error;
        }

        int id = Integer.parseInt(idField.getText().trim());
        if (!manager.idExists(id)) {
            idField.setBorder(errorBorder);
            idField.requestFocusInWindow();
            return "No workout was found with ID " + id + ", so nothing was updated.";
        }

        // Every value was already checked, so each update below should succeed.
        boolean allUpdated = manager.updateWorkout(id, "name", nameField.getText().trim())
                & manager.updateWorkout(id, "duration", durationField.getText().trim())
                & manager.updateWorkout(id, "weight", weightField.getText().trim())
                & manager.updateWorkout(id, "met", metField.getText().trim())
                & manager.updateWorkout(id, "completed", completedBox.isSelected() ? "T" : "F");
        if (!allUpdated) {
            return "The workout could not be fully updated.";
        }

        refreshTable();
        selectRowById(id);
        messageColor = green;
        return "Workout " + id + " was updated.";
    }

    /**
     * Remove Workout button: removes the workout with the ID in the form,
     * after the user confirms.
     *
     * @return a success, cancel or error message
     */
    private String handleRemove() {
        messageColor = red;
        clearFieldMarks();

        String error = fieldError("id", idField);
        if (!error.isEmpty()) {
            return error;
        }
        int id = Integer.parseInt(idField.getText().trim());
        Workout workout = manager.findWorkoutById(id);
        if (workout == null) {
            idField.setBorder(errorBorder);
            return "No workout was found with ID " + id + ", so nothing was removed.";
        }

        // Ask before deleting.
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete workout " + id + " (" + workout.getExerciseName() + ")?\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            messageColor = navy;
            return "Delete cancelled. Nothing was removed.";
        }

        if (manager.removeWorkout(id)) {
            refreshTable();
            clearForm();
            messageColor = green;
            return "Workout " + id + " (" + workout.getExerciseName() + ") was removed.";
        }
        return "No workout was found with ID " + id + ", so nothing was removed.";
    }

    /**
     * Calculate Calories button (the custom action): shows the calories burned
     * for the workout with the ID in the form.
     *
     * @return a success or error message
     */
    private String handleCalories() {
        messageColor = red;
        clearFieldMarks();

        String error = fieldError("id", idField);
        if (!error.isEmpty()) {
            return error;
        }
        int id = Integer.parseInt(idField.getText().trim());
        Workout workout = manager.findWorkoutById(id);
        if (workout == null) {
            idField.setBorder(errorBorder);
            resultLabel.setText("<html><span style='color:#5C6B80'>No workout with ID " + id + ".</span></html>");
            return "No workout was found with ID " + id + ", so no calories were calculated.";
        }

        double calories = manager.calculateCalories(id);
        double weightKg = workout.getBodyWeightLbs() / 2.20462;
        double hours = workout.getDurationMinutes() / 60.0;
        resultLabel.setText(String.format(Locale.US,
                "<html><b>%s</b><br>MET %.1f x %.2f kg x %.2f hours<br>"
                        + "<span style='font-size:20pt; color:#FF6B35'><b>%.1f calories</b></span></html>",
                workout.getExerciseName(), workout.getMetValue(), weightKg, hours, calories));
        messageColor = green;
        return String.format(Locale.US, "Workout %d burned about %.1f calories.", id, calories);
    }

    /**
     * Exit button and window close: asks the user to confirm, then closes the window.
     *
     * @return true if the window was closed, false if the user chose to stay
     */
    private boolean handleExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit Workout Tracker?\nWorkouts are not saved yet, so they will be cleared.",
                "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            return true;
        }
        return false;
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Checks the ID, name, duration, weight and MET boxes in order.
     * The completed check box needs no check since it is only on or off.
     *
     * @return an empty String if every box is valid, otherwise the first error message
     */
    private String firstFieldError() {
        String[] fields = {"id", "name", "duration", "weight", "met"};
        JTextField[] boxes = {idField, nameField, durationField, weightField, metField};
        for (int i = 0; i < fields.length; i++) {
            String error = fieldError(fields[i], boxes[i]);
            if (!error.isEmpty()) {
                return error;
            }
        }
        return "";
    }

    /**
     * Checks one text box with the validator. If it is not valid, the box
     * gets a red border and the cursor.
     *
     * @param field the field name the validator understands
     * @param box   the text box to check
     * @return an empty String if valid, otherwise the error message
     */
    private String fieldError(String field, JTextField box) {
        String error = validator.getFieldError(field, box.getText());
        if (!error.isEmpty()) {
            box.setBorder(errorBorder);
            box.requestFocusInWindow();
        }
        return error;
    }

    /**
     * Puts every text box back to the normal (not red) border.
     *
     * @return true when the borders are reset
     */
    private boolean clearFieldMarks() {
        for (JTextField field : new JTextField[]{idField, nameField, durationField, weightField, metField, pathField}) {
            field.setBorder(normalBorder);
        }
        return true;
    }

    /**
     * Empties the form, unselects the table row, and resets the result card.
     *
     * @return true when the form is cleared
     */
    private boolean clearForm() {
        idField.setText("");
        nameField.setText("");
        durationField.setText("");
        weightField.setText("");
        metField.setText("");
        completedBox.setSelected(false);
        table.clearSelection();
        clearFieldMarks();
        resultLabel.setText("<html><span style='color:#5C6B80'>Enter a workout ID, then click "
                + "Calculate Calories to see the result here.</span></html>");
        return true;
    }

    /**
     * Rebuilds the table from the manager's list, so the screen always shows
     * the current data, and updates the workout count in the header.
     *
     * @return how many workouts are in the table
     */
    private int refreshTable() {
        // Forget any old selection first so the sorter never sees a stale row number.
        table.clearSelection();
        table.getSelectionModel().setAnchorSelectionIndex(-1);
        table.getSelectionModel().setLeadSelectionIndex(-1);
        tableModel.setRowCount(0);
        for (Workout w : manager.getAllWorkouts()) {
            tableModel.addRow(new Object[]{
                    w.getWorkoutId(), w.getExerciseName(), w.getDurationMinutes(),
                    w.getBodyWeightLbs(), w.getMetValue(), w.isCompleted() ? "Yes" : "No"});
        }
        countLabel.setText("Workouts: " + manager.getWorkoutCount());
        return manager.getWorkoutCount();
    }

    /**
     * Copies the selected table row into the form so it can be edited.
     *
     * @return true if a row was selected and copied into the form
     */
    private boolean fillFormFromSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return false;
        }
        Workout w = manager.findWorkoutById((Integer) table.getValueAt(viewRow, 0));
        if (w == null) {
            return false;
        }
        clearFieldMarks();
        idField.setText(String.valueOf(w.getWorkoutId()));
        nameField.setText(w.getExerciseName());
        durationField.setText(String.valueOf(w.getDurationMinutes()));
        weightField.setText(String.valueOf(w.getBodyWeightLbs()));
        metField.setText(String.valueOf(w.getMetValue()));
        completedBox.setSelected(w.isCompleted());

        messageColor = navy;
        showStatus("Workout " + w.getWorkoutId() + " selected. Change any value and click Update, "
                + "or use Remove Workout or Calculate Calories.");
        return true;
    }

    /**
     * Finds the table row for a workout ID and selects it (which also fills the form).
     *
     * @param id the workout ID
     * @return true if the row was found
     */
    private boolean selectRowById(int id) {
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            if ((Integer) table.getValueAt(viewRow, 0) == id) {
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                return true;
            }
        }
        return false;
    }

    /**
     * Scrolls the table so the row for a workout ID is visible, without selecting it.
     *
     * @param id the workout ID
     * @return true if the row was found
     */
    private boolean scrollToId(int id) {
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            if ((Integer) table.getValueAt(viewRow, 0) == id) {
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                return true;
            }
        }
        return false;
    }

    /**
     * Shows a message in the status bar using the current message color.
     * (This method is void because it only updates the screen.)
     *
     * @param message the text to show
     */
    private void showStatus(String message) {
        statusLabel.setForeground(messageColor);
        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
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