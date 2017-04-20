package wisc.drivesense;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.splunk.mint.Mint;

import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.httpTools.TripMetadataDownloadRequest;
import wisc.drivesense.httpTools.TripUploadRequest;

/**
 * Created by Alex Sherman on 11/23/2016.
 */

/**
 * run once when the app launch, the first run before all other applications
 * this is where singleton class they are created
 */
public class DriveSenseApp extends Application {
    private static Context context;
    private static RequestQueue requestQueue = null;
    private static DatabaseHelper dbHelper = null;

    @Override
    public void onCreate() {
        super.onCreate();
        //the mint is used when teh user's app is crashed , the stack will send to us emails and
        // it will be stored online
        // accessed by : https://mint.splunk.com/account/team/5400007893188608
        Mint.initAndStartSession(this, "28d86904");
        context = getApplicationContext();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                // Mark all trips as finalized if the app is just starting,
                // catches cases like the app crashed during a trip
                DBHelper().finalizeLiveTrips();

                // Attempt to upload any unsent trips
                TripUploadRequest.Start(context);
                TripMetadataDownloadRequest update = new TripMetadataDownloadRequest(DBHelper().getCurrentUser());
                RequestQueue().add(update);
                return null;
            }
        }.execute();

    }

    public static synchronized RequestQueue RequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context);
        }
        return requestQueue;
    }
    public static synchronized DatabaseHelper DBHelper() {
        if(dbHelper == null)
            dbHelper = new DatabaseHelper(context);
        return dbHelper;
    }
}
