package db;

public class Updater {
    public static void main(String[] args) {
        SteamDBConnector sdbc = new SteamDBConnector();

        // Execute Code Block if Connection Succeeds
        if(sdbc.openConnection()) {
            // Make Updates
            sdbc.createAppInfoTable(); // Ensure AppInfo table Exists
            sdbc.updateAppList(); // Update App List
            sdbc.updateAppDetails(); // Update Apps

            // Closing Streams
            sdbc.closeConnection();
            sdbc.closeLogFile();
        }
    }
}
