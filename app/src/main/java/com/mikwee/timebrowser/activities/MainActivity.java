
package com.mikwee.timebrowser.activities;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.fragments.IntermediateFragment;
import com.mikwee.timebrowser.fragments.NegativeFragment;
import com.mikwee.timebrowser.fragments.PositiveFragment;
import com.mikwee.timebrowser.fragments.ScanFragment;
import com.mikwee.timebrowser.fragments.ScanResultFragment;
import com.mikwee.timebrowser.receivers.ConnectivityChangeReceiver;
import com.mikwee.timebrowser.utils.HostInfo;
import com.mikwee.timebrowser.utils.Wakkamole;

import java.util.ArrayList;

import static com.mikwee.timebrowser.R.id.fragment_container;

public class MainActivity extends AppCompatActivity implements PositiveFragment.OnStartScanListener {
    private final String TAG = getClass().getSimpleName();

    enum CurrentFragment {
        POSITIVE,
        INTERMEDIATE,
        NEGATIVE,
        SCAN,
        SCAN_RESULT
    }

    //Connectivity change receiver
    private BroadcastReceiver br;

    //why recreate a fragment if we can store it here? c'mon
    private NegativeFragment nFragment;
    private IntermediateFragment iFragment;
    private PositiveFragment pFragment;

    private CurrentFragment cf;
    private ConnectivityChangeReceiver.INTERMEDIATE_ACTION currentIntermediateAcrion;

    private boolean backPressed = false;


    String ip = null;
    String netmask = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //restore the current current fragment bookmark
        if (savedInstanceState != null) {
            cf = (CurrentFragment) savedInstanceState.getSerializable("currentFragment");

            Log.e(TAG, "saved state: " + cf.name());

            if (cf == CurrentFragment.SCAN)
                loadScan();
            else if (cf == CurrentFragment.SCAN_RESULT)
                loadScanResult(null);
        }

        //Check the wifi status and act as consequence before broadcast
        checkWifiAndConnection();

        //load ad if we are in portrait mode
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            AdView banner = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            banner.loadAd(adRequest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Register connectivity change Receiver
        br = new ConnectivityChangeReceiver();

        IntentFilter filters = new IntentFilter();
        //Broadcast intent action indicating that Wi-Fi has been enabled, disabled, enabling, disabling, or unknown. One extra provides this state as an int. Another extra provides the previous state, if available.
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        //Broadcast intent action indicating that the state of Wi-Fi connectivity has changed. One extra provides the new state in the form of a NetworkInfo object. If the new state is CONNECTED, additional extras may provide the BSSID and WifiInfo of the access point. as a String
        filters.addAction("android.net.wifi.STATE_CHANGE");

        this.registerReceiver(br, filters);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Unregister connectivity change Receiver
        unregisterReceiver(br);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("currentFragment", cf);
        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the actionBar Menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_action_bar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Depending on the Action Bar icon clicked, do some action
        switch (item.getItemId()) {
            case R.id.action_wakkamole:
                //Load and show ad
                new Wakkamole(this);
                return true;
            case R.id.action_settings:
                Intent iSettings = new Intent(this, SettingsActivity.class);
                startActivity(iSettings);
                return true;
            case R.id.action_credits:
                Intent iCredits = new Intent(this, CreditsActivity.class);
                startActivity(iCredits);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        //If we are on ScanFragment
        switch (cf) {
            case POSITIVE:
                super.onBackPressed();
                break;

            case INTERMEDIATE:
                super.onBackPressed();
                break;

            case NEGATIVE:
                super.onBackPressed();
                break;

            case SCAN:
                //Stop current network scan
                ((ScanFragment) getSupportFragmentManager()
                        .findFragmentByTag(ScanFragment.class.getSimpleName())).stopScan();
                //Return to positiveFragment
                loadPositive();
                break;

            case SCAN_RESULT:
                backPressed = true;
                loadPositive();
                break;
        }
    }


    public void onPerformScan(String ip, String netmask) {
        this.ip = ip;
        this.netmask = netmask;
        loadScan();
    }


    //Before we receive broadcast callback call, do some initialization
    private void checkWifiAndConnection() {

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        //Check if wifi is ON
        assert wifiManager != null; //this means it cannot be null
        if (wifiManager.isWifiEnabled()) {

            //Get connection info
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            assert connManager != null;//this means it cannot be null
            NetworkInfo activeInfo = connManager.getActiveNetworkInfo();

            //If we're connected to a wifi network
            if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                loadPositive();
            } else {
                loadIntermediate(ConnectivityChangeReceiver.INTERMEDIATE_ACTION.WIFI_BUT_DISCONNECTED);
            }

        } else {
            //Wifi disabled load negative fragment
            loadNegative();
        }

    }
    
    
    //---------------------------------------------------FRAGMENTS

    //Wifi is ON & CONNECTED, load positive fragment
    public void loadPositive() {

        //Try to see if ScanFragment is running
        ScanFragment sf = (ScanFragment) getSupportFragmentManager()
                .findFragmentByTag(ScanFragment.class.getSimpleName());

        //if we are scanning
        boolean isScannig = (sf != null) && sf.isScanning();

        //If we are on the result page
        boolean onResultPage = (cf == CurrentFragment.SCAN_RESULT) && !backPressed;

        backPressed = false;

        if (cf == CurrentFragment.POSITIVE || isScannig || onResultPage) {
            return;
        }

        //Update bookmark
        cf = CurrentFragment.POSITIVE;

        //See if we can find a instance saved by fragment manager
        PositiveFragment fragment = (PositiveFragment)
                getSupportFragmentManager().findFragmentByTag(PositiveFragment.class.getSimpleName());

        if (fragment == null) {
            // Try to find a saved instance in our activity
            if (pFragment == null)
                //Else create a new one 
                pFragment = new PositiveFragment();
        } else
            pFragment = fragment;

        //Replace all current fragments in container with positive one
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, pFragment, PositiveFragment.class.getSimpleName())
                .commit();
    }

    //WIfi is ON but NOT CONNECTED
    public void loadIntermediate(ConnectivityChangeReceiver.INTERMEDIATE_ACTION action) {

        //if the state is the same as this one than return
        if (cf == CurrentFragment.INTERMEDIATE && action == currentIntermediateAcrion) {
            return;
        }
        //update current location in app
        cf = CurrentFragment.INTERMEDIATE;
        currentIntermediateAcrion = action;


        //if activity does not have an instance
        if (iFragment == null) {
            iFragment = new IntermediateFragment();

            //Create bundle in which we put the state
            Bundle b = new Bundle();
            b.putSerializable("state", action);
            iFragment.setArguments(b);

        }

        //Replace other fragment with this
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, iFragment, IntermediateFragment.class.getSimpleName())
                .commit();

        if (iFragment == null) {
            //handle state change on the go
            iFragment.handleStateChange(action);
        }

    }

    //Wifi is OFF, load negative fragment
    public void loadNegative() {

        //if the state is the same as this one than return
        if (cf == CurrentFragment.NEGATIVE) {
            return;
        }
        //update the current location in app
        cf = CurrentFragment.NEGATIVE;

        //if activity does not have an instance
        if (nFragment == null) {
            nFragment = new NegativeFragment();
        }

        //replace fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, nFragment, NegativeFragment.class.getSimpleName())
                .commit();
    }

    //Scan button pressed, let's search
    public void loadScan() {

        //Update bookmark
        cf = CurrentFragment.SCAN;

        //find an instance saved by supportFragmentManager
        ScanFragment fragment = (ScanFragment) getSupportFragmentManager()
                .findFragmentByTag(ScanFragment.class.getSimpleName());

        if (fragment == null) {

            //Create a new one instance
            fragment = new ScanFragment();
            //put extras (ip, netmask, action)
            Bundle bundle = new Bundle();
            bundle.putString("ip", ip);
            bundle.putString("netmask", netmask);

            fragment.setArguments(bundle);

        }


        getSupportFragmentManager()
                .beginTransaction()
                .replace(fragment_container, fragment, ScanFragment.class.getSimpleName())
                .commit();
    }

    //Scan has ended, display hosts found
    public void loadScanResult(ArrayList<HostInfo> hosts) {

        ScanResultFragment fragment = null;

        if (cf == CurrentFragment.SCAN_RESULT) {
            fragment = (ScanResultFragment)
                    getSupportFragmentManager().findFragmentByTag(ScanResultFragment.class.getSimpleName());
        }

        //Update current fragment
        cf = CurrentFragment.SCAN_RESULT;

        if (fragment == null) {

            fragment = new ScanResultFragment();

            //Transform arraylist to array
            HostInfo[] hi = hosts.toArray(new HostInfo[hosts.size()]);

            Bundle bundle = new Bundle();
            bundle.putSerializable("hosts", hi);
            fragment.setArguments(bundle);


        }

        getSupportFragmentManager().beginTransaction()
                .replace(fragment_container, fragment, ScanResultFragment.class.getSimpleName())
                .commit();

    }

}