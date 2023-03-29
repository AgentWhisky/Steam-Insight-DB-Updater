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

    // MySQL Query to insert a new app into the AppInfo Table {Prepared Statement}
    public static final String insertNewApp = "INSERT INTO AppInfo (appid, name) VALUES\n";

    // MySQL Query to get all appids from Table AppInfo {Statement}
    public static final String getAllAppids = """
                SELECT appid
                FROM AppInfo
                """;

    // MySQL Query to drop the AppInfo Table (USED VERY RARELY) {Statement}
    public static final String dropTableAppInfo = "DROP TABLE AppInfo";

    // MySQL Query to get all apps that need to be updated {Prepared Statement}
    // >last_update is null or older than given number of days
    public static final String getOutOfDateApps = """
                SELECT appid
                FROM appinfo
                WHERE (last_update IS NULL or last_update < DATE_SUB(NOW(), INTERVAL ? DAY)) AND type <> 'invalid' OR type IS NULL;
                """;

    // MySQL Query to update an app's type {Prepared Statement}
    public static final String updateApp = """
            UPDATE AppInfo
            SET
                last_update=CURRENT_TIMESTAMP(), type=?
            WHERE appid=?
            """;



}
