package wisc.drivesense.activity.history;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import wisc.drivesense.R;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.Units;

/**
 * Created by peter on 3/8/17.
 */

public class SingleTripStatsFragment extends Fragment {

    @BindView(R.id.duration_display) public TextView tvDuration;
    @BindView(R.id.distance_display) public TextView tvDistance;

    private boolean metricUnits;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_single_trip_stats, container);
        metricUnits = SettingActivity.getMetricUnits(getContext());
        updateFields();
        return rootView;
    }

    private Trip getParentTrip() {
        return ((TripViewActivity)getContext()).trip_;
    }

    private void updateFields() {
        Trip trip = getParentTrip();
        if(trip != null) {
            long duration = trip.getDuration();
            tvDuration.setText(Units.displayTimeInterval(duration));
            Units.userFacingDouble distance = Units.largeDistance(getParentTrip().getDistance(), metricUnits);
            tvDistance.setText(String.format("%.2f", distance.value) + " " + distance.unitName);
        }
    }
}
