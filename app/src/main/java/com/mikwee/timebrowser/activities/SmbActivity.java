package com.mikwee.timebrowser.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.fragments.BrowserFragment;
import com.mikwee.timebrowser.fragments.LoginFragment;
import com.mikwee.timebrowser.smb.CheckThread;
import com.mikwee.timebrowser.utils.HostInfo;
import com.mikwee.timebrowser.utils.Utils;

import java.lang.ref.WeakReference;

import static com.mikwee.timebrowser.smb.CheckThread.SmbAction.AUTH;
import static com.mikwee.timebrowser.smb.CheckThread.SmbAction.ERROR;

public class SmbActivity extends AppCompatActivity {

    //Name of the preferences and extra bundle
    public static final String prefsName = "host";

    //Object containing url and other info about host
    private HostInfo hostInfo;

    //Temporary store username and password
    private String username = "guest", password = "";

    //Fragments
    private FragmentManager fm;
    private BrowserFragment browserFragment;
    private LoginFragment loginFragment;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //if it's called for the first time
        if (savedInstanceState == null) {
            //Get passed host info from intent
            hostInfo = (HostInfo) getIntent().getSerializableExtra(prefsName);
        } else {
            //Get host info from savedInstanceState
            hostInfo = (HostInfo) savedInstanceState.getSerializable(prefsName);
        }

        //if in both cases info is null
        if (hostInfo == null) {
            onBackPressed();
            return;
        }

        //Save an instance of FragmentManager
        fm = getSupportFragmentManager();
        //If browser fragment does not exist
        if (fm.findFragmentByTag(BrowserFragment.TAG) == null) {
            //load login fragment
            if (hostInfo.isPasswordRequired())
                loadLogin();
            else
                loadBrowser();
        }

    }

    @Override
    protected void onStop() { //todo to check
        super.onStop();
        //If the Activity is being closed not because of screen rotation or configuration changes
        if (!isChangingConfigurations()) { //todo check if is usefull anymore
            //Delete all temorary files
            Utils.deleteTempFiles(this);
        }
    }

    @Override
    public void onBackPressed() { //todo to check
        //if our browser fragment does not exist   OR   if we are at root directory go to positiveFragment
        if (browserFragment != null) {

            //if we are not in selection mode
            if (browserFragment.selectedBar.inSelection())
                browserFragment.selectedBar.hideFileSelection();

                //If we can't go back
            else if (!browserFragment.goBackTo(Utils.getDepth(browserFragment.currentPath) - 1))
                super.onBackPressed();

        } else
            super.onBackPressed();


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save current host info
        outState.putSerializable(prefsName, hostInfo);
    }


    //---------------------------------FRAGMENTS

    //Loads the LoginFragment in apposite View
    private void loadLogin() {

        loginFragment = (LoginFragment) fm.findFragmentByTag(LoginFragment.TAG);

        //if fragment is not null (and showing)
        if (loginFragment == null) {

            Bundle b = new Bundle();
            b.putString("ip", hostInfo.getIp());
            b.putString("hostName", hostInfo.getName());
            b.putString("mac", hostInfo.getMac());

            loginFragment = new LoginFragment();
            loginFragment.setArguments(b);

            fm.beginTransaction()
                    .replace(R.id.fragment_container, loginFragment, LoginFragment.TAG)
                    .commit();
        }


    }

    //Loads the BrowserFragment in apposite view
    public void loadBrowser() {

        //Try to get saved instance
        browserFragment = (BrowserFragment) fm.findFragmentByTag(BrowserFragment.TAG);

        //If it is null, create a new one
        if (browserFragment == null) {

            Bundle b = new Bundle();
            b.putString("url", createUrlFromIp(hostInfo.getIp())); //Create a url based on passed host IP
            b.putString("username", username);
            b.putString("password", password);

            browserFragment = new BrowserFragment();
            browserFragment.setArguments(b);

            //Inflate in layout
            fm.beginTransaction()
                    .replace(R.id.fragment_container, browserFragment, BrowserFragment.TAG)
                    .commit();

        }

    }

    //---------------------------------END FRAGMENTS

    //Called when the login button is pressed
    public void tryLogin(String u, String p) {

        //Store for after
        username = u;
        password = p;

        //Create base host url
        String url = createUrlFromIp(hostInfo.getIp());

        //Start a background thread to verify credentials
        new CheckThread.Builder(this)
                .setHandler(new Hand(this)).setUrl(url).setAction(AUTH)
                .setUsernamePassword(u, p)
                .build().start();

    }

    //Creates a smb url from the host ip
    private String createUrlFromIp(String ip) {
        return "smb://" + ip + "/";
    }

    //This handles the auth thread result
    private static class Hand extends Handler {

        private final WeakReference<SmbActivity> activityReference;

        Hand(SmbActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        //Handles the message passed from any thread
        public void handleMessage(Message msg) {

            SmbActivity activity = activityReference.get();
            if (activity == null)
                return;

            CheckThread.SmbAction action = CheckThread.SmbAction.values()[msg.what];
            if (action == AUTH) {

                //If authentication is positive
                if ((boolean) msg.obj)
                    //Load browserFragment
                    activity.loadBrowser();

            } else if (action == ERROR) {

                String error = (String) msg.obj;

                //If the loginFragment is not null hide loading
                if (activity.loginFragment != null)
                    activity.loginFragment.hideLoading();
                //Show the error to the user
                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();

            }

            super.handleMessage(msg);
        }

    }

}
