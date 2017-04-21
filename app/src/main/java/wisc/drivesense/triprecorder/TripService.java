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
import java.util.concurrent.LinkedBlockingQueue;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.R;
import wisc.drivesense.activity.MainActivity;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.httpPayloads.TripPayload;
import wisc.drivesense.httpTools.TripUploadRequest;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.RatingCalculation;
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

    //TODO
    private RealTimeTiltCalculation tiltCalc;
    private RatingCalculation rating;

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

    //IMPORTANT: onStartCommand will be called automatically by Android after it has killed
    // a service to reclaim memory and is then restarting it
    // intent will be null in this case
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start command called on TripService");
        if(intent == null) {
            //Null intent means the service was killed and is being restarted by android
            curtrip_ = DriveSenseApp.DBHelper().getLastTrip();
            if(curtrip_!=null) {
                List<Trace.Trip> points_ = DriveSenseApp.DBHelper().getGPSPoints(curtrip_.guid.toString());
                curtrip_.setGPSPoints(points_);
                Log.d(TAG, "Trip distance: "+curtrip_.getDistance() + " gps length: "+curtrip_.getGPSPoints().size());
                Log.d(TAG, "Restart driving detection service after being killed by android. UUID: "+curtrip_.guid);

                startRecording();
            } else {
                Log.d(TAG, "TripService was restarted, but no unfinalized trip was found");
            }
        } else if (intent.getBooleanExtra(START_IMMEDIATELY, false)) {
            startRecordingNewTrip();
        }

        registerReceiver(mPowerDisconnectedReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));

        return START_STICKY;
    }

    /**
     * Display a running status on the Android status bar
     */
    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("DriveSense Trip Recording")
                .setContentText("Currently recording a trip.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        //make it less likely to be killed by Android
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    /**
     * Initialize tripservice with a new curtrip_ and start recording sensor data.
     */
    public void startRecordingNewTrip() {
        curtrip_ = new Trip();
        DriveSenseApp.DBHelper().insertTrip(curtrip_);

        startRecording();
    }

    /**
     * Start recording (assumes curtrip_ has already been initialized)
     * Used to either start recording a new trip or resume recording after TripService was killed by Android
     *
     * If you want to start a new empty trip, call startRecordingNewTrip
     */
    private void startRecording() {
        lastSpeedNonzero = 0;
        stoprecording = false;

        Log.d(TAG, "Start driving detection service. UUID: "+curtrip_.guid);
        Toast.makeText(this, "Trip recording service starting in background.", Toast.LENGTH_SHORT).show();
        user = DriveSenseApp.DBHelper().getCurrentUser();

        tsw = new TraceStorageWorker(curtrip_.guid.toString(), this);
        tsw.start();

        tiltCalc = new RealTimeTiltCalculation();
        rating = new RatingCalculation(curtrip_.getGPSPoints().size(), curtrip_.getScore());

        startSensors();

        Intent tsi = new Intent(this, TripService.class);
        //trip service calls startService on itself when actively recording, to make sure it is persistent
        // even if the activity bound to it unbinds
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
            TripUploadRequest.Start(this);
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
            if(curtrip_ != null && SettingActivity.getAutoStop(context) == true)
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

            tiltCalc.processTrace(trace);

            if(trace instanceof Trace.GPS) {
                Trace.Trip tt = rating.getRating((Trace.GPS)trace);
                tt.tilt = (float)tiltCalc.getTilt();
                if(tt.speed != 0.0) {
                    lastSpeedNonzero = tt.time;
                }

                curtrip_.addGPS(tt);
                curtrip_.setScore(tt.score);
                curtrip_.setTilt(tt.tilt);

                //replace the contents of message with this triptrace instead of the GPS trace
                message = new TraceMessage(tt);
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
            boolean pauseRecordingPref = SettingActivity.getPauseWhenStationary(context);
            if(((curtime - lastSpeedNonzero) > context.getResources().getInteger(R.integer.default_pause_timeout) * 1000)
                    && pauseRecordingPref) {
                if(stoprecording == false)
                    Log.d(TAG, "Pausing trip recording because of no movement");
                stoprecording = true;
            } else {
                stoprecording = false;
            }
        }

    };

    //handle trace, by insert into database or upload
    private class TraceStorageWorker extends Thread {
        private static final String TAG = "TraceStorageWorker";
        private LinkedBlockingQueue<TraceMessage> traces;
        private String tripUUID;
        private long lastSent = 0;
        private Context context;
        private volatile double curDistance;
        ArrayList<TraceMessage> unsentMessages = new ArrayList<TraceMessage>();
        private volatile boolean running = true;
        public TraceStorageWorker(String tripUUID, Context context) {
            traces = new LinkedBlockingQueue();
            this.tripUUID = tripUUID;
            this.context = context;
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
        public boolean isRunning() {
            return running;
        }
        public void run() {
            //even if runnning == false we check size() to drain the queue of traces at the end
            //if it is running, it does not check the size of traces
            while (running || traces.size()!=0) {
                try {
                    ArrayList<TraceMessage> tmList = new ArrayList<>(traces.size());
                    traces.drainTo(tmList);
                    DriveSenseApp.DBHelper().updateTrip(curtrip_);
                    long[] rowids = DriveSenseApp.DBHelper().insertSensorData(tripUUID, tmList, false);
                    for (int i = 0; i < tmList.size(); i++) {
                        TraceMessage tm = tmList.get(i);
                        tm.rowid = rowids[i];
                        if(tm.value.getClass() == Trace.Trip.class) {
                            //only add GPS traces to be sent right now. Other traces will be synced later on WiFi
                            unsentMessages.add(tm);
                        }
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Worker thread was interrupted");
                } catch (Exception e) {
                    Log.e(TAG, "Something went wrong inserting a row of sensor data");
                    e.printStackTrace();
                }

                //for real time uploading
                if (!unsentMessages.isEmpty() && System.currentTimeMillis() - lastSent > SEND_INTERVAL && user != null) {
                    Log.d(TAG, "Worker queue size: "+traces.size());
                    Log.d(TAG, "Uploading " + unsentMessages.size() + " traces.");
                    TripPayload payload = new TripPayload();
                    payload.guid = tripUUID;
                    payload.traces = unsentMessages;
                    payload.distance = curDistance;
                    lastSent = System.currentTimeMillis();
                    unsentMessages = new ArrayList<>();

                    //it will fail silently if there is no internet connection
                    TripUploadRequest.Start(payload, context);
                }
            }
            Log.d(TAG, "Worker thread done running");
        }
    }
}
