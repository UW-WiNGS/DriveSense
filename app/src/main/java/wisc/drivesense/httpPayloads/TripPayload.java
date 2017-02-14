package wisc.drivesense.httpPayloads;

import java.util.List;

import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.TripMetadata;

/**
 * Created by Alex Sherman on 11/15/2016.
 */

public class TripPayload extends TripMetadata {
    public List<TraceMessage> traces;
}
