package db;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Utilities {
    private static final int MAX_RETRIES = 5; // Max Call Retries

    /**
     * Method to return a JSONString response from a given url
     * @param url is the given url
     * @return String Result from URL or Null on ANY Failure
     */
    public static String getJSONStringFromUrl(String url) {
        int retryCount = 0; // Used for Call Retries

        while(retryCount < MAX_RETRIES) {
            try {
                // Setup Connection
                HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
                httpConn.setRequestMethod("GET");
                httpConn.setRequestProperty("Accept", "application/json");

                // Get Result as String
                byte[] byteResult = httpConn.getInputStream().readAllBytes();
                String jsonString = new String(byteResult, StandardCharsets.UTF_8);

                httpConn.disconnect();
                return jsonString;
            }
            // Catch Any Error and Retry Up To MAX_RETRIES Times
            catch (IOException e) {
                System.err.printf("Failed to Fetch JSONString: Retrying In 1MIN (%d/%d)\n", retryCount+1, MAX_RETRIES);
                try {
                    Thread.sleep(60 * 1000);
                }
                catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                retryCount++;
            }
        }
        return null;
    }

    /**
     * Method to import a config.properties file in the resources directory
     * @return Properties Object imported from file
     */
    public static Properties importPropFile() {
        String propFile = "config.properties";
        // Import Prop File
        try(InputStream inputStream = Utilities.class.getClassLoader().getResourceAsStream(propFile)) {
            Properties prop = new Properties();
            prop.load(inputStream);
            return prop;
        }
        // Failed To Load File
        catch (IOException e) {
            System.err.println("Failed To Import Properties File");
        }
        return null;
    }
}
