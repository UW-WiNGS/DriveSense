package wisc.drivesense;

import com.google.gson.Gson;

import org.junit.Test;

import wisc.drivesense.utility.Serialize;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TraceTest {

    @Test
    public void serializeTrip() {
        Trace.Trip p = new Trace.Trip();
        p.tilt = 3;
        p.score = -10;
        TraceMessage m = new TraceMessage(p);
        String output = Serialize.toJson(m);
        assertTrue(m.equals(Serialize.fromJson(output, TraceMessage.class)));
    }
    @Test
    public void serializeGPS() {
        Trace.GPS p = new Trace.GPS();
        p.lat = 40;
        p.lon = 36;
        TraceMessage m = new TraceMessage(p);
        String output = Serialize.toJson(m);
        assertTrue(m.equals(Serialize.fromJson(output, TraceMessage.class)));
    }
}