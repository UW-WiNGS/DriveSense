package wisc.drivesense.httpTools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.List;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trip;

import static wisc.drivesense.utility.Constants.kBatchUploadCount;

/**
 * Created by Alex Sherman on 11/22/2016.
 */

public class TripUploadRequest extends CompressedGSONRequest<TripPayload> {
    private static volatile boolean running = false;
    private static volatile int failureCount = 0;
    private static final int FAILURE_THRESHOLD = 10;
    private static final String TAG = "TripUploadRequest";

    private Context context;

    /**
     * Start an upload of a trip payload that may or may not contain
     * the entire trip's points. Must indicate that an upload is in progress
     * so that race conditions don't occur
     * @param payload
     */
    public static synchronized void Start(TripPayload payload, Context context) {
        if(!running) {
            DriveSenseToken user = DriveSenseApp.DBHelper().getCurrentUser();
            if(user == null) return;
            running = true;
            TripUploadRequest currentRequest = new TripUploadRequest(Request.Method.POST, Constants.kTripURL, payload, user, context);
            DriveSenseApp.RequestQueue().add(currentRequest);
        }
    }

    /**
     * Start an upload of any past trips that aren't synced.
     * Must not start if an existing live upload is in progress.
     * @param context

     */
    public static synchronized void Start(Context context) {
        if(!running && context != null) {
            boolean wifi = wifiConnected(context) || !SettingActivity.getWifiOnly(context);

            //create a list of trips with either unsynced metadata or unsent traces.
            List<Trip> trips = DriveSenseApp.DBHelper().loadTrips("synced = 0 and status != 1");
            List<Trip> unsent = DriveSenseApp.DBHelper().getTripsWithUnsentTraces(!wifi);
            Log.d(TAG, "Found "+trips.size()+" trips with unsynced metadata and "+unsent.size()+" trips with unsent traces");
            trips.addAll(unsent);
            if(trips.size() == 0) return;
            TripPayload payload = new TripPayload();
            Trip trip = trips.get(0);
            payload.guid = trip.uuid.toString();
            payload.distance = trip.getDistance();
            payload.traces = DriveSenseApp.DBHelper().getUnsentTraces(payload.guid, kBatchUploadCount, !wifi);
            payload.status = trip.getStatus();

            Start(payload, context);
        }
    }

    /**
     * Return true if the device is currently connected to WiFi
     * @param context Current application context
     * @return true if connected to WiFi
     */
    private static boolean wifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connManager.getAllNetworks();
        boolean wifi = false;
        for (Network n: networks) {
            NetworkInfo ni = connManager.getNetworkInfo(n);
            if (ni.getType() == ConnectivityManager.TYPE_WIFI && ni.isConnected())
                wifi = true;
        }
        return wifi;
    }


    private TripUploadRequest(int method, String url, TripPayload body, DriveSenseToken dsToken, Context context) {
        super(method, url, body, TripPayload.class, dsToken);
        this.context = context.getApplicationContext();
    }

    private synchronized void onComplete() {
        running = false;
        if(failureCount < FAILURE_THRESHOLD)
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
        Long[] traceids = new Long[((TripPayload)payload).traces.size()];

        for (int i = 0; i < traceids.length; i++) {
            traceids[i] = ((TripPayload)payload).traces.get(i).rowid;
        }
        DriveSenseApp.DBHelper().markTracesSynced(traceids);

        // Mark trip synced. Note that this does not mean all traces have been synced, just metadata
        DriveSenseApp.DBHelper().markTripSynced(((TripPayload)payload).guid);
        onComplete();
    }
}
