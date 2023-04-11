package db;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class - Used to handle logging to a log file and to the default console or a UI JTextField console
 */
public class Logger {
    // Log Directory
    private static final String FILE_DIR = "logs/";

    // Log Types
    public static final int LOG_TYPE_LOG = 0;
    public static final int LOG_TYPE_WARNING = 1;
    public static final int LOG_TYPE_ERROR = 2;
    public static final int LOG_TYPE_UPDATE = 3;
    public static final int LOG_TYPE_QUERY = 4;
    public static final int LOG_TYPE_DELETE = 5;
    public static final int LOG_TYPE_CONNECTION = 6;

    // Log File
    private FileWriter logfile; // Open Logfile
    private boolean open;

    private JTextArea console;

    /**
     * Constructor - Create A New Logger Object
     */
    public Logger() {
        createLogDir(); // Create Log Directory if Necessary
        openLogFile(); // Open New Logfile
    }

    /**
     * Constructor - Create a New Logger Object with an existing console (JTextArea)
     * @param console is the existing console to write to
     */
    public Logger(JTextArea console) {
        this.console = console;

        createLogDir(); // Create Log Directory if Necessary
        openLogFile(); // Open New Logfile
    }

    /**
     * Method to log a given string with a given log type
     * @param type is the log type
     * @param data is the data to log
     */
    public void log(int type, String data) {
        // If File is not Open, Cannot Log
        if(!open) {
            System.err.println("Log File Not Open");
            return;
        }

        // Add Type Header
        String logTypeStr;
        switch(type) {
            case LOG_TYPE_LOG -> logTypeStr = "LOG";
            case LOG_TYPE_WARNING -> logTypeStr = "WARNING";
            case LOG_TYPE_ERROR -> logTypeStr = "ERROR";
            case LOG_TYPE_UPDATE -> logTypeStr = "UPDATE";
            case LOG_TYPE_QUERY -> logTypeStr = "QUERY";
            case LOG_TYPE_DELETE -> logTypeStr = "DELETE";
            case LOG_TYPE_CONNECTION -> logTypeStr = "CONNECTION";
            default -> logTypeStr = "NORMAL";
        }

        // Compile String to Log
        String timeStamp = getTimeStamp();
        String logString = String.format("%s: {%s} %s\n", timeStamp, logTypeStr, data);

        _log(logString);
    }

    /**
     * Method to add a break in the log
     */
    public void logBreak() {
        String logString = "=".repeat(50) + "\n";

        _log(logString);
    }


    // *** Private Methods ***
    /**
     * Method to log given string to console and logfile
     * @param s is the given string
     */
    private void _log(String s) {
        // Print to UI Console
        if(hasConsole()) {
            SwingUtilities.invokeLater(() -> console.append(s));
        }
        // Print to Default Console
        else {
            System.out.print(s);
        }

        // Write to Log File
        try {
            logfile.write(s);
            logfile.flush(); // Flush Buffer to File Immediately
        }
        catch (IOException e) {
            System.err.println("Failed to Write to Log File: " + e.getMessage());
        }
    }

    /**
     * Method to open a new logfile
     */
    private void openLogFile() {
        String filename = FILE_DIR + "log_" + getTimeStamp() + ".txt";
        try {
            File file = new File(filename);
            logfile = new FileWriter(file, true);
            open = true;

            log(LOG_TYPE_LOG, "Start of Logging");
        }
        catch (IOException e) {
            System.err.println("Failed to Create Log File");
        }
    }

    /**
     * Method to close the logfile
     */
    public void closeLogFile() {
        try {
            log(LOG_TYPE_LOG, "End of Logging");
            logfile.close();
            open = false;
        } catch (IOException e) {
            System.err.println("Failed to Close Log File");
        }
    }

    /**
     * Method to create the log directory if None Exists
     */
    private void createLogDir() {
        File dir = new File(FILE_DIR);
        if(!dir.exists()) {
            if(dir.mkdir()) {
                System.out.println("Log Directory Created: " + FILE_DIR);
            }
        }
    }

    /**
     * Method to get a TimeStamp string for logging
     * @return String timestamp
     */
    private String getTimeStamp() {
        String timeFormat = "yyyy-MM-dd HH-mm-ss";

        // Get TimeStamp
        LocalDateTime time = LocalDateTime.now();
        return time.format(DateTimeFormatter.ofPattern(timeFormat));
    }

    /**
     * Method to return if logger was given a UI console
     * @return if UI console exists
     */
    private boolean hasConsole() {
        return console != null;
    }
}
