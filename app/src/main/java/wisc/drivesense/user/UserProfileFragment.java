package wisc.drivesense.user;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import wisc.drivesense.DriveSenseApp;
import wisc.drivesense.R;

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
        DriveSenseToken user = DriveSenseApp.DBHelper().getCurrentUser();
        ((TextView)view.findViewById(R.id.username)).setText("Logged in as: " + user.email);
        ((TextView)view.findViewById(R.id.name)).setText(user.firstname + " " + user.lastname);

        view.findViewById(R.id.sign_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOutClicked(view);
            }
        });
    }

    public void signOutClicked(View view) {
        DriveSenseApp.DBHelper().userLogout();
        ((UserActivity)this.getActivity()).reland();
    }
}
