package db;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Utils {
    /**
     * Method to get the time string in given number of minutes
     * @param minutes is the given number of minutes
     * @return is the String representation of the time in given minutes
     */
    public static String getTimeString(int minutes) {
        // Update Time
        LocalTime futureTime = LocalTime.now().plus((minutes), ChronoUnit.MINUTES);
        return futureTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
