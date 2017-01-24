package wisc.drivesense.triprecorder;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Rating;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;

public class SensorService extends Service implements SensorEventListener, LocationListener {

    private final Binder binder_ = null; //new SensorBinder();
    private AtomicBoolean isRunning_ = new AtomicBoolean(false);

    private SensorManager sensorManager;
    private LocationManager locationManager;

    private RealTimeTiltCalculation tiltCalc = new RealTimeTiltCalculation();
    private Rating rating = new Rating(tiltCalc);

    int numberOfSensors = 3;
    int[] sensorType = {Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD,
    };

    /*Marked: for orientation*/
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];


    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];

    private long tLastGyroscope = 0;
    private long tLastAccelerometer = 0;
    private long tLastMagnetometer = 0;


    private final String TAG = "Sensor Service";


    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Trace.GPS trace = new Trace.GPS();
            trace.time = System.currentTimeMillis();
            trace.lat = (float) location.getLatitude();
            trace.lng = (float) location.getLongitude();
            trace.speed = location.getSpeed();
            trace.alt = (float) location.getAltitude();

            Trace.Trip tt = rating.getRating(trace);
            //TODO: Maybe generate ratings separately
            sendTrace(tt);
        }
    }

    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub
        Log.d(TAG, arg0 + " " + arg1 + " " + arg2);
        Trace.GPSStatus trace = new Trace.GPSStatus();
        trace.time = System.currentTimeMillis();
        if(arg1 == LocationProvider.AVAILABLE) {
            trace.values(new float[]{1,1,1});
        } else {
            trace.values(new float[]{0,0,0});
        }
        sendTrace(trace);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRunning_.get() == false) {
            return;
        }

        int type = event.sensor.getType();
        long time = System.currentTimeMillis();
        if (type == Sensor.TYPE_MAGNETIC_FIELD && (time - tLastMagnetometer) >= Constants.kRecordingInterval) {
            tLastMagnetometer = time;
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;

            Trace.Magnetometer trace = new Trace.Magnetometer();
            trace.time = time;
            trace.values(event.values);

            sendTrace(trace);

        } else if (type == Sensor.TYPE_ACCELEROMETER && (time - tLastAccelerometer) >= Constants.kRecordingInterval) {
            tLastAccelerometer = time;
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;

            Trace.Accel trace = new Trace.Accel();
            trace.time = time;
            trace.values(event.values);
            sendTrace(trace);

        } else if (type == Sensor.TYPE_GYROSCOPE && (time - tLastGyroscope) >= Constants.kRecordingInterval) {
            //Log.e(TAG, tLastGyroscope + "," + time + "," + String.valueOf(time - tLastGyroscope));

            tLastGyroscope = time;

            Trace.Gyro trace = new Trace.Gyro();
            trace.time = time;
            trace.values(event.values);
            sendTrace(trace);

        }

        /*Marked*/
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            mLastMagnetometerSet = false;
            mLastAccelerometerSet = false;

            Trace.Rotation trace = new Trace.Rotation();
            trace.time = time;
            trace.values(mR);
            sendTrace(trace);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return binder_;
    }
    /*
    public class SensorBinder extends Binder {
        public void setDatabaseHelper(DatabaseHelper dbhelper) {
            dbHelper_ = dbhelper;
        }
        public boolean isRunning() {
            return isRunning_.get();
        }
        public SensorService getService() {
            return SensorService.this;
        }
        public double getSpeed() {return speed_;}
    }
    */

    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "stop service");
        sensorManager.unregisterListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
        }
        isRunning_.set(false);
        stopSelf();
    }

    private void startService() {
        Log.d(TAG, "start service");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        for (int i = 0; i < numberOfSensors; ++i) {
            Sensor sensor = sensorManager.getDefaultSensor(sensorType[i]);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        else
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        isRunning_.set(true);
    }

    private void sendTrace(Trace trace) {
        tiltCalc.processTrace(trace);
        //Log.d(TAG, trace.toJson());
        Intent intent = new Intent("sensor");
        intent.putExtra("trace", GsonSingleton.toJson(new TraceMessage(trace)));

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}