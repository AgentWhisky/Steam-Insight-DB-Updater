package db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public Logger() {
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

        // Print to Console
        System.out.print(logString);

        // Write to Log File
        try {
            logfile.write(logString);
            logfile.flush();
        }
        catch (IOException e) {
            System.err.println("Failed to Write to Log File: " + e.getMessage());
        }
    }


    // *** Private Methods ***

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
}
