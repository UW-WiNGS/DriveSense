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
    SendHttpRequestTask httpRequest = null;

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

        nextTask(null);
    }

    /**
     * message from server
     * @param msg
     */
    private void nextTask(String msg) {
        DriveSenseToken user = dbHelper_.getCurrentUser();
        if(user == null) {
            //no one is signed in
            stopService();
            return;
        }

        if(true == selectAndUploadOneFile(user.email)){
            return;
        }

        if(true == synchronizeDeletion(user.email)) {
            return;
        }


        stopService();

    }


    private boolean synchronizeDeletion(String email) {
        long [] trips = dbHelper_.tripsToSynchronize(email);
        if(trips == null) {
            return false;
        }
        Gson gson = new Gson();
        String[] params = new String[]{Constants.kSychronizeTripDeletion, gson.toJson(trips)};
        httpRequest = new SendHttpRequestTask();
        httpRequest.execute(params);
        return true;
    }

    private boolean selectAndUploadOneFile(String email) {
        //upload next trip
        long time = dbHelper_.nextTripToUpload(email);
        if(-1 == time) {
            return false;
        }
        String dbname = String.valueOf(time);
        String[] params = new String[]{Constants.kUploadTripDBFile, dbname};
        httpRequest = new SendHttpRequestTask();
        httpRequest.execute(params);
        return true;
    }

    private class Message {
        public String type;
        public String dbname;

        public String status;
        public String data;
        public void Message () {

        }
    }


    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

        protected void onPostExecute(String result) {
            Log.d(TAG, "uploading result:" + result);
            if(result == null) {
                //server is done, retry later
                stopService();
                return;
            }
            //confirm the trip is uploaded
            Gson gson = new Gson();
            Message msg = gson.fromJson(result, Message.class);
            if(msg.type.equals(Constants.kUploadTripDBFile)) {
                long time = Long.parseLong(msg.dbname);
                int upload = dbHelper_.tripUploadDone(time);
                Log.d(TAG, String.valueOf(upload));
            } else if (msg.type.equals(Constants.kSychronizeTripDeletion)) {
                //
                Type listType = new TypeToken<List<Long>>(){}.getType();
                List<Long> local = gson.fromJson(msg.dbname, listType);
                for(Long dbname: local) {
                    int sync = dbHelper_.tripSynchronizeDone(dbname);
                    Log.d(TAG, dbname + "," + String.valueOf(sync));
                }
                List<Long> server = gson.fromJson(msg.data, listType);
                for(Long dbname: server) {
                    dbHelper_.removeTrip(dbname);
                }
            } else {

            }
            this.cancel(true);
            //SystemClock.sleep(1000 * 10);
            //sleep will cause the activity hangs and not able to start
            nextTask(result);
        }


        protected String doInBackground(String... params) {
            String res = null;
            String type = params[0];
            if(type.equals(Constants.kUploadTripDBFile)) {
                String dbname = params[1];
                Log.d(TAG, "uploading DB file:" + dbname);
                //TODO: Replace with new trip uploading
                //res = uploadTrip(dbname);
                if(res == null) {
                    Log.d(TAG, "upload error, res is null");
                    return null;
                }
                Log.d(TAG, res);
                Gson gson = new Gson();
                Message msg = gson.fromJson(res, Message.class);
                msg.type = type;
                msg.dbname = dbname;
                if(msg.status.equals("success")) {
                    return gson.toJson(msg);
                } else {
                    return null;
                }
            } else if(type.equals(Constants.kSychronizeTripDeletion)) {
                Log.d(TAG, "sync with server");

                String tripnames = params[1];
                res = synchronizeDeletion(tripnames);
                if(res == null) {
                    Log.d(TAG, "sync deletion error, res is null");
                    return null;
                }
                Log.d(TAG, res);
                Gson gson = new Gson();
                Message msg = gson.fromJson(res, Message.class);
                msg.type = type;
                msg.dbname = tripnames;
                if(msg.status.equals("success")) {
                    return gson.toJson(msg);
                } else {
                    return null;
                }
            } else {
                Log.e(TAG, "unknown type in upload and sync");
                return null;
            }
        }

        private String synchronizeDeletion(String tripnames) {
            DriveSenseToken user = dbHelper_.getCurrentUser();
            if(user == null) {
                return null;
            }
            String useremail = user.email;
            String androidid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            String res = null;
            try {
                //TODO: Rewrite
                //HttpClient client = new HttpClient(Constants.kSyncDeleteURL);
                //client.connectForMultipart();
                //client.addFormPart("deviceid", androidid);
                //client.addFormPart("email", useremail);
                //client.addFormPart("model", Build.MANUFACTURER + "," + Build.MODEL);
                //client.addFormPart("tripnames", tripnames);
                //client.finishMultipart();
                //res = client.getResponse();
            } catch(Throwable t) {
                t.printStackTrace();
                return null;
            }
            return res;
        }
    }


    public void stopService() {
        Log.d(TAG, "Stopping service..");

        if(httpRequest != null && httpRequest.isCancelled() == false) {
            httpRequest.cancel(true);
            httpRequest = null;
        }

        if(dbHelper_ != null && dbHelper_.isOpen()) {
            dbHelper_.closeDatabase();
        }

        isRunning_.set(false);
        stopSelf();

    }

}
