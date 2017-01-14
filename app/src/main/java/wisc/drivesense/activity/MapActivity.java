package wisc.drivesense.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
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

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.R;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;

public class MapActivity extends Activity implements OnMapReadyCallback {

    static final LatLng madison_ = new LatLng(43.073052, -89.401230);
    private GoogleMap map_ = null;
    private Trip trip_;
    private List<Trace.Trip> points_;
    private static String TAG = "MapActivity";
    private RadioButton speedButton;
    private RadioButton scoreButton;
    private RadioButton brakeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d(TAG, "onCreate");

        speedButton = (RadioButton) findViewById(R.id.radioButtonSpeed);
        scoreButton = (RadioButton) findViewById(R.id.radioButtonScore);
        brakeButton = (RadioButton) findViewById(R.id.radioButtonBrake);

        Intent intent = getIntent();
        String uuid = intent.getStringExtra("uuid");
        trip_ = DriveSenseApp.DBHelper().getTrip(uuid);

        if(trip_ == null) {
            finish();
            return;
        }

        Toolbar ratingToolbar = (Toolbar) findViewById(R.id.tool_bar_rating);

        ratingToolbar.setTitle("Your Trip");
        ratingToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        ratingToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        points_ = DriveSenseApp.DBHelper().getGPSPoints(trip_.uuid.toString());
        trip_.setGPSPoints(points_);
        //crash when there is no gps
        Log.d(TAG, String.valueOf(points_.size()));

        TextView ratingView = (TextView) findViewById(R.id.rating);
        ratingView.setText(String.format("%.1f", trip_.getScore()));

        map_ = null;
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map_ = map;
        map_.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        else
            map_.setMyLocationEnabled(true);
        map_.setTrafficEnabled(true);
        map_.setIndoorEnabled(true);
        map_.setBuildingsEnabled(true);
        map_.getUiSettings().setZoomControlsEnabled(true);

        if(trip_ == null) {
            return;
        }

        LatLng start;
        int sz = trip_.getGPSPoints().size();

        if(sz >= 2) {
            start = trip_.getStartPoint();
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
            plotRoute();
        }
    }


    public static List<BitmapDescriptor> producePoints(int [] colors) {
        List<BitmapDescriptor> res = new ArrayList<BitmapDescriptor>();
        int width = 10, height = 10;

        for (int color : colors) {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(width / 2, height / 2, 5, paint);

            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bmp);
            res.add(bitmapDescriptor);
        }
        return res;
    }

    private int getButtonIndex() {
        int index;

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
        //ListIterator<Trace.Trip> it = points_.listIterator();
        //while (it.hasNext()) {
        //    Trace.Trip cur = it.next();
        //    if(cur.speed == 0.0) {
        //        it.remove();
        //    }
        //}

        int sz = points_.size();
        Log.d(TAG, "gps size after remove zeros" + sz);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        int [] colors = {Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED};
        List<BitmapDescriptor> bitmapDescriptors = producePoints(colors);

        // plot the route on the google map
        double distance = trip_.getDistance();
        double step = distance/1000;
        Trace.Trip lastgps = null;
        for (int i = 0; i < sz; i++) {
            Trace.Trip point = points_.get(i);
            if(lastgps == null) {
                lastgps = point;
            } else {
                if(Trip.distance((Trace.GPS)lastgps, (Trace.GPS)point) < step) {
                    continue;
                }
                lastgps = point;
            }

            BitmapDescriptor bitmapDescriptor;

            if(index == 2) {
                //speed
                bitmapDescriptor = bitmapDescriptors.get(Math.min((int) (point.speed / 5.0), colors.length - 1));
            } else if(index == 3) {
                //score
                bitmapDescriptor = bitmapDescriptors.get(Math.min((int)(10.0 - point.score), colors.length - 1));
            } else {
                //brake behaviors
                double brake = point.brake;
                if(brake < 0) {
                    bitmapDescriptor = bitmapDescriptors.get(3);
                    //bitmapDescriptor =  BitmapDescriptorFactory.fromResource(R.drawable.attention_24);
                } else {
                    bitmapDescriptor = bitmapDescriptors.get(0);
                }
            }

            MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(point.lat, point.lng)).icon(bitmapDescriptor);
            Marker marker = map_.addMarker(markerOptions);
            builder.include(marker.getPosition());
        }

        // market the starting and ending points
        LatLng start = trip_.getStartPoint();
        MarkerOptions startOptions = new MarkerOptions().position(start).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_car));
        map_.addMarker(startOptions);
        LatLng end = trip_.getEndPoint();
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
    }
}
