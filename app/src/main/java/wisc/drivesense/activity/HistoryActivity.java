package wisc.drivesense.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;

import java.util.List;

import wisc.drivesense.R;
import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.utility.Trip;

public class HistoryActivity extends Activity {

    private final String TAG = "HistoryActivity";

    private DatabaseHelper dbHelper_ = null;

    private ArrayAdapter<Trip> adapter_ = null;
    List<Trip> trips_ = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.historytoolbar);
        toolbar.setTitle("Previous Trips");
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ListView listView = (ListView)findViewById(R.id.listView);

        dbHelper_ = new DatabaseHelper();
        trips_ = dbHelper_.loadTrips();
        adapter_ = new TripAdapter(this, trips_);

        listView.setAdapter(adapter_);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, view.toString() + ";" + position + ";" + id);

                Trip trip = adapter_.getItem(position);
                Intent intent = new Intent(HistoryActivity.this, MapActivity.class);
                intent.putExtra("Current Trip", trip);
                startActivity(intent);
            }

        });


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.d(TAG, view.toString() + ";" + position + ";" + id);
                AlertDialog.Builder showPlace = new AlertDialog.Builder(HistoryActivity.this);
                showPlace.setMessage("Remove this trip?");
                showPlace.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int pos) {
                        Log.d(TAG, "delete:" + position);
                        Trip trip = adapter_.getItem(position);
                        dbHelper_.removeTrip(trip.getStartTime());
                        adapter_.remove(trip);
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
    }

    protected void onDestroy() {
        if(dbHelper_ != null && dbHelper_.isOpen()) {
            dbHelper_.closeDatabase();
        }
        super.onDestroy();
    }
}
