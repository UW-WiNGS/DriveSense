package wisc.drivesense.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.R;
import wisc.drivesense.httpTools.TripUploadRequest;
import wisc.drivesense.utility.Trip;

public class HistoryActivity extends AppCompatActivity {
    // When requested, this adapter returns a DemoObjectFragment,
    // representing an object in the collection.
    MonthSearchPagerAdapter mMonthSearchPagerAdapter;
    ViewPager mViewPager;
    private static String TAG = "HistoryActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.history_toolbar);
        toolbar.setTitle("Trips");
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(toolbar);


        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mMonthSearchPagerAdapter =
                new MonthSearchPagerAdapter(
                        getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.history_search_view);
        mViewPager.setAdapter(mMonthSearchPagerAdapter);
        mViewPager.setCurrentItem(mMonthSearchPagerAdapter.getCount()-1, false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                //TODO: Actually do something with refresh action
                Log.d(TAG, "Refresh clicked");
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private static Calendar startOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); //set hours to zero
        cal.set(Calendar.MINUTE, 0); // set minutes to zero
        cal.set(Calendar.SECOND, 0); //set seconds to zero
        cal.add(Calendar.MILLISECOND,0);
        return cal;
    }

    private static long calToUnix(Calendar cal) {
        return cal.getTimeInMillis()+cal.get(Calendar.ZONE_OFFSET);
    }

    private static Calendar endOfMonth() {
        Calendar cal = startOfMonth();
        cal.add(Calendar.MONTH, 1);
        return cal;
    }

    static class MonthSearchPagerAdapter extends FragmentStatePagerAdapter {
        public MonthSearchPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new HistorySearchFragment();
            Bundle args = new Bundle();
            // Our object is just an integer :-P
            args.putInt(HistorySearchFragment.ARG_MONTH_OFFSET, (i-getCount()+1));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return 12;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int monthOffset = (position-getCount()+1);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, monthOffset);
            return cal.getDisplayName(Calendar.MONTH,Calendar.LONG, Locale.getDefault()) + " " + cal.get(Calendar.YEAR);
        }
    }

    public static class HistorySearchFragment extends Fragment {

        private final String TAG = "HistorySearchFragment";
        public static final String ARG_MONTH_OFFSET = "month";

        private ArrayAdapter<Trip> adapter_ = null;
        List<Trip> trips_ = null;

        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(
                    R.layout.history_search_fragment, container, false);
            Bundle args = getArguments();
            int monthsAgo = args.getInt(ARG_MONTH_OFFSET);
            Calendar start = startOfMonth();
            Calendar end = endOfMonth();
            start.add(Calendar.MONTH, monthsAgo);
            end.add(Calendar.MONTH, monthsAgo);

            ListView listView = (ListView)rootView.findViewById(R.id.listView);
            Log.d(TAG, "Start " +calToUnix(start));
            trips_ = DriveSenseApp.DBHelper().loadTrips("starttime >= "+calToUnix(start)+" and starttime < "+calToUnix(end) +" and status=2");
            adapter_ = new TripAdapter(this.getContext(), trips_);

            listView.setAdapter(adapter_);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, view.toString() + ";" + position + ";" + id);

                    Trip trip = adapter_.getItem(position);
                    Intent intent = new Intent(getContext(), TripViewActivity.class);
                    intent.putExtra("guid", trip.guid.toString());
                    startActivity(intent);
                }

            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
                    Log.d(TAG, view.toString() + ";" + position + ";" + id);
                    AlertDialog.Builder showPlace = new AlertDialog.Builder(getContext());
                    showPlace.setMessage("Remove this trip?");
                    showPlace.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int pos) {
                            Log.d(TAG, "delete:" + position);
                            Trip trip = adapter_.getItem(position);
                            DriveSenseApp.DBHelper().deleteTrip(trip.guid.toString());
                            adapter_.remove(trip);
                            TripUploadRequest.Start(view.getContext());
                        }
                    });
                    showPlace.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "cancel");
                        }
                    });
                    showPlace.show();
                    return true;
                }
            });
            return rootView;
        }
    }
}



