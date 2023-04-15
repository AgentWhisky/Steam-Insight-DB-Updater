package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;

import static db.Queries.*;
import static db.SteamConnector.*;

/**
 * Class - Defines an Object Used for Connection and Maintenance of a Steam Database
 */
public class DBConnector implements ConnectorInterface {
    // Database Driver
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    // JDBC Connection
    private Connection conn;

    // Logging
    private final Logger logger;

    /**
     * Constructor - Creates a DBConnector Object to handle I/O from Steam Database
     */
    public DBConnector() {
        logger = new Logger();
    }

    /**
     * Constructor - Creates a DBConnector Object to handle I/O from Steam Database with an existing Logger
     */
    public DBConnector(Logger logger) {
        this.logger = logger;
    }

    // *** Public Methods ***

    /**
     * Method to open connection to database with given Database Connection Info
     * @param info is the given connection info
     * @return true on success or false on any failure
     */
    public boolean openConnection(DatabaseInfo info) {
        try {
            Class.forName(JDBC_DRIVER);
            String url = String.format("jdbc:mysql://%s:%s/", info.address(), info.port());

            // Connect to Database
            conn = DriverManager.getConnection(url, info.username(), info.password());

            // Set Database Name
            conn.setCatalog(info.databaseName());

            // Log Connection
            logger.log(Logger.LOG_TYPE_CONNECTION, "Successfully Connected To Database");
            return true;
        }
        // Driver Failure
        catch (ClassNotFoundException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Open Connection To Database - Class Not Found");
        }
        // SQL Errors
        catch (SQLException e) {
            switch(e.getErrorCode()) {
                case 1045 -> logger.log(Logger.LOG_TYPE_ERROR, "Failed to Open Connection To Database - Invalid Login Credentials");
                case 1049 -> logger.log(Logger.LOG_TYPE_ERROR, "Failed to Open Connection To Database - Invalid Database Name");
                case 111 -> logger.log(Logger.LOG_TYPE_ERROR, "Failed to Open Connection To Database - Invalid Address or Port");
                case 8006 -> logger.log(Logger.LOG_TYPE_ERROR, "Failed to Open Connection To Database - Could Not Find Database at Address");
                default -> logger.log(Logger.LOG_TYPE_ERROR, String.format("Failed to Open Connection To Database - SQL Exception {%d}", e.getErrorCode()));
            }
        }
        return false;
    }

    /**
     * Method to close the connection to the database
     */
    public void closeConnection() {
        // Attempt to close Connection
        try {
            if(conn != null) {
                conn.close();
                logger.log(Logger.LOG_TYPE_CONNECTION, "Successfully Disconnected From Database");
            }
            else {
                logger.log(Logger.LOG_TYPE_ERROR, "Failed to Close Connection To Database - Already Closed");
            }
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Close Connection To Database - SQL Exception");
        }
    }

    /**
     * Method to update the database and return counters for its result
     */
    public UpdateResults update() {
        // Update Counters
        int newApps = 0;
        int updatedApps = 0;

        logger.logBreak();
        logger.log(Logger.LOG_TYPE_UPDATE, "===Beginning Database Update===");

        // 1) Attempt to Create AppInfo Table
        createAppInfoTable();

        // 2) Update AppList in Database and add new apps
        newApps += updateAppList();

        // 3) Update All Apps
        updatedApps += updateApps();

        logger.log(Logger.LOG_TYPE_UPDATE, "===Finished Database Update===");
        logger.logBreak();

        return new UpdateResults(newApps, updatedApps);

    }

    /**
     * Method to retrieve metadata from the DataBase
     * @return DatabaseMetaData Object for the connected Database
     */
    public DatabaseMetaData getMetaData() {
        if(!isConnected()) {
            logger.log(Logger.LOG_TYPE_ERROR, "Cannot Retrieve Database Meta Data - Not Connected To Database");
            return null;
        }

        try {
            return conn.getMetaData();
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed To Retrieve Database MetaData");
        }
        return null;
    }


    // *** Private Methods ***
    /**
     * Method to check whether the connection to the database is open
     * @return database connection status
     */
    private boolean isConnected() {
        try {
            return conn != null && !conn.isClosed();
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "SQLException While Checking Connection");
        }

        return false;
    }

    // * Update Methods *

    /**
     * Method to create the AppInfo Table in the connected database
     */
    private void createAppInfoTable() {
        if(!isConnected()) {
            logger.log(Logger.LOG_TYPE_WARNING, "Cannot Create AppInfo Table - Not Connected To Database");
            return;
        }

        // Create SQL Statement
        try(Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableAppInfo); // Execute Create Table Query
            logger.log(Logger.LOG_TYPE_UPDATE, "AppInfo Table Created Successfully");
        }
        catch (SQLException e) {
            // Ignore Table Already Exists Error
            if(e.getSQLState().equals("42S01")) {
                logger.log(Logger.LOG_TYPE_UPDATE, "AppInfo Table Already Exists In Database");
            }
            else {
                logger.log(Logger.LOG_TYPE_ERROR, "Failed to Create AppInfo Table");
            }
        }
    }

    /**
     * Method to update the database app list with new appids from the Steam Web API
     * @return The number of new apps added
     */
    private int updateAppList() {
        HashSet<Integer> existingAppids = getAppids(); // Get Existing appids from Database

        ArrayList<App> appList = getCurrentAppList(existingAppids, logger); // Get AppList
        // Null AppList
        if(appList == null) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed To Retrieve AppList From Steam Web API");
            return 0;
        }

        // Empty AppList
        if(appList.isEmpty()) {
            logger.log(Logger.LOG_TYPE_UPDATE, "No New Apps To Add to Database");
            return 0;
        }

        // Create New Query
        StringBuilder sb = new StringBuilder(insertNewApp);

        // Add Value Sections to SQL Query String
        for(int i = 0; i < appList.size(); i++) {
            sb.append("(?, ?)"); // Add Variable Section
            sb.append((i < appList.size()-1) ? ", " : ";"); // Line Terminators
            sb.append("\n"); //New Line
        }

        // Add Apps to Database
        try(PreparedStatement pStmt = conn.prepareStatement(sb.toString())) {
            int cnt = 1;

            for(App app : appList) {
                pStmt.setInt(cnt++, app.appid());
                pStmt.setString(cnt++, app.name());
            }
            int newApps = pStmt.executeUpdate();
            logger.log(Logger.LOG_TYPE_UPDATE, String.format("AppList Update Finished: %d New Apps Added", newApps));

            return newApps;
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, String.format("Failed to Add %d New Apps to the Database", appList.size()));
        }
        return 0;
    }

    /**
     * Method to update all apps with no type in database
     * @return The Number of Updated Apps
     * > Maximum 200 API Calls per 5-minute block to prevent rate-limiting
     * > Will Exit on 5 Failed Attempts to Update an App
     */
    private int updateApps() {
        final int MAX_API_CALLS = 199; // Max Calls within 5min to prevent RateLimiter
        final int MAX_FAILED_CALLS = 5; // Max Apps to Fail Before Stopping Update

        // Counters
        int apiCallCount = 0;
        int failCount = 0;
        int updatedApps = 0;

        // Get Apps to update
        ArrayList<Integer> appids = getAppsToUpdate();
        logger.log(Logger.LOG_TYPE_UPDATE, String.format("Attempting To Update %d Apps", appids.size()));

        for(int appid : appids) {
            // Prevent RateLimit By Waiting 5Min
            if(apiCallCount >= MAX_API_CALLS) {
                try {

                    logger.log(Logger.LOG_TYPE_WARNING, "API_LIMIT - Waiting 5 Minutes to Prevent RateLimit - Resume at: " + Utils.getTimeString(5));
                    Thread.sleep(5 * 60 * 1000); // Wait 5 Minutes Before Continuing
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                apiCallCount = 0;
            }
            apiCallCount++;


            // Get Update Info
            AppEntry entry = getAppInfoFromAppid(appid, logger);

            // Handle Info Failure
            if(entry == null) {
                logger.log(Logger.LOG_TYPE_ERROR, "Failed To Retrieve Info For: " + appid);
                if(failCount == MAX_FAILED_CALLS) {
                    logger.log(Logger.LOG_TYPE_WARNING, String.format("Failed To Fetch Response For %d Apps - Stopping Update", failCount));
                    return updatedApps;
                }
                failCount++;
            }
            else {
                // Update App
                updateAppDetails(entry);
                updatedApps++;
            }
        }
        return updatedApps;
    }

    // * Utility Methods *

    /**
     * Method to get the full list of appids from the Database
     * @return HashSet of all appids in Database
     */
    public HashSet<Integer> getAppids() {
        if(!isConnected()) {
            logger.log(Logger.LOG_TYPE_ERROR, "Cannot Retrieve AppList from Database - Not Connected To Database");
            return null;
        }

        HashSet<Integer> appids = new HashSet<>();

        // Execute Query for full appid list
        try(Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(getAllAppids);

            while(rs.next()) {
                appids.add(rs.getInt(COLUMN_APPID));
            }
            return appids;
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Retrieve App List From Database");
        }
        return null;
    }

    /**
     * Method to get the list of appids for apps to update
     * @return ArrayList of appids to update
     */
    public ArrayList<Integer> getAppsToUpdate() {
        if(!isConnected()) {
            logger.log(Logger.LOG_TYPE_WARNING, "Cannot Fetch Apps to Update - Not Connected To Database");
            return null;
        }

        ArrayList<Integer> appids = new ArrayList<>();

        // Execute Query to Get Apps Needed To Update
        try(Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(getAppidsToUpdate);

            while(rs.next()) {
                appids.add(rs.getInt(COLUMN_APPID));
            }
            return appids;
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Retrieve Apps To Update From Database");
        }
        return null;
    }

    /**
     * Method to update a database entry (app) with given entry info
     * @param entry is the entry info
     */
    private void updateAppDetails(AppEntry entry) {
        // Setup Prepared Statement
        try(PreparedStatement pStmt = conn.prepareStatement(updateApp)) {
            // Set SQL Items and Execute
            pStmt.setString(1, entry.type());
            pStmt.setString(2, entry.header_image());
            pStmt.setString(3, entry.background());
            pStmt.setInt(4, entry.appid());
            pStmt.executeUpdate();

            // Log Valid or Invalid
            if(!entry.isValid()) {
                logger.log(Logger.LOG_TYPE_UPDATE, "App Updated to Invalid Status: " + entry.appid());
            }
            else {
                logger.log(Logger.LOG_TYPE_UPDATE, String.format("Updated App: {%d - %s}", entry.appid(), entry.type()));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            logger.log(Logger.LOG_TYPE_ERROR, "Failed To Update App: " + entry.appid());
        }
    }
}
