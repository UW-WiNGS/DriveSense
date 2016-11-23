package wisc.drivesense.uploader;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.List;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.httpPayloads.GsonRequest;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;

/**
 * Created by Alex Sherman on 11/22/2016.
 */

public class TripUploadRequest extends GsonRequest<TripPayload> {
    private static volatile boolean running = false;
    private static volatile int failureCount = 0;
    private static final int FAILURE_THRESHOLD = 10;

    /**
     * Start an upload of a trip payload that may or may not contain
     * the entire trip's points. Must indicate that an upload is in progress
     * so that race conditions don't occur
     * @param context
     * @param payload
     */
    public static synchronized void Start(Context context, TripPayload payload) {
        if(!running) {
            DriveSenseToken user = DriveSenseApp.DBHelper().getCurrentUser();
            if(user == null) return;
            running = true;
            TripUploadRequest currentRequest = new TripUploadRequest(context, Request.Method.POST, Constants.kTripURL, payload, user);
            DriveSenseApp.RequestQueue().add(currentRequest);
        }
    }

    /**
     * Start an upload of any past trips that aren't synced.
     * Must not start if an existing live upload is in progress.
     * @param context
     */
    public static synchronized void Start(Context context) {
        if(!running) {
            List<Trip> trips = DriveSenseApp.DBHelper().loadTrips("synced = 0 and status = 2");
            if(trips.size() == 0) return;
            TripPayload payload = new TripPayload();
            Trip trip = trips.get(0);
            payload.guid = trip.uuid.toString();
            payload.traces = DriveSenseApp.DBHelper().getUnsentTraces(payload.guid);
            payload.status = trip.getStatus();
            Start(context, payload);
        }
    }

    private static boolean needsUpload() {
        return DriveSenseApp.DBHelper().loadTrips("synced = 0 and status = 2").size() > 0;
    }

    private Context context;

    private TripUploadRequest(Context context, int method, String url, TripPayload body, DriveSenseToken dsToken) {
        super(method, url, body, TripPayload.class, dsToken);
        this.context = context;
    }

    private synchronized void onComplete() {
        running = false;
        if(failureCount < FAILURE_THRESHOLD && needsUpload())
            Start(context);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        onComplete();
        failureCount ++;
    }

    @Override
    public void onResponse(TripPayload response) {
        failureCount = 0;
        for (TraceMessage trace : ((TripPayload)payload).traces) {
            DriveSenseApp.DBHelper().markTraceSynced(trace.rowid);
        }

        // Null check is apparently necessary because java is dumb at autoboxing
        if(((TripPayload)payload).status != null && ((TripPayload)payload).status == 2 &&
                DriveSenseApp.DBHelper().getUnsentTraces(((TripPayload)payload).guid).isEmpty())
            DriveSenseApp.DBHelper().markTripSynced(((TripPayload)payload).guid);
        onComplete();
    }
}
