package wisc.drivesense.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import wisc.drivesense.R;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.Units;

public class TripAdapter extends ArrayAdapter<Trip> {
    List<Trip> trips_ = null;
    private boolean metricUnits;

    private final String TAG = "TripAdapter";
    public TripAdapter(Context context, List<Trip> trips) {
        super(context, 0, trips);
        trips_ = trips;
        metricUnits = SettingActivity.getMetricUnits(context);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Trip trip = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.trip_item, parent, false);
        }
        // Lookup view for data population
        TextView tvStartDate = (TextView) convertView.findViewById(R.id.start_date);
        TextView tvStartTime = (TextView) convertView.findViewById(R.id.start_time);
        TextView tvDuration = (TextView) convertView.findViewById(R.id.duration);
        TextView tvDistance = (TextView) convertView.findViewById(R.id.distance_driven_unit);

        long start = trip.getStartTime();
        Date starting = new Date(start);

        long end = trip.getEndTime();
        Date ending = new Date(end);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMMM d");
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");

        long duration = trip.getDuration();
        double score = trip.getScore();

        tvStartDate.setText(dateFormat.format(starting));
        tvStartTime.setText(timeFormat.format(starting));
        tvDuration.setText(Units.displayTimeInterval(duration));
        Units.userFacingDouble distance = Units.largeDistance(trip.getDistance(), metricUnits);
        tvDistance.setText(String.format("%.2f", distance.value) + " " + distance.unitName);


        return convertView;
    }

}