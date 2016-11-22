package wisc.drivesense.httpPayloads;

import java.util.List;

import wisc.drivesense.utility.TraceMessage;

/**
 * Created by Alex Sherman on 11/15/2016.
 */

public class TripPayload {
    public String guid;
    public List<TraceMessage> traces;
    public Double distance;
    public Integer tripStatus;
}
