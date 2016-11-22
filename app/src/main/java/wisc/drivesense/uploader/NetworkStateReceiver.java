package wisc.drivesense.uploader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import wisc.drivesense.activity.MainActivity;

public class NetworkStateReceiver extends BroadcastReceiver {

    private static String TAG = "NetworkStateReceiver";
    private static Intent mUploaderIntent = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        //check internet connecction, and do uploading
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if(isConnected) {
            Log.d(TAG, "Internet is Connected!");
            if(!MainActivity.isServiceRunning(context, UploaderService.class)) {
                Log.d(TAG, "Start upload service!!!");
                mUploaderIntent = new Intent(context, UploaderService.class);
                context.startService(mUploaderIntent);
            }
        } else {
            Log.d(TAG, "Internet is Closed!");
            //end uploading
            if(MainActivity.isServiceRunning(context, UploaderService.class)) {
                Log.d(TAG, "Stop upload service!!!");
                context.stopService(mUploaderIntent);
                mUploaderIntent = null;
            }
        }
    }
}
