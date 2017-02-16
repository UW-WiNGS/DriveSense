package wisc.drivesense.httpTools;

import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.TripMetadata;

/**
 * Created by peter on 2/10/17.
 *
 */

public class TripUpdateRequest extends GsonRequest<List<TripMetadata>> {
    private static final Type responseType = new TypeToken<List<TripMetadata>>(){}.getType();
    private static final String TAG = "TripUpdateRequest";

    public TripUpdateRequest(DriveSenseToken dsToken) {
        super(Method.GET, Constants.kAllTripsURL, null, responseType, dsToken);
    }

    @Override
    public void onResponse(List<TripMetadata> response) {
        new AsyncTask<TripMetadata, Void, Void>() {
            @Override
            protected Void doInBackground(TripMetadata... tripMetadatas) {
                List<TripMetadata> toUpdate = new ArrayList<>();
                for (TripMetadata tripResp : tripMetadatas) {
                    //only store trips locally if they are not live
                    if(tripResp.status != TripMetadata.LIVE) {
                        Trip currentTrip = DriveSenseApp.DBHelper().getTrip(tripResp.guid);
                        if (currentTrip == null && tripResp.status == TripMetadata.FINALIZED) {
                            //download the traces for the trip
                            Log.d(TAG, "Download traces for trip: " + tripResp.guid);
                            TripTraceDownloadRequest currentRequest = new TripTraceDownloadRequest(Constants.kTripTracesURL, tripResp, dsToken);
                            DriveSenseApp.RequestQueue().add(currentRequest);
                        } else if (currentTrip != null && currentTrip.getSynced()) {
                            //only update a trip's data if it's synced
                            toUpdate.add(tripResp);
                        } //otherwise do nothing if the trip is not synced
                    }
                }
                Log.d(TAG, "Updating "+toUpdate.size()+ " trips in the database.");
                DriveSenseApp.DBHelper().updateTrips(toUpdate);
                Log.d(TAG, "Done processing list of trips from server");
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,response.toArray(new TripMetadata[response.size()]));
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }
}
