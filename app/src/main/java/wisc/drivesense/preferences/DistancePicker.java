package wisc.drivesense.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import wisc.drivesense.R;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Units;

/**
 * Created by peter on 3/23/17.
 */

public class DistancePicker extends DialogPreference {
    private int mCurrentValueMeters = Constants.DEFAULT_MINIMUM_TRIP_DIST_METERS;
    @BindView(R.id.minimum_distance) public EditText distanceText;
    @BindView(R.id.units) public TextView unitTv;
    boolean useMetric;

    public DistancePicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.distance_picker_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);

        useMetric = SettingActivity.getMetricUnits(context);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            mCurrentValueMeters = Double.valueOf( distanceText.getText().toString()).intValue();
            if(!useMetric) { mCurrentValueMeters /= Constants.kFeetPerMeter; }
            persistInt(mCurrentValueMeters);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        ButterKnife.bind(this, view);

        Units.userFacingDouble dist = Units.smallDistance(mCurrentValueMeters, useMetric);
        distanceText.setText(String.valueOf((int)dist.value));
        unitTv.setText(dist.unitName);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValueMeters = this.getPersistedInt(Constants.DEFAULT_MINIMUM_TRIP_DIST_METERS);

        } else {
            // Set default state from the XML attribute
            mCurrentValueMeters = Constants.DEFAULT_MINIMUM_TRIP_DIST_METERS;
        }
    }
}

