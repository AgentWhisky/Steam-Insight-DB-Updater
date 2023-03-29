package db;

public class AppDetails {
    private static final String INVALID = "invalid";

    private final int appid;
    private final String type;

    public AppDetails(int appid) {
        this.appid = appid;
        this.type = INVALID;
    }

    public AppDetails(int appid, String type) {
        this.appid = appid;
        this.type = type;
    }

    public boolean isInvalid() {
        return type.equals(INVALID);
    }

    public int getAppid() {
        return appid;
    }

    public String getType() {
        return type;
    }
}
