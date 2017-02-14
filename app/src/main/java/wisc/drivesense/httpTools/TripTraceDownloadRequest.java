package wisc.drivesense.httpTools;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;

/**
 * Created by peter on 2/8/17.
 */

public class TripTraceDownloadRequest extends GsonRequest<List<Trace.Trip>> {
    private Context context;
    private static volatile boolean running = false;
    private String uuid;
    private static final Type responseType = new TypeToken<List<Trace.Trip>>(){}.getType();

    private TripTraceDownloadRequest(String url, TraceRequest body, DriveSenseToken dsToken, Context context) {
        super(Method.POST, url, body, responseType, dsToken);
        this.context = context.getApplicationContext();
        uuid = body.guid;
    }

    public static synchronized void Start(String uuid, Context context) {
        if(!running) {
            DriveSenseToken user = DriveSenseApp.DBHelper().getCurrentUser();
            TraceRequest body = new TraceRequest(uuid, Trace.Trip.class);

            TripTraceDownloadRequest currentRequest = new TripTraceDownloadRequest(Constants.kTripTracesURL, body, user, context);
            DriveSenseApp.RequestQueue().add(currentRequest);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "Downloading traces for trip "+uuid+" failed. "+error.toString());
    }

    @Override
    public void onResponse(List<Trace.Trip> response) {
        Log.d(TAG, response.toString());
        List<TraceMessage> tmList = new ArrayList<>(response.size());
        for (int i = 0; i < response.size(); i++) {
            tmList.add(new TraceMessage(response.get(i)));
        }
        try {
            DriveSenseApp.DBHelper().insertSensorData(uuid, tmList);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
