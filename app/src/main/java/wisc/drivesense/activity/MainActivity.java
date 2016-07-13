
package wisc.drivesense.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

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
    private Button btnStart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSpeed = (TextView) findViewById(R.id.textspeed);
        tvMile = (TextView) findViewById(R.id.milesdriven);
        btnStart = (Button) findViewById(R.id.btnstart);

        android.support.v7.widget.Toolbar mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(mToolbar);

        File dbDir = new File(Constants.kDBFolder);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        addListenerOnButton();
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
                    showDriveRating();
                    stopRunning();
                    btnStart.setBackgroundResource(R.drawable.start_button);
                    btnStart.setText(R.string.start_button);
                }
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.user:
                showUser();
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
            Log.d(TAG, "Got message: " + message);
            Trace trace = new Trace();
            trace.fromJson(message);
            if(curtrip_ != null) {
                curtrip_.addGPS(trace);
                tvSpeed.setText(String.format("%.1f", curtrip_.getSpeed()));
                tvMile.setText(String.format("%.1f", curtrip_.getDistance()));
            }
            if(trace.values[2] < 0) {
                displayWarning();
            }
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

    public void showUser() {
        Intent intent = new Intent(this, UserActivity.class);
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

    /*
    private Handler handler = null;
    private Runnable runnable;
    private String timeFormat(long millis) {
        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        return hms;
    }
    public void startTimer() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000);
                long ms = 0;
                if(curtrip_ != null) {
                    ms = System.currentTimeMillis() - curtrip_.getStartTime();
                }
                tvTime.setText(timeFormat(ms));
            }
        };
        handler.postDelayed(runnable, 0);
    }
    */
}


