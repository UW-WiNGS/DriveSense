package wisc.drivesense.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import wisc.drivesense.R;
import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;

public class MapActivity extends Activity implements OnMapReadyCallback, GoogleMap.OnCameraChangeListener {

    static final LatLng madison_ = new LatLng(43.073052 , -89.401230);
    private GoogleMap map_ = null;
    private Trip trip_;
    private List<Trace> points_;
    private static String TAG = "MapActivity";
    private DatabaseHelper dbHelper_ = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d(TAG, "onCreate");

        Intent intent = getIntent();
        trip_ = (Trip) intent.getSerializableExtra("Current Trip");

        Toolbar ratingToolbar = (Toolbar) findViewById(R.id.tool_bar_rating);

        ratingToolbar.setTitle("Your Trip");
        ratingToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        ratingToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Gson gson = new Gson();
        Log.d(TAG, gson.toJson(trip_));
        if(trip_.getDistance() >= Constants.kTripMinimumDistance && trip_.getDuration() >= Constants.kTripMinimumDuration) {
            dbHelper_ = new DatabaseHelper();
            points_ = dbHelper_.getGPSPoints(trip_.getStartTime());
            trip_.setGPSPoints(points_);
            //crash when there is no gps
            Log.d(TAG, String.valueOf(points_.size()));

        }
        TextView ratingView = (TextView) findViewById(R.id.rating);
        ratingView.setText(String.format("%.1f", trip_.getScore()));

        map_ = null;
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void onCameraChange(CameraPosition position) {
        Log.d(TAG, String.valueOf(position.zoom));
        Log.d(TAG, position.target.toString());

        LatLngBounds bounds = map_.getProjection().getVisibleRegion().latLngBounds;
        Log.d(TAG, bounds.toString());
        //bounds.contains();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map_ = map;
        map_.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map_.setMyLocationEnabled(true);
        map_.setTrafficEnabled(true);
        map_.setIndoorEnabled(true);
        map_.setBuildingsEnabled(true);
        map_.getUiSettings().setZoomControlsEnabled(true);

        LatLng start;
        int sz = trip_.getGPSPoints().size();

        if(sz >= 2) {
            start = new LatLng(trip_.getStartPoint().values[0], trip_.getStartPoint().values[1]);
        } else {
            start = madison_;
        }
        CameraPosition position = CameraPosition.builder()
                .target(start)
                .zoom( 15f )
                .bearing( 0.0f )
                .tilt( 0.0f )
                .build();

        map_.moveCamera(CameraUpdateFactory.newCameraPosition(position));


        if(sz >= 2) {
            //deal with orientation change
            RadioButton rButton = (RadioButton) findViewById(R.id.radioButtonSpeed);
            rButton.setChecked(true);
            plotRoute();


            map.setOnCameraChangeListener(this);
        }
    }


    private List<BitmapDescriptor> producePoints(int [] colors) {
        List<BitmapDescriptor> res = new ArrayList<BitmapDescriptor>();
        int width = 10, height = 10;

        for(int i = 0; i < colors.length; ++i) {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint();
            paint.setColor(colors[i]);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(width / 2, height / 2, 5, paint);

            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bmp);
            res.add(bitmapDescriptor);
        }
        return res;
    }

    private int getButtonIndex() {
        int index = -1;
        RadioButton speedButton = (RadioButton) findViewById(R.id.radioButtonSpeed);
        RadioButton scoreButton = (RadioButton) findViewById(R.id.radioButtonScore);
        RadioButton brakeButton = (RadioButton) findViewById(R.id.radioButtonBrake);
        if(speedButton.isChecked()) {
            index = 2;
        } else if(scoreButton.isChecked()) {
            index = 3;
        } else if(brakeButton.isChecked()) {
            index = 4;
        } else {
            index = -1;
        }
        return index;
    }


    private void plotRoute() {
        int index = getButtonIndex();
        Log.d(TAG, "plot:" + String.valueOf(index));
        if(index < 2 || index > 4) {
            Log.e(TAG, "invalid input");
            return;
        }

        if(points_ == null || points_.size() <=2) {
            Log.e(TAG, "invalid GPS points");
            return;
        }
        //TODO: change it to display according to speed
        // remove zero points
        ListIterator<Trace> it = points_.listIterator();
        while (it.hasNext()) {
            Trace cur = it.next();
            if(cur.values[2] == 0.0) {
                it.remove();
            }
        }
        //
        int sz = points_.size();
        Log.d(TAG, "gps size after remove zeros" + sz);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        int [] colors = {Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED};
        List<BitmapDescriptor> bitmapDescriptors = producePoints(colors);

        // plot the route on the google map
        int rate = sz/1200 + 1;
        for (int i = 0; i < sz; i+=rate) {
            Trace point = points_.get(i);

            BitmapDescriptor bitmapDescriptor = null;

            if(index == 2) {
                //speed
                double speed = point.values[2];
                bitmapDescriptor = bitmapDescriptors.get(Math.min((int) (speed / 5.0), colors.length - 1));
            } else if(index == 3) {
                //score
                double score = point.values[3];
                bitmapDescriptor = bitmapDescriptors.get(Math.min((int)(10.0 - score), colors.length - 1));
            } else {
                //brake behaviors
                double brake = point.values[4];
                if(brake < 0) {
                    bitmapDescriptor = bitmapDescriptors.get(3);
                    //bitmapDescriptor =  BitmapDescriptorFactory.fromResource(R.drawable.attention_24);
                } else {
                    bitmapDescriptor = bitmapDescriptors.get(0);
                }
            }

            MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(point.values[0], point.values[1])).icon(bitmapDescriptor);
            Marker marker = map_.addMarker(markerOptions);
            builder.include(marker.getPosition());
        }

        // market the starting and ending points
        LatLng start = new LatLng(trip_.getStartPoint().values[0], trip_.getStartPoint().values[1]);
        MarkerOptions startOptions = new MarkerOptions().position(start).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_car));
        map_.addMarker(startOptions);
        LatLng end = new LatLng(trip_.getEndPoint().values[0], trip_.getEndPoint().values[1]);
        MarkerOptions endOptions = new MarkerOptions().position(end);
        map_.addMarker(endOptions);

        // zoom the map to cover the whole trip
        final LatLngBounds bounds = builder.build();
        map_.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            public void onMapLoaded() {
                int padding = 100;
                map_.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }
        });
    }



    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        if(checked == false) {
            return;
        }
        Log.d(TAG, view.getId() + " is checked: " + checked);
        // Check which radio button was clicked
        plotRoute();
        /*
        switch(view.getId()) {
            case R.id.radioButtonSpeed:
                plotRoute();
                break;
            case R.id.radioButtonScore:
                plotRoute();
                break;
            case R.id.radioButtonBrake:
                plotRoute();
                break;
            default:
                break;
        }
        */
    }


    protected void onDestroy() {
        if(dbHelper_ != null && dbHelper_.isOpen()) {
            dbHelper_.closeDatabase();
        }
        super.onDestroy();
    }
}
