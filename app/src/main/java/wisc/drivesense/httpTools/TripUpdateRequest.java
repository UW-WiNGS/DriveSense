package wisc.drivesense.httpTools;

import android.util.Log;

import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.TripMetadata;

/**
 * Created by peter on 2/10/17.
 */

public class TripUpdateRequest extends GsonRequest<List<TripMetadata>> {
    private static final Type responseType = new TypeToken<List<TripMetadata>>(){}.getType();

    public TripUpdateRequest(DriveSenseToken dsToken) {
        super(Method.GET, Constants.kAllTripsURL, null, responseType, dsToken);
    }

    @Override
    public void onResponse(List<TripMetadata> response) {
        for (TripMetadata tripResp : response) {
            //only store trips locally if they are not live
            if(tripResp.status != TripMetadata.LIVE) {
                Trip currentTrip = DriveSenseApp.DBHelper().getTrip(tripResp.guid);
                if (currentTrip == null && tripResp.status == TripMetadata.FINALIZED) {
                    //download the traces for the trip
                    //Log.d(TAG, "Download traces for trip: " + tripResp.guid);
                    //TripTraceDownloadRequest.Start(trip.guid);
                } else if (currentTrip != null && currentTrip.getSynced() == true) {
                    DriveSenseApp.DBHelper().updateTrip(tripResp);
                    Log.d(TAG, "Updated trip " + tripResp.guid + " in the database.");
                } //otherwise do nothing if the trip is not synced
            }
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }
}
