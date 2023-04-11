package ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import db.DBConnector;
import db.Logger;
import db.ConnectorInterface.*;

/**
 * Class - Used to allow for portable MySQL Database Updating for a Steam App Database
 */
public class Menu extends JFrame {
    // Window Icon
    private final ImageIcon LUNA_ICON = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icons/luna.png")));

    // Logging Tool
    private final Logger logger;

    // Database Connector
    private final DBConnector sdbc;

    // Preferences
    private final Preferences prefs;
    private static final String PREF_ADDRESS = "address";
    private static final String PREF_PORT = "port";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_DATABASE_NAME = "databaseName";
    private static final String PREF_INTERVAL = "interval";

    // Connection
    private boolean isConnected;
    private static final String CONNECT_BUTTON = "Connect";
    private static final String DISCONNECT_BUTTON = "Disconnect";

    // Update
    private Timer updateTimer;
    private boolean isRunning;
    private static final String START_UPDATE = "Start Update";
    private static final String STOP_UPDATE = "Stop Update";

    // Run Info Status
    private static final int STATUS_CONN = 0;
    private static final int STATUS_READY = 1;
    private static final int STATUS_UPDATING = 2;

    // Update Worker
    private SwingWorker<Void, Void> worker;

    // *** GUI Components ***
    // Settings Components
    private JTextField addressInput;
    private JTextField portInput;
    private JTextField usernameInput;
    private JTextField passwordInput;
    private JTextField databaseNameInput;
    private JSpinner updateIntervalSpinner;
    private JButton connectButton;
    private JButton updateButton;

    // Database Info
    private JLabel productNameLabel;
    private JLabel productVersionLabel;
    private JLabel driverNameLabel;
    private JLabel driverVersionLabel;

    // Run Info
    private JLabel statusLabel;
    private JLabel nextUpdateLabel;
    private JLabel newAppsLabel;
    private JLabel updatedAppsLabel;

    private int newAppsCount;
    private int updatedAppsCount;

    // Console
    private JTextArea console;

    /**
     * Constructor - Creates a GUI For Steam DB Updater
     */
    public Menu() {

        // *** UI ***
        // Load Preferences
        prefs = Preferences.userRoot().node(this.getClass().getName());

        // Initialize Components
        initComponents();

        // Initialize Frame Settings
        initFrame();

        // *** Updater ***
        logger = new Logger(console); // Create Logger with given JTextArea
        sdbc = new DBConnector(logger); // Send Existing Logger to Connector
        isConnected = false;
        isRunning = false;

        // Setup Counters
        newAppsCount = 0;
        updatedAppsCount = 0;

        // Setup Run Info Section
        updateRunInfo(STATUS_CONN);
    }

    /**
     * Method to initialize the frame settings
     */
    private void initFrame() {

        setIconImage(LUNA_ICON.getImage()); // Load Image
        setTitle("LunaDB-Updater"); // Set Title

        // Set Frame Dimension
        Dimension d = new Dimension(800, 600);
        setSize(d);
        setMinimumSize(d);

        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Method to initialize the window components
     */
    private void initComponents() {

        // Init Input Panel
        JPanel controlPanel = initInputPanel();

        // Init Info Panel
        JPanel infoPanel = initInfoPanel();

        JPanel topPanel = new JPanel(new GridLayout());
        topPanel.add(controlPanel);
        topPanel.add(infoPanel);

        // Init Bottom Console
        console = new JTextArea();
        console.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(console);
        DefaultCaret caret = (DefaultCaret) console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Create MainPanel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Add Components to Panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Add Panel to Frame
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Method to initialize the input (settings) panel
     * @return input panel
     */
    private JPanel initInputPanel() {

        // *** Inputs ***
        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        inputPanel.setBorder(UiUtils.getPaddedBorder(5, 5));

        // Create Address Input
        inputPanel.add(new JLabel("Address: "));
        addressInput = new JTextField(prefs.get(PREF_ADDRESS, "localhost"));
        addressInput.addCaretListener(e -> savePreferences());
        inputPanel.add(addressInput);

        // Create Port Input
        inputPanel.add(new JLabel("Port: "));
        portInput = new JTextField(prefs.get(PREF_PORT, "3306"));
        portInput.addCaretListener(e -> savePreferences());
        inputPanel.add(portInput);

        // Create Username Input
        inputPanel.add(new JLabel("Username: "));
        usernameInput = new JTextField(prefs.get(PREF_USERNAME, ""));
        usernameInput.addCaretListener(e -> savePreferences());
        inputPanel.add(usernameInput);

        // Create Password Input
        inputPanel.add(new JLabel("Password: "));
        passwordInput = new JTextField(prefs.get(PREF_PASSWORD, "")); // Hides Password
        passwordInput.addCaretListener(e -> savePreferences());
        inputPanel.add(passwordInput);

        // Create Database Name Input
        inputPanel.add(new JLabel("Database Name: "));
        databaseNameInput = new JTextField(prefs.get(PREF_DATABASE_NAME, ""));
        databaseNameInput.addCaretListener(e -> savePreferences());
        inputPanel.add(databaseNameInput);

        // Create Update Interval Setting
        inputPanel.add(new JLabel("Update Interval (Hrs): "));
        updateIntervalSpinner = new JSpinner();
        updateIntervalSpinner.setValue(prefs.getInt(PREF_INTERVAL, 1));
        updateIntervalSpinner.addChangeListener(e -> {
            limitJSpinner(e);
            savePreferences();
        });
        inputPanel.add(updateIntervalSpinner);

        // *** Buttons ***
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
        buttonPanel.setBorder(UiUtils.getPaddedBorder(5, 5));

        // Setup Connect Button
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            if(connectButton.getText().equals(CONNECT_BUTTON)) {
                connect();
            }
            else {
                disconnect();
            }

        });

        // Setup Update Button
        updateButton = new JButton(START_UPDATE);
        updateButton.setEnabled(false); // Set as Initially Disabled
        updateButton.addActionListener(e -> {
            if(updateButton.getText().equals(START_UPDATE)) {
                startUpdate();
            }
            else {
                stopUpdate();
            }
        });

        buttonPanel.add(connectButton);
        buttonPanel.add(updateButton);

        // Setup Control Panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(UiUtils.getPaddedAdvancedBorder(0, 5, Color.BLACK, "Settings"));

        controlPanel.add(inputPanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);


        return controlPanel;
    }

    /**
     * Method to initialize the info panel containing Database and Run Info
     * @return Initialized Info Panel
     */
    private JPanel initInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(0, 1));

        // *** Database Info ***
        JPanel databaseInfoPanel = new JPanel(new GridLayout(0, 2, 5, 0));
        databaseInfoPanel.setBorder(UiUtils.getPaddedAdvancedBorder(0, 5, Color.BLACK, "Database Info"));

        // Product Name
        databaseInfoPanel.add(centeredLabel("Product Name:"));
        productNameLabel = centeredLabel("N/A");
        databaseInfoPanel.add(productNameLabel);

        // Product Version
        databaseInfoPanel.add(centeredLabel("Product Version:"));
        productVersionLabel = centeredLabel("N/A");
        databaseInfoPanel.add(productVersionLabel);

        // Driver Name
        databaseInfoPanel.add(centeredLabel("Driver Name:"));
        driverNameLabel = centeredLabel("N/A");
        databaseInfoPanel.add(driverNameLabel);

        // Driver Version
        databaseInfoPanel.add(centeredLabel("Driver Version:"));
        driverVersionLabel = centeredLabel("N/A");
        databaseInfoPanel.add(driverVersionLabel);

        // *** Run Info ***
        JPanel runInfoPanel = new JPanel(new GridLayout(0, 2, 5, 0));
        runInfoPanel.setBorder(UiUtils.getPaddedAdvancedBorder(0, 5, Color.BLACK, "Run Info"));

        // Status
        runInfoPanel.add(centeredLabel("Status:"));
        statusLabel = centeredLabel("N/A");
        runInfoPanel.add(statusLabel);

        // Next Update
        runInfoPanel.add(centeredLabel("Next Update:"));
        nextUpdateLabel = centeredLabel("N/A");
        runInfoPanel.add(nextUpdateLabel);

        // New Apps
        runInfoPanel.add(centeredLabel("New Apps:"));
        newAppsLabel = centeredLabel("N/A");
        runInfoPanel.add(newAppsLabel);

        // Updated Apps
        runInfoPanel.add(centeredLabel("Updated Apps:"));
        updatedAppsLabel = centeredLabel("N/A");
        runInfoPanel.add(updatedAppsLabel);
        

        // Add Component Panels to InfoPanel
        infoPanel.add(databaseInfoPanel);
        infoPanel.add(runInfoPanel);

        return infoPanel;
    }

    // *** Private Utilities ***

    /**
     * Method used to limit the JSpinner Components
     * @param e is the event where the JSpinner Changes
     */
    private void limitJSpinner(ChangeEvent e) {
        // Get Value
        JSpinner s = (JSpinner) e.getSource();
        int curVal = (int) s.getValue();

        // Limit Possible Values
        if(curVal < 1) {
            updateIntervalSpinner.setValue(1);
        }
        if(curVal > 24) {
            updateIntervalSpinner.setValue(24);
        }
    }

    /**
     * Method to create a JLabel that is horizontally centered
     * @param text is the text for the JLabel to contain
     * @return the centered JLabel
     */
    private JLabel centeredLabel(String text) {
        JLabel cntr = new JLabel(text);
        cntr.setHorizontalAlignment(JLabel.CENTER);
        return cntr;
    }

    /**
     * Method to save inputs as preferences
     */
    private void savePreferences() {
        prefs.put(PREF_ADDRESS, addressInput.getText());
        prefs.put(PREF_PORT, portInput.getText());
        prefs.put(PREF_USERNAME, usernameInput.getText());
        prefs.put(PREF_PASSWORD, passwordInput.getText());
        prefs.put(PREF_DATABASE_NAME, databaseNameInput.getText());
        prefs.putInt(PREF_INTERVAL, (int) updateIntervalSpinner.getValue());

        // Immediately Save Preferences
        try {
            prefs.flush();
            prefs.sync();
        }
        catch (BackingStoreException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Save Preferences");
        }

    }

    // *** Button Methods ***
    /**
     * Method to handle connecting to the database
     * > Creates an UpdateSettings object to send to the connector
     * > On Connection Success -> Modifies UI to connected status and sets database info
     * > On Connection Failure -> Do Nothing (Connection Failure Log will Post)
     */
    private void connect() {
        DatabaseInfo us = new DatabaseInfo(
                addressInput.getText(),
                portInput.getText(),
                usernameInput.getText(),
                passwordInput.getText(),
                databaseNameInput.getText()
        );

        // Attempt Connection
        if(sdbc.openConnection(us)) {
            connectButton.setText(DISCONNECT_BUTTON);
            updateButton.setEnabled(true);
            isConnected = true;
            updateDatabaseInfo(); // Update Database Info Section
            updateRunInfo(STATUS_READY); // Update Run Info Section
        }
    }

    /**
     * Method to handle disconnecting from the database
     */
    private void disconnect() {
        stopUpdate(); // Stop Update if In Progress
        sdbc.closeConnection(); // Close Connection

        // Set UI as disconnected
        connectButton.setText(CONNECT_BUTTON);
        updateButton.setEnabled(false);
        isConnected = false;
        updateDatabaseInfo(); // Update Database Info Section
        updateRunInfo(STATUS_CONN); // Update Run Info Section
    }

    /**
     * Method to start a database update on a continuous interval
     *  > Utilizes a separate thread for the updating to keep UI usable
     *  > Prevents Multiple Concurrent Updates by using a Semaphore (isTicking)
     */
    private void startUpdate() {
        if(isConnected && !isRunning) {
            isRunning = true;
            updateButton.setText(STOP_UPDATE);

            // Setup Semaphore to Prevent Multiple Updates
            AtomicBoolean isTicking = new AtomicBoolean(false);

            // Setup Timer for Continuous Updates
            int hours = (int) updateIntervalSpinner.getValue();
            updateTimer = new Timer(hours * 60 * 60 * 1000, e -> {

                // Prevent Multiple Updates at the same time
                if(!isTicking.get()) {
                    // Run Update on Separate Thread to Allow for GUI to Continue Working
                    worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {

                            isTicking.set(true);
                            updateRunInfo(STATUS_UPDATING);

                            // Begin Update
                            UpdateResults ur = sdbc.update();

                            // Increment Counts With Result
                            newAppsCount += ur.newApps();
                            updatedAppsCount += ur.updatedApps();

                            // Update UI
                            updateRunInfo(STATUS_READY);
                            isTicking.set(false);
                            return null;
                        }
                    };
                    worker.execute(); // Begin Thread Execution
                }
                else {
                    logger.log(Logger.LOG_TYPE_WARNING, "Update Could Not Begin - Update In Progress");
                }
            });
            // Start Timer and Begin First Execution Immediately
            updateTimer.setInitialDelay(0);
            updateTimer.start();
        }
    }

    /**
     * Method to stop a database update (Will not stop in-progress update)
     */
    private void stopUpdate() {
        if(updateTimer != null) {
            updateTimer.stop(); // Stop Timer
            updateButton.setText(START_UPDATE); // Update Button Text
            updateRunInfo(STATUS_READY);
            isRunning = false;
            worker.cancel(true); // Force End To Update Thread
        }
    }

    // *** Utility Methods ***
    /**
     * Method to update database info section by attempting fetch to database
     */
    private void updateDatabaseInfo() {
        if(isConnected) {
            // Fetch MetaData and attempt updating labels
            DatabaseMetaData md = sdbc.getMetaData();
            if(md != null) {
                try {
                    productNameLabel.setText(md.getDatabaseProductName());
                    productVersionLabel.setText(md.getDatabaseProductVersion());
                    driverNameLabel.setText(md.getDriverName());
                    driverVersionLabel.setText(md.getDriverVersion());
                }
                catch (SQLException e) {
                    logger.log(Logger.LOG_TYPE_ERROR, "Failed to Fetch Database MetaData");
                }
            }
        }
        // On Failure, Set all to Default (empty)
        else {
            productNameLabel.setText("N/A");
            productVersionLabel.setText("N/A");
            driverNameLabel.setText("N/A");
            driverVersionLabel.setText("N/A");
        }
    }

    /**
     * Method to update the Run Info Section on the GUI
     * @param status is the current status to set
     */
    private void updateRunInfo(int status) {
        // Update Status
        switch (status) {
            case STATUS_CONN -> statusLabel.setText("Waiting For Connection");
            case STATUS_READY -> statusLabel.setText("Ready");
            case STATUS_UPDATING -> statusLabel.setText("Update In Progress");
        }

        // Update Timer
        if(updateTimer != null && updateTimer.isRunning()) {
            // Get Time String of Next Update
            long nextExecutionTimeLong = System.currentTimeMillis() + updateTimer.getDelay();
            LocalTime nextExecutionTime = LocalTime.ofInstant(
                    Instant.ofEpochMilli(nextExecutionTimeLong), ZoneId.systemDefault()
            );
            String nextExecutionString = nextExecutionTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            nextUpdateLabel.setText(nextExecutionString);
        }
        else {
            nextUpdateLabel.setText("Not Running");
        }

        // Update Counter Labels
        newAppsLabel.setText("" + newAppsCount);
        updatedAppsLabel.setText("" + updatedAppsCount);
    }
}


