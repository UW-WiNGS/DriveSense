
package wisc.drivesense.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import com.facebook.FacebookSdk;
//import com.facebook.appevents.AppEventsLogger;

import com.google.gson.Gson;

import java.io.File;

import wisc.drivesense.R;
import wisc.drivesense.triprecorder.TripService;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;


public class MainActivity extends AppCompatActivity {

    //for display usage only, all calculation is conducted in TripService

    private Trip curtrip_ = null;


    private static String TAG = "MainActivity";

    private TextView tvSpeed = null;
    private TextView tvMile = null;
    private TextView tvTilt = null;
    private Button btnStart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initializing Facebook Integration

        tvSpeed = (TextView) findViewById(R.id.textspeed);
        tvMile = (TextView) findViewById(R.id.milesdriven);
        tvTilt = (TextView) findViewById(R.id.texttilt);
        btnStart = (Button) findViewById(R.id.btnstart);

        //tvTilt.setVisibility(View.VISIBLE);
        tvTilt.setText(String.format("%.0f", 0.0) + (char) 0x00B0);


        android.support.v7.widget.Toolbar mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(mToolbar);

        File dbDir = new File(Constants.kDBFolder);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        addListenerOnButton();
        //FacebookSdk.sdkInitialize(getApplicationContext());
        //AppEventsLogger.activateApp(this);
    }


    private class TripServiceConnection implements ServiceConnection {
        private TripService.TripServiceBinder binder = null;

        public void onServiceConnected(ComponentName className, IBinder service) {
            binder = ((TripService.TripServiceBinder)service);
            curtrip_ = binder.getTrip();
        }
        public void onServiceDisconnected(ComponentName className) {
            binder = null;
        }
    };
    private Intent mTripServiceIntent = null;
    private ServiceConnection mTripConnection = null;

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onSTop");

    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPuase");
        if(mTripConnection != null) {
            unbindService(mTripConnection);
        }
    }
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");


        if (MainActivity.isServiceRunning(this, TripService.class) == true) {
            btnStart.setBackgroundResource(R.drawable.stop_button);
            btnStart.setText(R.string.stop_button);
            //if the service is running, then start the connnection
            mTripServiceIntent = new Intent(this, TripService.class);
            mTripConnection = new TripServiceConnection();
            bindService(mTripServiceIntent, mTripConnection, Context.BIND_AUTO_CREATE);
        } else {
            btnStart.setBackgroundResource(R.drawable.start_button);
            btnStart.setText(R.string.start_button);
        }
    }


    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if(SettingActivity.isAutoMode(MainActivity.this)) {
            Toast.makeText(MainActivity.this, "Disable Auto Mode to Stop", Toast.LENGTH_SHORT).show();
            return;
        }
        mTripServiceIntent = new Intent(this, TripService.class);
        stopService(mTripServiceIntent);
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
                Log.d(TAG, "start button clicked");
                if (MainActivity.isServiceRunning(MainActivity.this, TripService.class) == false) {
                    //Toast.makeText(MainActivity.this, "Service Started!", Toast.LENGTH_SHORT).show();
                    startRunning();
                    btnStart.setBackgroundResource(R.drawable.stop_button);
                    btnStart.setText(R.string.stop_button);
                } else {
                    //Toast.makeText(MainActivity.this, "Service Stopped!", Toast.LENGTH_SHORT).show();
                    stopRunning();
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
    private synchronized void startRunning() {
        Log.d(TAG, "start running");

        //curtrip_ = new Trip(System.currentTimeMillis());
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("driving"));

        mTripServiceIntent = new Intent(this, TripService.class);
        mTripConnection = new TripServiceConnection();
        if(MainActivity.isServiceRunning(this, TripService.class) == false) {
            Log.d(TAG, "Start driving detection service!!!");
            bindService(mTripServiceIntent, mTripConnection, Context.BIND_AUTO_CREATE);
            startService(mTripServiceIntent);
        }
    }

    private synchronized void stopRunning() {

        Log.d(TAG, "Stopping live data..");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        tvSpeed.setText(String.format("%.1f", 0.0));
        tvMile.setText(String.format("%.2f", 0.00));
        tvTilt.setText(String.format("%.0f", 0.0) + (char) 0x00B0);

        if(MainActivity.isServiceRunning(this, TripService.class) == true) {
            Log.d(TAG, "Stop driving detection service!!!");
            stopService(mTripServiceIntent);
            unbindService(mTripConnection);
            mTripConnection = null;
            mTripServiceIntent = null;
        }

    }


    private void displayWarning() {
        Toast toast = new Toast(MainActivity.this);
        ImageView view = new ImageView(MainActivity.this);
        view.setImageResource(R.drawable.attention_512);

        toast.setView(view);
        toast.show();
    }

    //
    /**
     * where we get the sensor data
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("trip");
            Trace trace = new Trace();
            trace.fromJson(message);
            if(curtrip_ != null) {
                if(trace.type.equals(Trace.GPS)) {
                    Log.d(TAG, "Got message: " + message);
                    curtrip_.addGPS(trace);
                    tvSpeed.setText(String.format("%.1f", curtrip_.getSpeed()));
                    tvMile.setText(String.format("%.2f", curtrip_.getDistance() * Constants.kMeterToMile));
                    /*
                    if(curtrip_.getSpeed() >= 5.0 && trace.values[2] < 0) {
                        displayWarning();
                    }
                    */
                } else if(trace.type.equals(Trace.ACCELEROMETER)) {
                    tvTilt.setText(String.format("%.0f", curtrip_.getTilt()) + (char) 0x00B0);
                }
            }
        }
    };


    public void showDriveRating() {
        Log.d(TAG, "in showDriveRating");
        Gson gson = new Gson();
        Log.d(TAG, gson.toJson(curtrip_));
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

}


