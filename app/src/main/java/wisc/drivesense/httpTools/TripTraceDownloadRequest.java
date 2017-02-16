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
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.TripMetadata;

/**
 * Created by peter on 2/8/17.
 */

public class TripTraceDownloadRequest extends GsonRequest<List<Trace.Trip>> {
    private TripMetadata trip;
    private static final Type responseType = new TypeToken<List<Trace.Trip>>(){}.getType();

    public TripTraceDownloadRequest(String url, TripMetadata trip, DriveSenseToken dsToken) {
        super(Method.POST, url, new TraceRequest(trip.guid, Trace.Trip.class), responseType, dsToken);
        this.trip = trip;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "Downloading traces for trip "+trip.guid+" failed. "+error.toString());
    }

    @Override
    public void onResponse(final List<Trace.Trip> response) {
        //Log.d(TAG, response.toString());
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                List<TraceMessage> tmList = new ArrayList<>(response.size());
                for (int i = 0; i < response.size(); i++) {
                    tmList.add(new TraceMessage(response.get(i)));
                }
                try {
                    Log.d(TAG, "Inserting trip and traces for "+trip.guid);
                    DriveSenseApp.DBHelper().insertTripAndTraces(trip, tmList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);

    }

    private static class TraceRequest {
        String guid;
        int start;
        String type;
        public TraceRequest(String guid, Class<? extends Trace> type) {
            this.guid=guid;
            this.start=0;
            this.type=GsonSingleton.typeNameLookup.get(type);
        }
    }
}
