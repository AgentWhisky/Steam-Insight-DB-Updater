package db;

public class Queries {
    // Column Names
    public static final String COLUMN_APPID = "appid";

    // MySQL Query to create a new Table AppInfo with set columns {Statement}
    public static final String createTableAppInfo = """
                CREATE TABLE AppInfo (
                appid INT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                last_update DATE,
                type VARCHAR(16)
                )
                """;

    // MySQL Query to Get All appids from the Table
    public static final String getAllAppids = """
            SELECT appid
            FROM AppInfo
            """;

    // MySQL Query to Get All appids from the Table for updating
    public static final String getAppidsToUpdate = """
                SELECT appid
                FROM AppInfo
                WHERE last_update IS NULL AND (type <> 'invalid' OR type IS NULL);
                """;

    // MySQL Query to insert a new app into the AppInfo Table {Prepared Statement}
    public static final String insertNewApp = "INSERT INTO AppInfo (appid, name) VALUES\n";

    // MySQL Query to update an app's type {Prepared Statement}
    public static final String updateApp = """
            UPDATE AppInfo
            SET
                last_update=CURRENT_TIMESTAMP(), type=?
            WHERE appid=?
            """;
}
