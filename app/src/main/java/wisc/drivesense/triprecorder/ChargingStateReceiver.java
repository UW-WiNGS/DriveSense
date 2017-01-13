package wisc.drivesense.triprecorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import wisc.drivesense.activity.MainActivity;
import wisc.drivesense.activity.SettingActivity;

public class ChargingStateReceiver extends BroadcastReceiver {

    private static String TAG = "ChargingStateReceiver";
    private static Intent mDrivingDetectionIntent = null;


    @Override
    public void onReceive(final Context context, Intent intent) {

        if(SettingActivity.isAutoMode(context) == false) {
            return;
        }

        //check charging status, and start sensor service automatically
        String action = intent.getAction();
        mDrivingDetectionIntent = new Intent(context, TripService.class);
        mDrivingDetectionIntent.putExtra(TripService.START_IMMEDIATELY, true);

        switch (action) {
            case Intent.ACTION_POWER_CONNECTED:
                // Do something when power connected
                Log.d(TAG, "Plugged. Start driving detection service!!!");
                context.startService(mDrivingDetectionIntent);
                break;
            //power disconnect is received in the TripService
        }
    }



}
