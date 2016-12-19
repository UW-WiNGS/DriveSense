package wisc.drivesense.triprecorder;

import android.app.Notification;
import android.app.PendingIntent;
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

import java.util.ArrayList;
import java.util.List;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.R;
import wisc.drivesense.activity.MainActivity;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.uploader.TripUploadRequest;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;

public class TripService extends Service {
    ArrayList<TraceMessage> unsentMessages = new ArrayList<TraceMessage>();
    private long lastSent = 0;
    private final long SEND_INTERVAL = 1000;
    private DriveSenseToken user = null;
    private Trip curtrip_ = null;

    public Binder _binder = new TripServiceBinder();

    private final String TAG = "Trip Service";

    private final int ONGOING_NOTIFICATION_ID = 1;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return _binder;
    }

    public class TripServiceBinder extends Binder {
        public TripService getService() {return TripService.this;}
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start command called on TripService");
        if(intent == null) {
            //Null intent means the service was killed and is being restarted by android
            curtrip_ = DriveSenseApp.DBHelper().getLastTrip();
            if(curtrip_!=null) {
                List<Trace.Trip> points_ = DriveSenseApp.DBHelper().getGPSPoints(curtrip_.uuid.toString());
                curtrip_.setGPSPoints(points_);
                Log.d(TAG, "Trip distance: "+curtrip_.getDistance() + " gps length: "+curtrip_.getGPSPoints().size());
                Log.d(TAG, "Restart driving detection service after being killed by android. UUID: "+curtrip_.uuid);
                startSensors();
                startForeground();
            } else {
                Log.d(TAG, "TripService was restarted, but no unfinalized trip was found");
            }
        }

        return START_STICKY;
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("DriveSense Trip Recording")
                .setContentText("Currently recording a trip.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    public void startRecordingNewTrip() {
        curtrip_ = new Trip();
        DriveSenseApp.DBHelper().insertTrip(curtrip_);
        Log.d(TAG, "Start driving detection service. UUID: "+curtrip_.uuid);
        startSensors();

        Intent tsi = new Intent(this, TripService.class);
        startService(tsi);
        startForeground();
    }

    public void stopRecordingTrip() {
        stopForeground(true);
        stopSensors();
        if(curtrip_ != null) {
            //validate the trip based on distance and travel time
            if(curtrip_.getDistance() >= SettingActivity.getMinimumDistance(this)) {
                Toast.makeText(this, "Saving trip in background!", Toast.LENGTH_LONG).show();
                curtrip_.setStatus(2);

            } else {
                Toast.makeText(this, "Trip too short, not saved!", Toast.LENGTH_LONG).show();
                curtrip_.setStatus(0);
            }
            curtrip_.setEndTime(System.currentTimeMillis());
            DriveSenseApp.DBHelper().updateTrip(curtrip_);
            TripUploadRequest.Start();
            curtrip_ = null;
        }

        stopSelf();
    }


    private void startSensors() {
        Intent senI = new Intent(this, SensorService.class);
        startService(senI);

        Toast.makeText(this, "Trip recording service starting in background.", Toast.LENGTH_SHORT).show();
        user = DriveSenseApp.DBHelper().getCurrentUser();

        LocalBroadcastManager.getInstance(this).registerReceiver(mSensorMessageReceiver, new IntentFilter("sensor"));
    }
    private void stopSensors() {
        Intent senI = new Intent(this, SensorService.class);
        stopService(senI);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSensorMessageReceiver);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy for tripservice called");
        stopSensors();
    }

    public Trip getCurtrip() {
        return curtrip_;
    }


    /**
     * where we get the sensor data
     */
    private long lastGPS = 0;
    private long lastSpeedNonzero = 0;
    private boolean stoprecording = false;
    private BroadcastReceiver mSensorMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TraceMessage message = GsonSingleton.fromJson(intent.getStringExtra("trace"), TraceMessage.class);
            Trace trace = message.value;
            long curtime = trace.time;

            if(trace == null) return;
            if(curtrip_ == null) return;

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
            boolean endTripAuto= SettingActivity.getEndTripAuto(context);
            if(!stoprecording) {
                try {
                    message.rowid = DriveSenseApp.DBHelper().insertSensorData(curtrip_.uuid.toString(), trace);
                    DriveSenseApp.DBHelper().updateTrip(curtrip_);
                    unsentMessages.add(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ((curtime - lastSpeedNonzero > 60*60*1000) && endTripAuto){
                //send trip ended broadcast
                stopRecordingTrip();
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



            if(curtime - lastSpeedNonzero > SettingActivity.getPauseTimeout(context)) {
                stoprecording = true;
            } else {
                stoprecording = false;
            }
        }

    };
}
