package wisc.drivesense.activity.history;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
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
import wisc.drivesense.R;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;

/**
 * Created by peter on 3/8/17.
 */

public class SingleTripMapFragment extends Fragment implements OnMapReadyCallback {
    private static String TAG = "SingleTripMapFragment";
    @BindView(R.id.radioButtonSpeed) public RadioButton speedButton;
    @BindView(R.id.radioButtonBrake) public RadioButton brakeButton;
    @BindView(R.id.mapLoadingSpinner) public ProgressBar pbLoadingSpinner;
    private static final LatLng madison_ = new LatLng(43.073052, -89.401230);

    private GoogleMap map_ = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        map_ = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_single_trip_map, container);
        brakeButton.setOnClickListener(radioClickListener);
        speedButton.setOnClickListener(radioClickListener);
        SupportMapFragment mapFragment = (SupportMapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return rootView;
    }

    private Trip getParentTrip() {
        return ((TripViewActivity)getContext()).trip_;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map_ = map;
        map_.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        CameraPosition position = CameraPosition.builder()
                .target(madison_)
                .zoom( 15f )
                .bearing( 0.0f )
                .tilt( 0.0f )
                .build();

        map_.moveCamera(CameraUpdateFactory.newCameraPosition(position));

        if(getParentTrip().getGPSPoints() != null && getParentTrip().getGPSPoints().size() != 0) {
            populateMap();
        }
    }

    public void populateMap() {
        if(getParentTrip().getGPSPoints() == null || map_ == null)
            return;

        pbLoadingSpinner.setVisibility(View.INVISIBLE);

        LatLng start;
        int sz = getParentTrip().getGPSPoints().size();

        if(sz >= 2) {
            start = getParentTrip().getStartPoint();
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

        if(getParentTrip().getGPSPoints() == null || getParentTrip().getGPSPoints().size() <=2) {
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

        int sz = getParentTrip().getGPSPoints().size();
        Log.d(TAG, "gps size after remove zeros" + sz);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        int [] colors = {Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED};
        List<BitmapDescriptor> bitmapDescriptors = producePoints(colors);

        // plot the route on the google map
        double distance = getParentTrip().getDistance();
        double step = distance/1000;
        Trace.Trip lastgps = null;
        for (int i = 0; i < sz; i++) {
            Trace.Trip point = getParentTrip().getGPSPoints().get(i);
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
        LatLng start = getParentTrip().getStartPoint();
        MarkerOptions startOptions = new MarkerOptions().position(start).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        map_.addMarker(startOptions);
        LatLng end = getParentTrip().getEndPoint();
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


    private View.OnClickListener radioClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            onRadioButtonClicked(view);
        }
    };
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