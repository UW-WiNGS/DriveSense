package wisc.drivesense.user;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import wisc.drivesense.R;
import wisc.drivesense.httpPayloads.SignupPayload;
import wisc.drivesense.httpPayloads.GsonRequest;
import wisc.drivesense.uploader.RequestQueueSingleton;
import wisc.drivesense.utility.Constants;

public class SignupFragment extends Fragment {
    private final String TAG = "SignupFragment";
    EditText mEmailText;
    EditText mPasswordText;
    EditText mPasswordRepeatText;
    EditText mFirstnameText;
    EditText mLastnameText;

    public static SignupFragment newInstance() {
        return new SignupFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mEmailText = ((EditText)view.findViewById(R.id.email));
        mPasswordText = ((EditText)view.findViewById(R.id.password));
        mPasswordRepeatText = ((EditText)view.findViewById(R.id.password_repeat));
        mFirstnameText = ((EditText)view.findViewById(R.id.firstname));
        mLastnameText = ((EditText)view.findViewById(R.id.lastname));
        view.findViewById(R.id.sign_up_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpClicked(view);
            }
        });
    }

    private void signUpClicked(View view){
        EditText[] fields = {mEmailText, mPasswordText, mPasswordRepeatText, mFirstnameText, mLastnameText};

        View errorField = null;
        // Reset errors.
        for (EditText field: fields) {
            field.setError(null);
            if(TextUtils.isEmpty(field.getText().toString())) {
                field.setError(getString(R.string.error_field_required));
                errorField = field;
            }
        }

        if(!mPasswordRepeatText.getText().toString().equals(mPasswordText.getText().toString())) {
            mPasswordRepeatText.setError(getString(R.string.error_mismatch_password));
            errorField = mPasswordRepeatText;
        }

        //if a field had an error, focus on it and do not submit the form
        if (errorField != null) {
            errorField.requestFocus();
        } else {
            //no errors, so submit the signup
            String email = mEmailText.getText().toString();
            String password = mPasswordText.getText().toString();
            String firstname = mFirstnameText.getText().toString();
            String lastname = mLastnameText.getText().toString();

            attemptSignup(email, password, firstname, lastname);
        }
    }

    public void attemptSignup(String email, String password, String firstname, String lastname) {
        final Fragment self = this;

        SignupPayload signup = new SignupPayload();
        signup.email = email;
        signup.password = password;
        signup.firstname = firstname;
        signup.lastname = lastname;

        GsonRequest<SignupPayload> loginReq = new GsonRequest<SignupPayload>(Request.Method.POST, Constants.kSignUpURL,
                signup, SignupPayload.class) {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(self.getContext(), R.string.sign_up_error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(SignupPayload response) {
                // Display the first 500 characters of the response string.
                Log.d(TAG,"Got a login token: " + response.token);
                ((UserActivity)self.getActivity()).handleDrivesenseLogin(response.token);
            }
        };
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this.getContext()).getRequestQueue().add(loginReq);
    }

}
