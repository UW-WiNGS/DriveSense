package wisc.drivesense.utility;

/**
 * Created by peter on 1/13/17.
 */

public class Units {
    public static class userFacingDouble {
        public String unitName;
        public double value;
    }
    public static class userFacingInt {
        String unitName;
        int value;
    }
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
            val.unitName = "mi/h";
        }
        return val;
    }
}
