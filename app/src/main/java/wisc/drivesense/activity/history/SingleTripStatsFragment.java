package wisc.drivesense.activity.history;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import wisc.drivesense.R;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.Units;

/**
 * Created by peter on 3/8/17.
 */

public class SingleTripStatsFragment extends Fragment {

    @BindView(R.id.duration_display) public TextView tvDuration;
    @BindView(R.id.distance_display) public TextView tvDistance;
    @BindView(R.id.top_speed_display) public TextView tvTopSpeed;
    @BindView(R.id.average_speed_display) public TextView tvAverageSpeed;

    private boolean metricUnits;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_single_trip_stats, container, false);
        ButterKnife.bind(this,rootView);
        metricUnits = SettingActivity.getMetricUnits(getContext());
        updateFields();
        return rootView;
    }

    private Trip getParentTrip() {
        TripViewActivity t = ((TripViewActivity)getContext());
        if(t!= null)
            return t.trip_;
        return null;
    }

    public void updateFields() {
        Trip trip = getParentTrip();
        if(trip != null) {
            long duration = trip.getDuration();
            tvDuration.setText(Units.displayTimeInterval(duration));
            double distance = getParentTrip().getDistance();
            Units.userFacingDouble distDisplay = Units.largeDistance(distance, metricUnits);
            tvDistance.setText(String.format("%.2f", distDisplay.value) + " " + distDisplay.unitName);

            double averageSpeed = distance / duration * 1000;
            Units.userFacingDouble averageSpeedDisplay = Units.speed(averageSpeed, metricUnits);
            tvAverageSpeed.setText(String.format("%.1f", averageSpeedDisplay.value) + " " + averageSpeedDisplay.unitName);

            double topSpeed = 0;
            List<Trace.Trip> gps = getParentTrip().getGPSPoints();
            if(gps != null) {
                for (Trace.Trip t: gps) {
                    if(t.speed > topSpeed)
                        topSpeed=t.speed;
                }
            }
            Units.userFacingDouble topSpeedDisplay = Units.speed(topSpeed, metricUnits);
            tvTopSpeed.setText(String.format("%.1f", topSpeedDisplay.value) + " " + topSpeedDisplay.unitName);
        }
    }
}
