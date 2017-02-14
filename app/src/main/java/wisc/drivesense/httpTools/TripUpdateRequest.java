package wisc.drivesense.httpTools;

import android.util.Log;

import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;

/**
 * Created by peter on 2/10/17.
 */

public class TripUpdateRequest extends GsonRequest<List<TripPayload>> {
    private static final Type responseType = new TypeToken<List<TripPayload>>(){}.getType();

    public TripUpdateRequest(DriveSenseToken dsToken) {
        super(Method.GET, Constants.kAllTripsURL, null, responseType, dsToken);
    }

    @Override
    public void onResponse(List<TripPayload> response) {
        Log.d(TAG, response.toString());
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }
}
