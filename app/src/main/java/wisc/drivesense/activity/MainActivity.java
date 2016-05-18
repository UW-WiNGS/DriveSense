
package wisc.drivesense.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import wisc.drivesense.R;
import wisc.drivesense.triprecorder.TripService;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;


public class MainActivity extends AppCompatActivity {

    //for display usage only, all calculation is conducted in TripService
    private Trip curtrip_ = null;

    private int started = 0;

    private static String TAG = "MainActivity";

    private TextView tvSpeed = null;
    private TextView tvMiles = null;
    private Button btnStart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSpeed = (TextView) findViewById(R.id.textspeed);
        tvMiles = (TextView) findViewById(R.id.milesdriven);
        btnStart = (Button) findViewById(R.id.btnstart);

        android.support.v7.widget.Toolbar mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(mToolbar);

        File dbDir = new File(Constants.kDBFolder);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        addListenerOnButton();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    //get the selected dropdown list value
    public void addListenerOnButton() {

        //final TextView txtView= (TextView) findViewById(R.id.textspeed);
        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(SettingActivity.isAutoMode(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Disable Auto Mode in Settings", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (started == 0) {
                    //Toast.makeText(MainActivity.this, "Service Started!", Toast.LENGTH_SHORT).show();
                    startRunning();
                    started = 1;
                    btnStart.setBackgroundResource(R.drawable.stop_button);
                    btnStart.setText(R.string.stop_button);
                } else {
                    //Toast.makeText(MainActivity.this, "Service Stopped!", Toast.LENGTH_SHORT).show();
                    stopRunning();
                    started = 0;
                    btnStart.setBackgroundResource(R.drawable.start_button);
                    btnStart.setText(R.string.start_button);
                    showDriveRating();
                }
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
    private static Intent mTripServiceIntent = null;
    private synchronized void startRunning() {
        Log.d(TAG, "start running");

        curtrip_ = new Trip(System.currentTimeMillis());
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("driving"));

        mTripServiceIntent = new Intent(this, TripService.class);
        if(MainActivity.isServiceRunning(this, TripService.class) == false) {
            Log.d(TAG, "Start driving detection service!!!");
            startService(mTripServiceIntent);
        }
    }

    private synchronized void stopRunning() {

        Log.d(TAG, "Stopping live data..");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        if(curtrip_.getDistance() < 0.3 || curtrip_.getDuration() < 1.0) {
            Toast.makeText(MainActivity.this, "Trip too short, not saved!", Toast.LENGTH_SHORT).show();
        }

        tvSpeed.setText(String.format("%.1f", 0.0));
        tvMiles.setText(String.format("%.2f", 0.00));


        if(MainActivity.isServiceRunning(this, TripService.class) == true) {
            Log.d(TAG, "Stop driving detection service!!!");
            stopService(mTripServiceIntent);
            mTripServiceIntent = null;
        }

    }


    //
    /**
     * where we get the sensor data
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("trip");
            Log.d(TAG, "Got message: " + message);
            Trace trace = new Trace();
            trace.fromJson(message);
            curtrip_.addGPS(trace);
            tvSpeed.setText(String.format("%.1f", curtrip_.getSpeed()));
            tvMiles.setText(String.format("%.2f", curtrip_.getDistance()));
        }
    };


    public void showDriveRating() {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("Current Trip", curtrip_);
        startActivity(intent);
    }

    public void showSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void showHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }



    public static boolean isServiceRunning(Context context, Class running) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (running.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    protected void onPause() {
        super.onPause();
    }
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }



}


