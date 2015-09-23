package technolyst360.googlesignin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;



/**
 * Created by Prashant on 23-07-2015.
 */
public class GPlus implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {


    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;

    private static final int RC_SIGN_IN = 0;

    private static final String DIALOG_ERROR = "dialog_error";
    private static final String SAVED_PROGRESS = "sign_in_progress";


    private FragmentActivity activity;
    private GoogleApiClient mGoogleApiClient;
    private SignInButton buttonRef;
    private boolean mResolvingError = false;
    private int mSignInProgress;
    private PendingIntent mSignInIntent;
    private int mSignInError;
    private Fragment fragment;

    private GPlusLoginListener gPlusLoginListener;
    private boolean isConnected = false;

    public Person getGPerson() {
        return mGPerson;
    }

    private Person mGPerson;

    public GPlus(FragmentActivity activity) {
        this.activity = activity;
    }

    public GPlus onCreate(Bundle savedInstanceState) {


        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState
                    .getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }




        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addScope(Plus.SCOPE_PLUS_LOGIN);
        ;

        mGoogleApiClient = builder.build();
        return this;
    }


    public void onCreateView(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState
                    .getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }

    }

    public void onStart() {
        mGoogleApiClient.connect();
    }

    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == Activity.RESULT_OK) {
                    // If the error resolution was successful we should continue
                    // processing errors.
                    mSignInProgress = STATE_SIGN_IN;
                } else {
                    // If the error resolution was not successful or the user canceled,
                    // we should stop processing errors.
                    mSignInProgress = STATE_DEFAULT;
                }

                if (!mGoogleApiClient.isConnecting()) {
                    // If Google Play services resolved the issue with a dialog then
                    // onStart is not called so we need to re-attempt connection here.
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    public GPlus setSignInRef(SignInButton buttonReference, GPlusLoginListener gPlusLoginListener) {
        this.buttonRef = buttonReference;
        if(buttonRef !=null)
        this.buttonRef.setOnClickListener(this);
        setGPlusLoginListener(gPlusLoginListener);
        return this;
    }

    @Override
    public void onConnected(Bundle bundle) {


        isConnected = true;
       Log.d(getClass().getName(), "onConnected");
       if( sendCurrentLoginPerson() != null){
           if(buttonRef !=null)
           buttonRef.setEnabled(false);

       }
        // Indicate that the sign in process is complete.
        mSignInProgress = STATE_DEFAULT;
    }

    private Person sendCurrentLoginPerson() {

         mGPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        if (mGPerson != null  && getGPlusLoginListener() != null) {
            getGPlusLoginListener().onSuccess(mGPerson);
        }else if(getGPlusLoginListener() != null){

            getGPlusLoginListener().onError();
        }

        return mGPerson;

    }

    @Override
    public void onConnectionSuspended(int i) {

        onError();
        if(buttonRef !=null)
        buttonRef.setEnabled(true);

        Log.d(getClass().getSimpleName(), "onConnectionSuspended GPlus");
        mGoogleApiClient.connect();



    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(getClass().getName(), "onConnectionFailed GPlus");
        if(buttonRef !=null)
        buttonRef.setEnabled(true);

        // Refer to the javadoc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(getClass().getName(), "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());

        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // An API requested for GoogleApiClient is not available. The device's current
            // configuration might not be supported with the requested API or a required component
            // may not be installed, such as the Android Wear application. You may need to use a
            // second GoogleApiClient to manage the application's optional APIs.
            Log.d(getClass().getName(), "API Unavailable.");
        } else if (mSignInProgress != STATE_IN_PROGRESS) {
            // We do not have an intent in progress so we should store the latest
            // error resolution intent for use when the sign in button is clicked.
            mSignInIntent = result.getResolution();
            mSignInError = result.getErrorCode();

            if (mSignInProgress == STATE_SIGN_IN) {
                // STATE_SIGN_IN indicates the user already clicked the sign in button
                // so we should continue processing errors until the user is signed in
                // or they click cancel.
                resolveSignInError();
            }
        }

      onError();

    }

    private void onError() {
        if(getGPlusLoginListener() != null){
            getGPlusLoginListener().onError();
        }

    }


    private void resolveSignInError() {
        if (mSignInIntent != null) {
            // We have an intent which will allow our user to sign in or
            // resolve an error.  For example if the user needs to
            // select an account to sign in with, or if they need to consent
            // to the permissions your app is requesting.

            try {
                // Send the pending intent that we stored on the most recent
                // OnConnectionFailed callback.  This will allow the user to
                // resolve the error currently preventing our connection to
                // Google Play services.
                mSignInProgress = STATE_IN_PROGRESS;

                activity.startIntentSenderForResult(mSignInIntent.getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);

            } catch (IntentSender.SendIntentException e) {
                Log.d(getClass().getName(), "Sign in intent could not be sent: "
                        + e.getLocalizedMessage());
                // The intent was canceled before it was sent.  Attempt to connect to
                // get an updated ConnectionResult.
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
            }
        } else {
            // Google Play services wasn't able to provide an intent for some
            // error types, so we show the default Google Play services error
            // dialog which may still start an intent on our behalf if the
            // user can resolve the issue.
            createErrorDialog().show();
        }
    }


    private Dialog createErrorDialog() {
        if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
            return GooglePlayServicesUtil.getErrorDialog(
                    mSignInError,
                    activity,
                    RC_SIGN_IN,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Log.d(getClass().getName(), "Google Play services resolution cancelled");
                            mSignInProgress = STATE_DEFAULT;
                            Log.d(getClass().getName(), "G+Logout");
                        }
                    });
        } else {
            return new AlertDialog.Builder(activity)
                    .setMessage(R.string.play_services_error)
                    .setPositiveButton(R.string.close,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(getClass().getName(), "Google Play services error could not be "
                                            + "resolved: " + mSignInError);
                                    mSignInProgress = STATE_DEFAULT;
                                    Log.d(getClass().getName(), "G+Logout");
                                }
                            }).create();
        }
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }


    public void onDestroy() {

    }


    public void onResume() {

    }


    @Override
    public void onClick(View v) {
        try {

            if (!mGoogleApiClient.isConnecting()) {

                if (buttonRef == v) {
                    onEvent_login();
                }

            }
        } catch (Exception exp) {
            Log.e(getClass().getName(), "Handled Exception ", exp);
        }
    }

    private void onEvent_login() {

        if(!mGoogleApiClient.isConnected()) {
            if (getGPlusLoginListener() != null) {
                getGPlusLoginListener().onLoginButtonClick();
            }
            mSignInProgress = STATE_SIGN_IN;
            mGoogleApiClient.connect();
        }else{
            sendCurrentLoginPerson();
        }

    }


    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        dialogFragment.setGPlus(this);
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(activity.getSupportFragmentManager(), "errordialog");
    }

    public void setFragment(android.support.v4.app.Fragment fragment) {
        this.fragment = fragment;
    }

    public GPlusLoginListener getGPlusLoginListener() {
        return gPlusLoginListener;
    }

    public void setGPlusLoginListener(GPlusLoginListener gPlusLoginListener) {
        this.gPlusLoginListener = gPlusLoginListener;
    }

    public void logout() {
        try {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        }catch (Exception exp){
            Log.d(getClass().getName(),"Logout Exception");
        }
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        private GPlus gPlus;

        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            gPlus.onDialogDismissed();
        }

        public void setGPlus(GPlus GPlus) {
            this.gPlus = GPlus;
        }

        public GPlus getGPlus() {
            return gPlus;
        }
    }


    public static interface GPlusLoginListener{

        public void onError();
        public void onSuccess(Person person);
        public void onLoginButtonClick();
    }
}
