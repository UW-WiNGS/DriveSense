package wisc.drivesense.triprecorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final long SEND_INTERVAL = 1000;
    private DriveSenseToken user = null;
    private Trip curtrip_ = null;
    private long lastSpeedNonzero = 0;
    private boolean stoprecording = false;
    private TraceStorageWorker tsw;

    public Binder _binder = new TripServiceBinder();

    private final String TAG = "Trip Service";

    private final int ONGOING_NOTIFICATION_ID = 1;
    public static final String START_IMMEDIATELY = "startImmediately";
    public static final String TRIP_STATUS_CHANGE = "tripStatusChange";

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
        } else if (intent.getBooleanExtra(START_IMMEDIATELY, false)) {
            startRecordingNewTrip();
        }

        registerReceiver(mPowerDisconnectedReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));

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
        lastSpeedNonzero = 0;
        stoprecording = false;

        Log.d(TAG, "Start driving detection service. UUID: "+curtrip_.uuid);
        Toast.makeText(this, "Trip recording service starting in background.", Toast.LENGTH_SHORT).show();
        user = DriveSenseApp.DBHelper().getCurrentUser();

        tsw = new TraceStorageWorker(curtrip_.uuid.toString());
        tsw.start();

        startSensors();

        Intent tsi = new Intent(this, TripService.class);
        startService(tsi);
        startForeground();

        Intent intent = new Intent(TRIP_STATUS_CHANGE);
        intent.putExtra("recording", true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void stopRecordingTrip() {
        stopForeground(true);
        stopSensors();
        tsw.stopRunning();
        try {
            tsw.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        //broadcast a notification that the trip is ending
        Intent intent = new Intent(TRIP_STATUS_CHANGE);
        intent.putExtra("recording", false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        stopSelf();
    }


    private void startSensors() {
        Intent senI = new Intent(this, SensorService.class);
        startService(senI);

        LocalBroadcastManager.getInstance(this).registerReceiver(mSensorMessageReceiver, new IntentFilter("sensor"));
    }
    private void stopSensors() {
        Intent senI = new Intent(this, SensorService.class);
        stopService(senI);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSensorMessageReceiver);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy for tripservice called");
        try {
            unregisterReceiver(mPowerDisconnectedReceiver);
        } catch (IllegalArgumentException e) {

        }
        stopSensors();
    }

    public Trip getCurtrip() {
        return curtrip_;
    }

    //the service is started by the ChargingStateReceiver, but needs to stop itself when power disconnnected
    private BroadcastReceiver mPowerDisconnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(curtrip_ != null)
                stopRecordingTrip();
        }
    };

    /**
     * where we get the sensor data
     */
    private BroadcastReceiver mSensorMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TraceMessage message = GsonSingleton.fromJson(intent.getStringExtra("trace"), TraceMessage.class);
            Trace trace = message.value;
            long curtime = trace.time;

            if(trace == null) return;
            if(curtrip_ == null) return;

            if(lastSpeedNonzero == 0) {
                lastSpeedNonzero = trace.time;
            }

            if(trace instanceof Trace.GPS) {
                Trace.GPS gps = (Trace.GPS)trace;
                //Log.d(TAG, "Got message: " + trace.toJson());

                if(gps.speed != 0.0) {
                    lastSpeedNonzero = gps.time;
                }

                curtrip_.addGPS(gps);
            }

            if(trace instanceof Trace.Trip) {
                curtrip_.setTilt(((Trace.Trip) trace).tilt);
                curtrip_.setScore(((Trace.Trip) trace).score);
            }
            boolean endTripAuto= SettingActivity.getEndTripAuto(context);
            if(!stoprecording) {
                try {
                    tsw.addTrace(message, curtrip_.getDistance());
                    //update trip async?

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ((curtime - lastSpeedNonzero > context.getResources().getInteger(R.integer.end_trip_inactivity_timeout)*1000) && endTripAuto){
                //send trip ended broadcast
                stopRecordingTrip();
            }

            if(curtime - lastSpeedNonzero > SettingActivity.getPauseTimeout(context)) {
                stoprecording = true;
            } else {
                stoprecording = false;
            }
        }

    };

    private class TraceStorageWorker extends Thread {
        private static final String TAG = "TraceStorageWorker";
        private LinkedBlockingQueue<TraceMessage> traces;
        private String tripUUID;
        private long lastSent = 0;
        private volatile double curDistance;
        ArrayList<TraceMessage> unsentMessages = new ArrayList<TraceMessage>();
        private boolean running = true;
        public TraceStorageWorker(String tripUUID) {
            traces = new LinkedBlockingQueue();
            this.tripUUID = tripUUID;
        }
        public void addTrace(TraceMessage tm, double curDistance) {
            try {
                //traces.put is thread safe
                traces.put(tm);
                this.curDistance = curDistance;
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
        public void stopRunning() {
            running = false;
            this.interrupt();
        }
        public void run() {
            while (running || traces.size()!=0) {
                try {
                    ArrayList<TraceMessage> tmList = new ArrayList<>(traces.size());
                    traces.drainTo(tmList);
                    long[] rowids = DriveSenseApp.DBHelper().insertSensorData(tripUUID, tmList);
                    for (int i = 0; i < tmList.size(); i++) {
                        TraceMessage tm = tmList.get(i);
                        tm.rowid = rowids[i];
                        unsentMessages.add(tm);
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Worker thread was interrupted");
                } catch (Exception e) {
                    Log.e(TAG, "Something went wrong inserting a row of sensor data");
                    e.printStackTrace();
                }

                if (!unsentMessages.isEmpty() && System.currentTimeMillis() - lastSent > SEND_INTERVAL && user != null) {
                    //TODO: I don't think this is thread safe
                    DriveSenseApp.DBHelper().updateTrip(curtrip_);
                    Log.d(TAG, "Worker queue size: "+traces.size());
                    Log.d(TAG, "Uploading " + unsentMessages.size() + " traces.");
                    TripPayload payload = new TripPayload();
                    payload.guid = tripUUID;
                    payload.traces = unsentMessages;
                    payload.distance = curDistance;
                    lastSent = System.currentTimeMillis();
                    unsentMessages = new ArrayList<>();

                    TripUploadRequest.Start(payload);
                }
            }
            Log.d(TAG, "Worker thread done running");
        }
    }
}
