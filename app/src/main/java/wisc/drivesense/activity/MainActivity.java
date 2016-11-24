
package wisc.drivesense.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.app.Fragment;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;

import wisc.drivesense.R;
import wisc.drivesense.triprecorder.TripService;
import wisc.drivesense.uploader.TripUploadRequest;
import wisc.drivesense.user.UserActivity;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;


public class MainActivity extends AppCompatActivity {

    //for display usage only, all calculation is conducted in TripService

    private Trip curtrip_ = null;


    private static String TAG = "MainActivity";
    private MapViewFragment mapFragment;
    private TextView tvSpeed = null;
    private TextView tvMile = null;
    private TextView tvTilt = null;
    private Button btnStart = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }


        // Initializing Facebook Integration

        tvSpeed = (TextView) findViewById(R.id.textspeed);
        tvMile = (TextView) findViewById(R.id.milesdriven);
        tvTilt = (TextView) findViewById(R.id.texttilt);
        btnStart = (Button) findViewById(R.id.btnstart);

        //tvTilt.setVisibility(View.VISIBLE);
        tvTilt.setText(String.format("%.0f", 0.0) + (char) 0x00B0);


        Toolbar mToolbar = (Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(mToolbar);

        addListenerOnButton();
    }


    @Override
    public void onStart() {
            super.onStart();
    }


    private class TripServiceConnection implements ServiceConnection {
        private TripService.TripServiceBinder binder = null;

        public void onServiceConnected(ComponentName className, IBinder service) {
            binder = ((TripService.TripServiceBinder) service);
            curtrip_ = binder.getTrip();
        }

        public void onServiceDisconnected(ComponentName className) {
            binder = null;
        }
    }

    ;
    private Intent mTripServiceIntent = null;
    private ServiceConnection mTripConnection = null;

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onSTop");
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPuase");
        if (mTripConnection != null) {
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

        if (SettingActivity.isAutoMode(MainActivity.this)) {
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
                if (SettingActivity.isAutoMode(MainActivity.this)) {
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

    private synchronized void startRunning() {
        Log.d(TAG, "start running");

        //curtrip_ = new Trip(System.currentTimeMillis());
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("sensor"));

        if(SettingActivity.showMapWhileDriving(MainActivity.this)) {
            displayMapFragment();
        }
        mTripServiceIntent = new Intent(this, TripService.class);
        mTripConnection = new TripServiceConnection();
        if (MainActivity.isServiceRunning(this, TripService.class) == false) {
            Log.d(TAG, "Start driving detection service!!!");
            bindService(mTripServiceIntent, mTripConnection, Context.BIND_AUTO_CREATE);
            startService(mTripServiceIntent);
        }
    }

    private void displayMapFragment() {
        findViewById(R.id.textspeed).setVisibility(View.GONE);
        mapFragment = MapViewFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mapFragment)
                .commit();
    }

    private void hideMapFragment() {
        if(mapFragment != null) {
            findViewById(R.id.textspeed).setVisibility(View.VISIBLE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(mapFragment)
                    .commit();
        }
        mapFragment = null;
    }

    private synchronized void stopRunning() {

        Log.d(TAG, "Stopping live data..");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        hideMapFragment();
        tvSpeed.setText(String.format("%.1f", 0.0));
        tvMile.setText(String.format("%.2f", 0.00));
        tvTilt.setText(String.format("%.0f", 0.0) + (char) 0x00B0);

        if (MainActivity.isServiceRunning(this, TripService.class) == true) {
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

    public void sendToRealTimeMapFragment(Trace gps) {
//        mMapFrag.updateWTrace
    }
    //
    /**
     * where we get the sensor data
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("trace");
            Trace trace = GsonSingleton.fromJson(message, TraceMessage.class).value;
            if (curtrip_ != null) {
                if (trace instanceof Trace.GPS) {
                    Log.d(TAG, "Got message: " + message);
                    sendToRealTimeMapFragment(trace);
                    tvSpeed.setText(String.format("%.1f", ((Trace.GPS) trace).speed * Constants.kMeterPSToMilePH));
                    tvMile.setText(String.format("%.2f", curtrip_.getDistance() * Constants.kMeterToMile));
                    /*
                    if(curtrip_.getSpeed() >= 5.0 && trace.values[2] < 0) {
                        displayWarning();
                    }
                    */
                } else if (trace instanceof Trace.Accel) {
                    tvTilt.setText(String.format("%.0f", curtrip_.getTilt()) + (char) 0x00B0);
                }
            }
        }
    };


    public void showDriveRating() {
        Log.d(TAG, "in showDriveRating");
        String trip_string = GsonSingleton.toJson(curtrip_);
        Log.d(TAG, trip_string);
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("Current Trip", trip_string);
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


