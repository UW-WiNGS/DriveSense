package wisc.drivesense.triprecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import wisc.drivesense.activity.MainActivity;
import wisc.drivesense.activity.SettingActivity;

public class ChargingStateReceiver extends BroadcastReceiver {

    private static String TAG = "ChargingStateReceiver";
    private static Intent mDrivingDetectionIntent = null;
    @Override
    public void onReceive(Context context, Intent intent) {

        if(SettingActivity.isAutoMode(context) == false) {
            return;
        }

        //check charging status, and start sensor service automatically
        String action = intent.getAction();
        mDrivingDetectionIntent = new Intent(context, TripService.class);

        switch (action) {
            case Intent.ACTION_POWER_CONNECTED:
                // Do something when power connected
                Log.d(TAG, "Plugged. Start driving detection service!!!");
                context.startService(mDrivingDetectionIntent);
                //TODO: bind to service and start recording
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                // Do something when power disconnected
                Log.d(TAG, "Unplugged. Stop driving detection service!!!");
                //TODO: bind and stop recording
                context.stopService(mDrivingDetectionIntent);
                break;
        }
    }



}
