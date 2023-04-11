package db;

import java.sql.DatabaseMetaData;

/**
 * Interface - Contains Important Records and Basic Methods for use with external code
 */
public interface ConnectorInterface {
    // Record for storing Database Connection Info
    record DatabaseInfo(String address, String port, String username, String password, String databaseName) {}

    // Record for storing results of an update to be used in teh GUI
    record UpdateResults(int newApps, int updatedApps) {}

    // Record for Storing init App Data
    record App(int appid, String name) {} // Used for adding initial entry

    // Record for Storing App Update Data
    record AppEntry(int appid, String type) { // Used for updating type/time of entry
        public boolean isValid() {
            return !type.equals("invalid");
        }
    }

    boolean openConnection(DatabaseInfo info); // Method to Open Database Connection with given info
    void closeConnection(); // Method to Close Database Connection
    UpdateResults update(); // Method to Update The Database
    DatabaseMetaData getMetaData(); // Method to get the Database MetaData from the connected database
}
