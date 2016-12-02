package wisc.drivesense.triprecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.httpPayloads.GsonRequest;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.uploader.TripUploadRequest;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Rating;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;

public class TripService extends Service {
    ArrayList<TraceMessage> unsentMessages = new ArrayList<TraceMessage>();
    private long lastSent = 0;
    private final long SEND_INTERVAL = 1000;
    private DriveSenseToken user = null;
    public Trip curtrip_ = null;
    public Rating rating_ = null;
    public RealTimeTiltCalculation tiltCal_ = null;

    public Binder _binder = new TripServiceBinder();
    private AtomicBoolean _isRunning = new AtomicBoolean(false);

    private final String TAG = "Trip Service";


    private static Intent mSensor = null;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return _binder;
    }

    public class TripServiceBinder extends Binder {
        public TripService getService() {return TripService.this;}
        public Trip getTrip() {
            return curtrip_;
        }
        public boolean isRunning() {
            return _isRunning.get();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "stop driving detection service");
        _isRunning.set(false);
        stopService(mSensor);
        mSensor = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        //validate the trip based on distance and travel time
        if(curtrip_.getDistance() >= Constants.kTripMinimumDistance && curtrip_.getDuration() >= Constants.kTripMinimumDuration) {
            Toast.makeText(this, "Saving trip in background!", Toast.LENGTH_SHORT).show();
            curtrip_.setStatus(2);
            curtrip_.setEndTime(System.currentTimeMillis());
            DriveSenseApp.DBHelper().updateTrip(curtrip_);
            TripUploadRequest.Start();
        } else {
            Toast.makeText(this, "Trip too short, not saved!", Toast.LENGTH_SHORT).show();
            DriveSenseApp.DBHelper().deleteTrip(curtrip_.uuid.toString());
        }

        stopSelf();
    }



    private void startService() {
        _isRunning.set(true);
        Log.d(TAG, "start driving detection service");

        mSensor = new Intent(this, SensorService.class);
        startService(mSensor);

        Toast.makeText(this, "Start trip in background!", Toast.LENGTH_SHORT).show();
        user = DriveSenseApp.DBHelper().getCurrentUser();

        curtrip_ = new Trip();
        DriveSenseApp.DBHelper().insertTrip(curtrip_);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("sensor"));
    }


    /**
     * where we get the sensor data
     */
    private long lastGPS = 0;
    private long lastSpeedNonzero = 0;
    private boolean stoprecording = false;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TraceMessage message = GsonSingleton.fromJson(intent.getStringExtra("trace"), TraceMessage.class);
            Trace trace = message.value;
            if(trace == null) return;

            if(lastSpeedNonzero == 0 && lastGPS == 0) {
                lastSpeedNonzero = trace.time;
                lastGPS = trace.time;
            }

            if(trace instanceof Trace.GPS) {
                Trace.GPS gps = (Trace.GPS)trace;
                //Log.d(TAG, "Got message: " + trace.toJson());

                if(gps.speed != 0.0) {
                    lastSpeedNonzero = gps.time;
                }
                lastGPS = gps.time;

                curtrip_.addGPS(gps);
            }

            if(trace instanceof Trace.Trip) {
                curtrip_.setTilt(((Trace.Trip) trace).tilt);
                curtrip_.setScore(((Trace.Trip) trace).score);
            }

            if(!stoprecording) {
                try {
                    message.rowid = DriveSenseApp.DBHelper().insertSensorData(curtrip_.uuid.toString(), trace);
                    unsentMessages.add(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(!unsentMessages.isEmpty() && System.currentTimeMillis() - lastSent > SEND_INTERVAL && user != null) {
                Log.d(TAG, "Uploading "+unsentMessages.size()+" traces.");
                TripPayload payload = new TripPayload();
                payload.guid = curtrip_.uuid.toString();
                payload.traces = unsentMessages;
                payload.distance = curtrip_.getDistance();
                lastSent = System.currentTimeMillis();
                unsentMessages = new ArrayList<>();

                TripUploadRequest.Start(payload);
            }

            long curtime = trace.time;
            if(curtime - lastSpeedNonzero > Constants.kInactiveDuration || curtime - lastGPS > Constants.kInactiveDuration) {
                //stoprecording = true;
            } else {
                stoprecording = false;
            }
        }

    };
}
