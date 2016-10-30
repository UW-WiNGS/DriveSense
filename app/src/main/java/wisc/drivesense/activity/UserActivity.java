package wisc.drivesense.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import wisc.drivesense.R;
import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.fragment.AuthLandingFragment;
import wisc.drivesense.fragment.UserProfileFragment;
import wisc.drivesense.httpPayloads.LoginPayload;
import wisc.drivesense.httpPayloads.TokenLoginPayload;
import wisc.drivesense.uploader.GsonRequest;
import wisc.drivesense.uploader.RequestQueueSingleton;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.DriveSenseToken;

public class UserActivity extends AppCompatActivity {
    private final String TAG = "UserActivity";
    public CallbackManager callbackManager;
    public GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final AppCompatActivity self = this;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user);

        initFacebook();
        initGoogle();

        reland();
    }

    public void reland() {
        DatabaseHelper dbH = new DatabaseHelper();
        if(dbH.getCurrentUser() != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_fragment_content, UserProfileFragment.newInstance())
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_fragment_content, AuthLandingFragment.newInstance())
                    .commit();
        }
        dbH.closeDatabase();

    }

    public void handleDrivesenseLogin(String driveSenseJWT) {
        DriveSenseToken dsToken = DriveSenseToken.InstantiateFromJWT(driveSenseJWT);
        DatabaseHelper dbH = new DatabaseHelper();
        if(dbH.hasUser(dsToken.email)) {
            dbH.userLogin(dsToken.email);
        } else {
            dbH.newUser(dsToken.email, dsToken.firstname, dsToken.lastname);
        }
        dbH.closeDatabase();

        this.reland();
    }

    private void initFacebook() {
        final AppCompatActivity self = this;
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
            new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    String fbtoken = loginResult.getAccessToken().getToken();
                    Log.d(TAG, "Got facebook token: "+fbtoken);
                    TokenLoginPayload tokenLogin = new TokenLoginPayload();
                    tokenLogin.access_token = fbtoken;
                    GsonRequest<LoginPayload> loginReq = new GsonRequest<LoginPayload>(Request.Method.POST, Constants.kFacebookSignInURL,
                            tokenLogin, LoginPayload.class,
                            new Response.Listener<LoginPayload>() {
                                @Override
                                public void onResponse(LoginPayload response) {
                                    // Display the first 500 characters of the response string.
                                    Log.d(TAG,"Got drivesense token: "+response.token);
                                    handleDrivesenseLogin(response.token);
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "Facebook token authentication with Drivesense Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                    RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue().add(loginReq);
                }

                @Override
                public void onCancel() {
                    // App code
                }

                @Override
                public void onError(FacebookException exception) {
                    Toast.makeText(self, "Error during facebook login.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void initGoogle(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_server_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.e("ERROR:", "Connection to Google failed");
                        Toast.makeText(getApplicationContext(),
                                "Please check your Internet connection",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    public void handleGoogleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "Google sign in result:" + result.isSuccess() + result.toString());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            Log.d(TAG, "Got google auth token: " + acct.getIdToken());
            final Context ctx = this;
            TokenLoginPayload tokenLogin = new TokenLoginPayload();
            tokenLogin.id_token = acct.getIdToken();
            GsonRequest<LoginPayload> loginReq = new GsonRequest<LoginPayload>(Request.Method.POST, Constants.kGoogleSignInURL,
                    tokenLogin, LoginPayload.class,
                    new Response.Listener<LoginPayload>() {
                        @Override
                        public void onResponse(LoginPayload response) {
                            Log.d(TAG,"Got drivesense token: "+response.token);
                            handleDrivesenseLogin(response.token);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ctx, R.string.login_failed, Toast.LENGTH_SHORT).show();
                }
            });
            // Add the request to the RequestQueue.
            RequestQueueSingleton.getInstance(this).getRequestQueue().add(loginReq);
        } else {
            // Signed out, show unauthenticated UI.

        }
    }
}
