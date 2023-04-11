package db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;

import static db.ConnectorInterface.*;

/**
 * Class - Contains Static Methods For Fetching Data From Steam Web API
 */
public class SteamConnector {
    private static final int MAX_RETRIES = 5; // Max API Call Retries
    private static final String INVALID = "invalid";

    // *** Steam Web API Methods ***

    /**
     * Method to get current app list from Steam Web API excluding given list of appids
     * @param appids is the list of existing appids in the database
     * @param logger is the existing logger
     * @return ArrayList of Apps to add to database
     */
    public static ArrayList<App> getCurrentAppList(HashSet<Integer> appids, Logger logger) {
        // Setup URL
        final String url = "https://api.steampowered.com/ISteamApps/GetAppList/v2/";

        // Get JSON String from Steam Web API
        String jsonResult = getJSONStringFromURL(url, logger);
        if(jsonResult == null) {
            return null;
        }

        // Return JSONArray from Result
        try {
            // Get App List from JSON String
            JSONArray jsonArray = new JSONObject(jsonResult).getJSONObject("applist").getJSONArray("apps");
            ArrayList<App> appList = new ArrayList<>();

            // Convert JSONArray to ArrayList<App>
            for(Object obj : jsonArray) {
                JSONObject app = (JSONObject) obj;
                String name = app.getString("name");
                int appid = app.getInt("appid");

                // Add To List if Not in database
                if(!appids.contains(appid)) {
                    appList.add(new App(appid, name));
                }
            }
            return appList;
        }
        // Return Null on Extraction Failure
        catch (JSONException e) {
            System.err.println("Failed to Retrieve JSON Object from Steam Web API");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to get App Info for given appid from Steam Web API
     * @param appid is the given appid
     * @param logger is the current logger
     * @return AppEntry Object with app info
     */
    public static AppEntry getAppInfoFromAppid(int appid, Logger logger) {
        final String steam_api_url = "https://store.steampowered.com/api/appdetails?appids=%d";

        String url = String.format(steam_api_url, appid);

        String jsonResult = getJSONStringFromURL(url, logger);
        if(jsonResult == null) {
            return null;
        }

        // Deconstruct JSON Result and Convert To Usable Data
        try {
            JSONObject json = new JSONObject(jsonResult);
            JSONObject parent = json.getJSONObject("" + appid);

            String type;

            // Set as Invalid
            if(!parent.getBoolean("success")) {
                type = INVALID;
            }
            // Handle Real Result
            else {
                JSONObject details = parent.getJSONObject("data");
                type = details.getString("type");
            }
            return new AppEntry(appid, type);
        }
        // Return Invalid on Extraction Failure
        catch (JSONException e) {
            return new AppEntry(appid, INVALID);
        }
    }

    // *** Private Utilities ***

    /**
     * Method to get a JSONString from a given API URL
     * @param url is the given url
     * @param logger is the current logger
     * @return JSONStrong response for API or null on failure
     */
    private static String getJSONStringFromURL(String url, Logger logger) {
        int retryCount = 0; // Used for Call Retries

        while(retryCount < MAX_RETRIES) {
            try {
                // Setup Connection
                HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
                httpConn.setRequestMethod("GET");
                httpConn.setRequestProperty("Accept", "application/json");

                // Get Result as String
                byte[] byteResult = httpConn.getInputStream().readAllBytes();
                return new String(byteResult, StandardCharsets.UTF_8);
            }
            // Catch Any Error and Retry Up To MAX_RETRIES Times
            catch (IOException e) {
                if(logger != null) {
                    logger.log(Logger.LOG_TYPE_CONNECTION, String.format("Failed to Fetch JSONString: Retrying In 60 Seconds (%d/%d)\n", retryCount+1, MAX_RETRIES));
                }

                try {
                    Thread.sleep(60 * 1000); // Wait 60 Seconds
                }
                catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                retryCount++;
            }
        }
        return null;
    }

}
