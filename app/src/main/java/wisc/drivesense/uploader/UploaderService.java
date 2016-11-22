package wisc.drivesense.uploader;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trip;

/**
 * Created by lkang on 3/30/16.
 */
public class UploaderService extends Service {

    private static String TAG = "uploader";
    //doing nothing at this version
    private DatabaseHelper dbHelper_ = new DatabaseHelper();

    private AtomicBoolean isRunning_ = new AtomicBoolean(false);

    //private SendHttpRequestTask httpRequest = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "oncreate");
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, TAG + " onDestroy", Toast.LENGTH_LONG).show();
        stopService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);
        //Toast.makeText(this, TAG + " onStartCommand", Toast.LENGTH_LONG).show();

        startService();
        return START_STICKY;
    }

    private void startService() {
        Log.d(TAG, "Starting uploding service");
        isRunning_.set(true);
    }

    public void stopService() {
        Log.d(TAG, "Stopping service..");

        if(dbHelper_ != null && dbHelper_.isOpen()) {
            dbHelper_.closeDatabase();
        }

        isRunning_.set(false);
        stopSelf();

    }

}
