package wisc.drivesense.utility;

/**
 * Created by lkang on 3/29/16.
 */
public class Constants {
    public static final double kEarthGravity = 9.80665; /*m^2/s*/

    /*for GPS*/
    public static final double kSmallEPSILON = 1e-8;
    public static final double kEarthRadius = 6371 * 1000; /*m*/

    public static final double kMeterToMile = 0.000621371;
    public static final double kMeterPSToMilePH = 2.23694;
    public static final double kKmPHToMPH = 0.621371;
    public static final double kKmPHToMeterPS = 0.277778;

    public static final double kMileToMeters = 1609.34;

    public static final String kInputSeperator = "\t";
    public static final String kOutputSeperator = "\t";
    public static final String slash = "/";

    public static final double kSampleRate = 1.0;
    public static final double kRecordingInterval = 100;


    public static final String kUploadTripDBFile = "dbfile";
    public static final String kSychronizeTripDeletion = "delete";


    private static final String kDomain = "http://drivesensetest.wirover.com:8000";
    public static final String kUploadURL = kDomain + "/upload";
    public static final String kSyncDeleteURL = kDomain + "/androidsync";


    public static final String kSignInURL = kDomain + "/auth/signin";
    public static final String kSignUpURL = kDomain + "/androidsignup";

    public static final String kGoogleSignInURL = kDomain + "/auth/google";
    public static final String kFacebookSignInURL = kDomain + "/auth/facebook";

    public static final String kDBFolder = "/data/data/wisc.drivesense/databases/";

    public static final int kNumberOfTripsDisplay = 20;
    public static final double kTripMinimumDuration = 1.0; //mins
    public static final double kTripMinimumDistance = 1000; //meter


    public static final double kExponentialMovingAverageAlpha = 0.4;//0.3;

    public static final long kInactiveDuration = 1 * 60 * 1000; //millisecond
    public static final long kFileUploadMaxSize = 10 * 1000000; //byte


}
