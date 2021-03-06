package wisc.drivesense.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;

import wisc.drivesense.R;
import wisc.drivesense.utility.Constants;

public class SettingActivity extends AppCompatActivity {


    private String TAG = "SettingActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

    public static boolean getAutoStart(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("pref_auto_start", context.getResources().getBoolean(R.bool.auto_start_default));
    }

    public static boolean getAutoStop(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("pref_auto_stop", context.getResources().getBoolean(R.bool.auto_stop_default));
    }

    public static boolean getEndTripAuto(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("end_trip_inactivity", context.getResources().getBoolean(R.bool.default_end_trip_inactivity));
    }

    public static boolean showMapWhileDriving(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("showMap", false);
    }

    public static boolean getPauseWhenStationary(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("pause_when_stationary", context.getResources().getBoolean(R.bool.default_pause_stationary));
    }

    public static int getMinimumDistance(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        //minimum distance is in meters
        int minDistance = sharedPref.getInt("minimum_distance_int", Constants.DEFAULT_MINIMUM_TRIP_DIST_METERS);
        return minDistance;
    }

    public static boolean getMetricUnits(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean metric = sharedPref.getString("unit_type", "2").equals("1");
        return metric;
    }

    /**
     *
     * @param context
     * @return true if non-critical data should be restricted to WiFi
     */
    public static boolean getWifiOnly(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("wifi_only", true);
    }

    public static class SettingsFragment extends PreferenceFragment {
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);


            EditTextPreference idPref = (EditTextPreference)findPreference("pre_id");
            String androidid = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
            idPref.setTitle("Device ID: " + androidid);

        }
    }
}
