package wisc.drivesense.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import wisc.drivesense.R;
import wisc.drivesense.activity.UserActivity;
import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.utility.DriveSenseToken;
import wisc.drivesense.utility.User;

/**
 * Created by peter on 10/29/16.
 */

public class UserProfileFragment extends Fragment {
    public static UserProfileFragment newInstance() {
        return new UserProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        DatabaseHelper dbH = new DatabaseHelper();
        User user = dbH.getCurrentUser();
        ((TextView)view.findViewById(R.id.username)).setText("Logged in as: " + user.firstname_ + " " + user.lastname_);
        dbH.closeDatabase();

        view.findViewById(R.id.sign_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOutClicked(view);
            }
        });
    }

    public void signOutClicked(View view) {
        DatabaseHelper dbH = new DatabaseHelper();
        dbH.userLogout();
        dbH.closeDatabase();
        ((UserActivity)this.getActivity()).reland();
    }
}
