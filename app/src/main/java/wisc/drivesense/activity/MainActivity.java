
package wisc.drivesense.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.R;
import wisc.drivesense.activity.history.HistoryActivity;
import wisc.drivesense.activity.history.TripViewActivity;
import wisc.drivesense.triprecorder.TripService;
import wisc.drivesense.user.UserActivity;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.Units;


public class MainActivity extends AppCompatActivity {

    //for display usage only, all calculation is conducted in TripService

    private static String TAG = "MainActivity";
    private LiveMapViewFragment mapFragment;
    @BindView(R.id.speed_display) TextView tvSpeed;
    @BindView(R.id.speed_unit) TextView tvSpeedUnit;
    @BindView(R.id.distance_driven) TextView tvTotalDistance;
    @BindView(R.id.distance_driven_unit) TextView tvTotalDistanceUnit;
    @BindView(R.id.texttilt) TextView tvTilt;
    @BindView(R.id.elapsedtime) TextView tvElapsed;
    @BindView(R.id.btnstart) Button btnStart;
    private ServiceConnection mTripConnection = null;
    private TripService boundTripService = null;
    private boolean displayingMap = false;
    private TimerTask elapsedTimeUpdater;
    private boolean metricUnits;
    private Handler noGPSHandler = new Handler();

    private class TripServiceConnection implements ServiceConnection {
        private TripService.TripServiceBinder binder = null;

        public void onServiceConnected(ComponentName className, IBinder service) {
            binder = ((TripService.TripServiceBinder) service);
            boundTripService = binder.getService();
            Log.d(TAG, "Bound to TripService");
            updateButton();
            if(binder.getService().getCurtrip()!= null) {
                startElapsedTime();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            binder = null;
            boundTripService=null;
        }
    }

    private Runnable noGPSRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "No GPS, resetting speed display.");
            tvSpeed.setText("--.--");
            if (boundTripService != null && boundTripService.getCurtrip() != null) {
                Units.userFacingDouble distance = Units.largeDistance(boundTripService.getCurtrip().getDistance(), metricUnits);
                tvTotalDistance.setText(String.format("*%.2f", distance.value));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }

        ButterKnife.bind(this);
        //tvTilt.setVisibility(View.VISIBLE);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(mToolbar);

        addListenerOnButton();
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        stopElapsedTime();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTraceMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecordingStatusChangedReciever);
        unbindTripService();
    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        metricUnits = SettingActivity.getMetricUnits(this);
        //only reset displays to 0 on resume if a trip is not being recorded
        if(boundTripService == null || boundTripService.getCurtrip() == null)
            resetNumericalDisplays();
        LocalBroadcastManager.getInstance(this).registerReceiver(mTraceMessageReceiver, new IntentFilter("sensor"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecordingStatusChangedReciever, new IntentFilter(TripService.TRIP_STATUS_CHANGE));
        bindTripService();
        updateButton();
    }

    private void updateButton() {
        if (boundTripService != null && boundTripService.getCurtrip() != null) {
            btnStart.setBackgroundResource(R.drawable.stop_button);
            btnStart.setText(R.string.stop_button);
        } else {
            btnStart.setBackgroundResource(R.drawable.start_button);
            btnStart.setText(R.string.start_button);
        }

        if(SettingActivity.showMapWhileDriving(MainActivity.this)) {
            displayMapFragment();
        } else {
            hideMapFragment();
        }
    }

    private void startElapsedTime() {
        if(elapsedTimeUpdater == null) {
            Timer t = new Timer();
            elapsedTimeUpdater = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateElapsedTime();
                        }
                    });
                }
            };
            t.scheduleAtFixedRate(elapsedTimeUpdater, 0, 1000);
        }
    }
    private void stopElapsedTime() {
        if(elapsedTimeUpdater != null) {
            elapsedTimeUpdater.cancel();
            elapsedTimeUpdater = null;
        }
    }
    private void resetGPSTimeout() {
        noGPSHandler.removeCallbacks(noGPSRunnable, null);
        noGPSHandler.postDelayed(noGPSRunnable, 10000);
    }
    private void updateElapsedTime () {
        if (boundTripService != null && boundTripService.getCurtrip() != null) {
            long duration = System.currentTimeMillis() - boundTripService.getCurtrip().getStartTime();
            tvElapsed.setText(Units.displayTimeInterval(duration));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    //get the selected dropdown list value
    public void addListenerOnButton() {
        final Context context = this;
        //final TextView txtView= (TextView) findViewById(R.id.textspeed);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Start button clicked");
                if (boundTripService != null && boundTripService.getCurtrip() == null) {
                    startRecording();
                } else if(boundTripService != null) {
                    stopRecording();
                    showDriveRating();
                }
                updateButton();
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.user:
                startActivity(new Intent(this, UserActivity.class));
                return true;

            case R.id.settings:
                showSettings();
                return true;

            case R.id.history:
                showHistory();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1001: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "Got permission to use location");
                }
            }
        }
    }

    private void bindTripService() {
        Intent tsi = new Intent(this, TripService.class);
        mTripConnection = new TripServiceConnection();
        bindService(tsi, mTripConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindTripService() {
        Intent tsi = new Intent(this, TripService.class);
        unbindService(mTripConnection);
        mTripConnection = null;
    }

    private void startRecording() {
        if(boundTripService != null && boundTripService.getCurtrip() == null){
            boundTripService.startRecordingNewTrip();
            startElapsedTime();
            resetGPSTimeout();
            if(mapFragment != null)
                mapFragment.clearMarkers();
        }
    }

    private void stopRecording() {
        if (boundTripService != null && boundTripService.getCurtrip() != null) {
            noGPSHandler.removeCallbacks(noGPSRunnable, null);
            stopElapsedTime();
            boundTripService.stopRecordingTrip();
        }

        Log.d(TAG, "Stopping live data..");
        resetNumericalDisplays();
    }

    private void resetNumericalDisplays() {
        Units.userFacingDouble speed = Units.speed(0, metricUnits);
        tvSpeed.setText("--.--");
        tvSpeedUnit.setText(speed.unitName);
        Units.userFacingDouble distance = Units.largeDistance(0, metricUnits);
        tvTotalDistance.setText(String.format("*%.2f",  distance.value));
        tvTotalDistanceUnit.setText(distance.unitName);
        tvTilt.setText(String.format("%.0f", 0.0) + (char) 0x00B0);
        tvElapsed.setText(Units.displayTimeInterval(0));
    }

    private void displayMapFragment() {
        findViewById(R.id.speed_display).setVisibility(View.GONE);
        findViewById(R.id.speed_unit).setVisibility(View.GONE);
        if(!displayingMap) {
            mapFragment = LiveMapViewFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, mapFragment)
                    .commit();
            displayingMap = true;
        }
    }

    private void hideMapFragment() {
        if(mapFragment != null) {
            findViewById(R.id.speed_display).setVisibility(View.VISIBLE);
            findViewById(R.id.speed_unit).setVisibility(View.VISIBLE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(mapFragment)
                    .commit();
        }
        mapFragment = null;
        displayingMap = false;
    }

    public void sendToRealTimeMapFragment(Trace gps) {
        if(gps != null && mapFragment != null) {
            mapFragment.addMarker(gps);
        }
    }
    //
    /**
     * where we get the sensor data
     */
    private BroadcastReceiver mRecordingStatusChangedReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received broadcast saying that trip recording status changed.");
            updateButton();
        }
    };

    private BroadcastReceiver mTraceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("trace");
            Trace trace = GsonSingleton.fromJson(message, TraceMessage.class).value;
            if (boundTripService != null && boundTripService.getCurtrip() != null) {
                if (trace instanceof Trace.GPS) {
                    sendToRealTimeMapFragment(trace);
                    resetGPSTimeout();
                    Units.userFacingDouble speed = Units.speed(((Trace.GPS) trace).speed, metricUnits);
                    tvSpeed.setText(String.format("%.1f", speed.value));
                    tvSpeedUnit.setText(speed.unitName);
                    Units.userFacingDouble distance = Units.largeDistance(boundTripService.getCurtrip().getDistance(), metricUnits);
                    tvTotalDistance.setText(String.format("%.2f",  distance.value));
                    tvTotalDistanceUnit.setText(distance.unitName);
                } else if (trace instanceof Trace.Accel) {
                    tvTilt.setText(String.format("%.0f", boundTripService.getCurtrip().getTilt()) + (char) 0x00B0);
                }
            }
        }
    };


    public void showDriveRating() {
        Trip lastTrip = DriveSenseApp.DBHelper().getLastTrip();
        if(lastTrip != null && lastTrip.getStatus() == 2) {
            Intent intent = new Intent(this, TripViewActivity.class);
            intent.putExtra("guid", lastTrip.guid.toString());
            startActivity(intent);
        } else {
            Log.d(TAG, "Tried to show a trip, but it was null or deleted.");
        }
    }

    public void showSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void showHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }
}


