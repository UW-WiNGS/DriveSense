package wisc.drivesense.utility;

import java.util.UUID;

/**
 * Created by peter on 2/14/17.
 * Trip metadata that is considered "shared" between the DriveSense server and client.
 * The Trip class contains more client specific information
 */

public class TripMetadata {
    public String guid;
    public Integer status; // 0 = deleted, 1 = live, 2 = finalized
    public Double distance; // in meter

    public static final int LIVE = 1;
    public static final int DELETED = 0;
    public static final int FINALIZED = 2;

}
