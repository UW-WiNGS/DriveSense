package wisc.drivesense.utility;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by peter on 1/13/17.
 */

public class Units {
    // imperial units: feet, mi, mph
    // metric units: meter, km, km/h


    public static class userFacingDouble {
        public String unitName;
        public double value;
    }
    public static class userFacingInt {
        String unitName;
        int value;
    }

    /**
     * @param meters
     * @param metricOutput
     * @return
     */
    public static userFacingDouble smallDistance(double meters, boolean metricOutput) {
        userFacingDouble val = new userFacingDouble();
        if(metricOutput){
            val.value = meters;
            val.unitName = "meters";
        } else {
            val.value = meters * Constants.kFeetPerMeter;
            val.unitName = "feet";
        }
        return val;
    }
    public static userFacingDouble largeDistance(double meters, boolean metricOutput) {
        userFacingDouble val = new userFacingDouble();
        if(metricOutput){
            val.value = meters / 1000;
            val.unitName = "km";
        } else {
            val.value = meters / Constants.kMetersPerMile;
            val.unitName = "mi";
        }
        return val;
    }
    public static userFacingDouble speed(double metersPerSecond, boolean metricOutput) {
        userFacingDouble val = new userFacingDouble();
        if(metricOutput){
            val.value = metersPerSecond * Constants.kMeterPSToKMPH;
            val.unitName = "km/h";
        } else {
            val.value = metersPerSecond * Constants.kMeterPSToMilePH;
            val.unitName = "mph";
        }
        return val;
    }


    /**
     * Mainly for trip duration
     * @param milliseconds
     * @return
     */
    public static String displayTimeInterval(long milliseconds) {
        final long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        final long min = TimeUnit.MILLISECONDS.toMinutes(milliseconds - TimeUnit.HOURS.toMillis(hours));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(milliseconds - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(min));
        if(hours != 0) {
            return String.format(Locale.US, "%dh %dm", hours, min);
        }
        return String.format(Locale.US, "%dm %02ds", min, sec);
    }
}
