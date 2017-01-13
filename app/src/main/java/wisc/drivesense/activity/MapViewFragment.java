package wisc.drivesense.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.LinkedList;
import java.util.List;

import wisc.drivesense.R;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;

public class MapViewFragment extends Fragment {
    private MapView mMapView;
    private GoogleMap googleMap;
    private List<BitmapDescriptor> bitmapDescriptors;
    private List<Marker> markers = new LinkedList<Marker>();
    private int [] colors = {Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED};

    public static MapViewFragment newInstance() {
        return new MapViewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }


        bitmapDescriptors = MapActivity.producePoints(colors);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                try {
                    googleMap.setMyLocationEnabled(true);
                } catch (SecurityException se) {
                    Log.e("ERROR:", se.getMessage());
                }
                // For dropping a marker at a point on the Map
                LatLng madison = new LatLng(43.0731, -89.4012);
                //googleMap.addMarker(new MarkerOptions().position(madison).title("Marker Title").snippet("Marker Description"));

                // For zooming automatically to the location of the marker
                //CameraPosition cameraPosition = new CameraPosition.Builder().target(madison).zoom(12).build();
                //googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
        return rootView;
    }

    private Trace.GPS lastgps = null;
    public void addMarker(Trace gps) {
        Trace.GPS curgps = (Trace.GPS)gps;
        if(lastgps == null) {
            lastgps = curgps;
        } else {
            if(Trip.distance(lastgps, curgps) < 5) {
                return;
            } else {
                lastgps = curgps;
            }
        }

        BitmapDescriptor bitmapDescriptor = bitmapDescriptors.get(0);
        bitmapDescriptor = bitmapDescriptors.get(Math.min((int) (curgps.speed / 5.0), colors.length - 1));
        MarkerOptions markerOptions = new MarkerOptions().position(curgps.toLatLng()).icon(bitmapDescriptor);
        Marker curMarker = googleMap.addMarker(markerOptions);
        markers.add(curMarker);
        if(markers.size() > Constants.kNumberOfMarkerDisplay) {
            markers.remove(0).remove();
        }

        // For zooming automatically to the location of the marker
        CameraPosition cameraPosition = new CameraPosition.Builder().target(curgps.toLatLng()).zoom(16).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    public void addNMarkers(Trip trip) {
        if(trip == null) {
            return;
        }
        List<Trace.GPS> gps = trip.getGPSPoints();
        int sz = gps.size();
        if(sz > Constants.kNumberOfMarkerDisplay * 2) {
            gps = gps.subList(sz - Constants.kNumberOfMarkerDisplay * 2, sz);
        }
        for(int i = 0; i < gps.size(); ++i) {
            addMarker(gps.get(i));
        }
    }
}