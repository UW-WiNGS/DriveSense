package wisc.drivesense.uploader;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.List;

import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.httpPayloads.GsonRequest;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trip;

/**
 * Created by Alex Sherman on 11/22/2016.
 */

public class TripUploadRequest extends GsonRequest<TripPayload> {
    private static volatile boolean needsUpload = false;
    private static volatile boolean running = false;

    public static synchronized void Start(Context context) {
        if(!running) {
            List<Trip> trips = DatabaseHelper.single().loadTrips("synced = 0 and status = 1");
            DriveSenseToken user = DatabaseHelper.single().getCurrentUser();
            if(user == null || trips.size() == 0) return;
            TripPayload payload = new TripPayload();
            Trip trip = trips.get(0);
            payload.guid = trip.uuid.toString();
            payload.traces = DatabaseHelper.single().getUnsentTraces(payload.guid);
            running = true;
            needsUpload = false;
            TripUploadRequest currentRequest = new TripUploadRequest(context, Request.Method.POST, Constants.kTripURL, payload, user);
            RequestQueueSingleton.getInstance(context).getRequestQueue().add(currentRequest);
        }
        else
            needsUpload = true;
    }

    private Context context;

    private TripUploadRequest(Context context, int method, String url, TripPayload body, DriveSenseToken dsToken) {
        super(method, url, body, TripPayload.class, dsToken);
        this.context = context;
    }

    private synchronized void onComplete() {
        running = false;
        if(needsUpload)
            Start(context);
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }

    @Override
    public void onResponse(TripPayload response) {

    }
}
