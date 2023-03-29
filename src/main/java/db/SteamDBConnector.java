package db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import static db.Queries.*;
import static db.Utilities.*;

public class SteamDBConnector {
    private static final int MAX_DAYS_OLD = 999; // Maximum Number of Days Old And App Needs to Be

    // Database Info
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private final String DB_URL;
    private final String DB_NAME;
    private final String USER;
    private final String PASSWORD;

    // Properties file
    private final Properties props;

    // JDBC Connection
    private Connection conn;
    private boolean connected;

    // Log File
    Logger logger;

    // Steam Web API Response Storage
    record App(int appid, String name){} // Used to Store Result from AppList Response


    /**
     * Class - Used to Connect to Chosen Database and Keep Steam Web API Information Up-to-date
     */
    public SteamDBConnector() {
        props = importPropFile(); // Import Properties File
        if(props == null) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Import Properties File");
            System.exit(1); // Exit Program (Properties File Required)
        }

        // Connection Info pulled from Properties File
        DB_URL = String.format("jdbc:mysql://%s:%s/", props.getProperty("db.address"), props.getProperty("db.port"));
        DB_NAME = props.getProperty("db.name");

        // Get Login Info
        USER = props.getProperty("db.user");
        PASSWORD = props.getProperty("db.password");
        connected = false;

        // Initialize Logger
        logger = new Logger(); // Setup Logger
    }

    /**
     * Method to create the AppInfo Table in the connected database
     */
    public void createAppInfoTable() {
        if(!connected) {
            logger.log(Logger.LOG_TYPE_WARNING, "Cannot Create AppInfo Table - Not Connected To Database");
            return;
        }

        // Create SQL Statement
        try(Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableAppInfo); // Execute Query
            logger.log(Logger.LOG_TYPE_UPDATE, "AppInfo Table Created");
        }
        catch (SQLException e) {
            // Ignore Table Already Exists Error
            if(e.getSQLState().equals("42S01")) {
                logger.log(Logger.LOG_TYPE_WARNING, "AppInfo Table Already Exists");
            }
            else {
                logger.log(Logger.LOG_TYPE_ERROR, "Failed to Create AppInfo Table");
            }
        }
    }

    /**
     * Method to update the database app list by adding any new apps not in the database
     */
    public void updateAppList() {
        logger.log(Logger.LOG_TYPE_UPDATE, "Beginning AppList Update");

        // Get the Existing AppList from the Database
        HashSet<Integer> dbAppList = getAllAppIDsFromDB();
        if(dbAppList == null) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Get Existing App List from MySQL Database");
            return;
        }

        // Fetch App List from Steam Web API with Database Set
        ArrayList<App> appList = getCurrentAppList(dbAppList);
        if(appList == null) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed To Fetch App List from Steam Web API");
            return;
        }

        // If No New Apps, Return
        if(appList.isEmpty()) {
            logger.log(Logger.LOG_TYPE_UPDATE, "AppList Update Finished: No New Apps Added");
            return;
        }

        // Create Query String
        StringBuilder sb = new StringBuilder(insertNewApp);

        // Add Value Sections to SQL Query String
        for(int i = 0; i < appList.size(); i++) {
            sb.append("(?, ?)"); // Add Variable Section
            sb.append((i < appList.size()-1) ? ", " : ";"); // Line Terminators
            sb.append("\n"); //New Line
        }

        // Perform Query
        try(PreparedStatement pStmt = conn.prepareStatement(sb.toString())) {
            int count = 1;

            // Add New Apps to Statement
            for(App app : appList) {
                pStmt.setInt(count++, app.appid);
                pStmt.setString(count++, app.name);
            }

            int addedApps = pStmt.executeUpdate();
            logger.log(Logger.LOG_TYPE_UPDATE, String.format("AppList Update Finished: %d New Apps Added", addedApps));

        }
        catch (SQLException e) {
            e.printStackTrace();
            logger.log(Logger.LOG_TYPE_ERROR, String.format("Failed to Add %d New Apps to the Database", appList.size()));
        }
    }

    /**
     * Method to update all out-of-date apps in the database
     */
    public void updateAppDetails() {
        // Get List Of Apps to update from Database
        ArrayList<Integer> appList = getAppsToUpdate();
        if(appList == null) {
            return;
        }

        logger.log(Logger.LOG_TYPE_UPDATE, String.format("Beginning App Details Update: %d To Update", appList.size()));

        // Update Apps from List
        updateAppsFromAppids(appList);
    }

    /**
     * Method to open a connection to the Database
     * @return true on success or false on failure
     */
    public boolean openConnection() {
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD); // Connect to Database
            conn.setCatalog(DB_NAME);

            setConnected(); // Set Connection Active
            return true;
        }
        catch (SQLException | ClassNotFoundException e) {
            logger.log(Logger.LOG_TYPE_CONNECTION, "Failed To Open Connection To Database: " + DB_URL);
        }
        closeConnection(); // Close connection or statement
        return false;
    }

    /**
     * Method to close a connection to the Database
     */
    public void closeConnection() {
        // Attempt to close Connection
        try {
            if(conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed To Close Connection To Database");
        }
        setDisconnected();
    }

    /**
     * Method to close the logFile at the end of update
     */
    public void closeLogFile() {
        logger.closeLogFile();
    }

    // *** Private Utilities ***
    /**
     * Method to set class as connected
     */
    private void setConnected() {
        connected = true;
        logger.log(Logger.LOG_TYPE_CONNECTION, "Connected To Database");
    }

    /**
     * Method to set class as disconnected
     */
    private void setDisconnected() {
        connected = false;
        logger.log(Logger.LOG_TYPE_CONNECTION, "Disconnected From Database");
    }

    // *** Steam API Methods ***

    /**
     * Method to get the Steam App List as a JSONArray that do not appear in the database
     * @param dbAppList is the set of current appids in the database
     * @return Steam App List on Success or Null on Failure
     */
    private ArrayList<App> getCurrentAppList(HashSet<Integer> dbAppList) {
        // Setup URL
        String apiURL = "https://api.steampowered.com/ISteamApps/GetAppList/v2/?key=%s";
        String url = String.format(apiURL, props.getProperty("steamAPI.key"));

        // Get JSON String
        String jsonStringResult = getJSONStringFromUrl(url);
        // If the JSON Fetch Failed, Return
        if(jsonStringResult == null) {
            return null;
        }

        // Return JSONArray from Result
        try {
            // Get App List from JSON String
            JSONArray jsonArray = new JSONObject(jsonStringResult).getJSONObject("applist").getJSONArray("apps");
            ArrayList<App> appList = new ArrayList<>();

            // Convert JSONArray to ArrayList<App>
            for(Object obj : jsonArray) {
                JSONObject app = (JSONObject) obj;
                String name = app.getString("name");
                int appid = app.getInt("appid");

                // Add To List if Not in database
                if(!dbAppList.contains(appid)) {
                    appList.add(new App(appid, name));
                }
            }
            return appList;
        }
        // Return Null on Extraction Failure
        catch (JSONException e) {
            return null;
        }
    }

    private void updateAppsFromAppids(ArrayList<Integer> appids) {
        final int MAX_API_CALLS = 195; // Max Calls within 5min to prevent RateLimiter
        final int MAX_FAILED_CALLS = 5;

        int apiCallCount = 0;
        int failCallCount = 0;

        String steam_api_url = "https://store.steampowered.com/api/appdetails?appids=";


        ArrayList<AppDetails> appDetails = new ArrayList<>();

        for(int appid : appids) {
            String url = steam_api_url + appid; // Setup Call URL

            // Prevent RateLimit By Waiting 5Min
            if(apiCallCount >= MAX_API_CALLS) {
                try {
                    logger.log(Logger.LOG_TYPE_WARNING, "API_CALL_LIMIT - Waiting 5 Minutes to Prevent HTTP ERROR 429");
                    Thread.sleep(5 * 60 * 1000); // Wait 5 Minutes Before Continuing
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                apiCallCount = 0;
            }
            apiCallCount++;

            // Make API Call
            String jsonStringResult = getJSONStringFromUrl(url);

            // If Call Fails, Skip To Next appid
            if(jsonStringResult == null) {
                logger.log(Logger.LOG_TYPE_ERROR, "Failed To Retrieve Response For: " + appid);
                if(failCallCount == MAX_FAILED_CALLS) {
                    logger.log(Logger.LOG_TYPE_WARNING, String.format("Failed To Fetch Response For %d Apps - Stopping Update", MAX_FAILED_CALLS));
                    return;
                }
                failCallCount++;
                continue;
            }

            // Deconstruct JSON Result and Convert To Usable Data
            try {
                JSONObject json = new JSONObject(jsonStringResult);
                JSONObject parent = json.getJSONObject("" + appid);

                AppDetails temp;
                // Set as Invalid
                if(!parent.getBoolean("success")) {
                    temp = new AppDetails(appid);
                }
                // Handle Real Result
                else {
                    JSONObject details = parent.getJSONObject("data");
                    temp = new AppDetails(appid, details.getString("type"));
                }
                updateAppDetails(temp); // Update Database
            }
            // Return Null on Extraction Failure
            catch (JSONException e) {
                e.printStackTrace();
                logger.log(Logger.LOG_TYPE_ERROR, "Failed To Retrieve Result for Appid: " + appid);
            }
        }
    }


    // *** Database Methods ***

    /**
     * Method to retrieve the list of all apps to update from the Database
     * > Update Requirments: Last Update > daysOld OR App NEVER Updated (Null Last Update)
     * @return ArrayList of all appids to update
     */
    private ArrayList<Integer> getAppsToUpdate() {
        if(!connected) {
            logger.log(Logger.LOG_TYPE_WARNING, "Cannot Fetch Apps to update - Not Connected To Database");
            return null;
        }

        // Prepare SQL Statement To Fetch Apps To Update
        ArrayList<Integer> updateApps = new ArrayList<>();
        try(PreparedStatement pStmt = conn.prepareStatement(getOutOfDateApps)) {
            pStmt.setInt(1, MAX_DAYS_OLD); // Set Days Old

            ResultSet rs = pStmt.executeQuery(); // Retrieve results

            // Compile Results into ArrayList
            while(rs.next()) {
                updateApps.add(rs.getInt(COLUMN_APPID));
            }
            return updateApps;
        }
        catch (SQLException e) {
            logger.log(Logger.LOG_TYPE_ERROR, "Failed to Retrieve Apps to Update from Database");
            return null;
        }
    }

    /**
     * Method to get the full list of existing appids in the Database AppInfo Table
     * @return HashSet of all appids or null if connection is invalid
     */
    private HashSet<Integer> getAllAppIDsFromDB() {
        if(!connected) {
            logger.log(Logger.LOG_TYPE_WARNING, "Cannot Fetch AppList - Not Connected To Database");
            return null;
        }

        // Add All appids to HashSet for Quick Searching
        HashSet<Integer> appidSet = new HashSet<>();
        try(Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(getAllAppids);

            // Iterate through result set
            while(rs.next()) {
                appidSet.add(rs.getInt(COLUMN_APPID));
            }
        }
        catch (SQLException e) {
            return null;
        }
        return appidSet;
    }

    /**
     * Method to update an entry in the AppInfo Table with the given AppDetails Object
     * @param appDetails is the given AppDetails Object
     */
    private void updateAppDetails(AppDetails appDetails) {
        // Setup Prepared Statement
        try(PreparedStatement pStmt = conn.prepareStatement(updateApp)) {
            // Set SQL Items and Execute
            pStmt.setString(1, appDetails.getType());
            pStmt.setInt(2, appDetails.getAppid());
            pStmt.executeUpdate();

            // Log Valid or Invalid
            if(appDetails.isInvalid()) {
                logger.log(Logger.LOG_TYPE_WARNING, "App Updated to Invalid Status: " + appDetails.getAppid());
            }
            else {
                logger.log(Logger.LOG_TYPE_UPDATE, "Updated App: " + appDetails.getAppid());
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            logger.log(Logger.LOG_TYPE_ERROR, "Failed To Update App: " + appDetails.getAppid());
        }
    }
}
