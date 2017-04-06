package wisc.drivesense.activity.history;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.R;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.Units;

public class TripViewActivity extends AppCompatActivity {
    protected Trip trip_;
    private static String TAG = "TripViewActivity";
    private SingleTripMapFragment mapFrag;
    private SingleTripStatsFragment statsFrag;
    @BindView(R.id.single_trip_tabs) public TabLayout tripTabs;
    @BindView(R.id.single_trip_tab_pager) public ViewPager singleTripTabPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_trip_view);

        ButterKnife.bind(this);

        Log.d(TAG, "onCreate");

        mapFrag = new SingleTripMapFragment();
        statsFrag = new SingleTripStatsFragment();

        singleTripTabPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager()));
        tripTabs.setupWithViewPager(singleTripTabPager);

        Intent intent = getIntent();
        String uuid = intent.getStringExtra("guid");
        trip_ = DriveSenseApp.DBHelper().getTrip(uuid);

        if(trip_ == null) {
            finish();
            return;
        }

        Toolbar tripViewToolbar = (Toolbar) findViewById(R.id.tool_bar_rating);

        tripViewToolbar.setTitle("Your Trip");
        tripViewToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        tripViewToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        new AsyncTripLoader().execute(trip_.guid);
    }

    private class AsyncTripLoader extends AsyncTask<String, Void, List<Trace.Trip>> {
        protected List<Trace.Trip> doInBackground(String ... uuids) {
            int count = uuids.length;
            if(count!=1)
                return null;

            String uuid = uuids[0];
            List<Trace.Trip> points = DriveSenseApp.DBHelper().getGPSPoints(uuid);
            return points;
        }
        protected void onPostExecute(List<Trace.Trip> result) {
            trip_.setGPSPoints(result);
            if(statsFrag != null) {
                statsFrag.updateFields();
            }
            if(mapFrag!= null)
            {
                mapFrag.setTrip(trip_);
                mapFrag.populateMap();
            }

        }
    }

    private class TabPagerAdapter extends FragmentPagerAdapter {

        public TabPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Map";
                default:
                    return "Stats";
            }
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return mapFrag;
                default:
                    return statsFrag;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
