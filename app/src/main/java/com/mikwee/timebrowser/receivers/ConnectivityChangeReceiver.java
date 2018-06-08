package com.mikwee.timebrowser.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.mikwee.timebrowser.activities.MainActivity;


public class ConnectivityChangeReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName();

    //The possible types of action to send to the mainActivity
    public enum INTERMEDIATE_ACTION {
        CONNECTING,
        DISCONNECTING,
        WIFI_BUT_DISCONNECTED
    }

    //Just for debugging, all actions indexed just as in WifiManager class
    String[] wifiState = {
            "disabling",
            "disabled",
            "enabling",
            "enabled",
            "unknown"
    };

    //------Broadcast Receiver filters
    //Broadcast intent action indicating that Wi-Fi has been enabled, disabled, enabling, disabling, or unknown.
    //One extra provides this state as an int. Another extra provides the previous state, if available.
    String wifiSTATE = "android.net.wifi.WIFI_STATE_CHANGED";
    //Broadcast intent action indicating that the state of Wi-Fi connectivity has changed.
    //One extra provides the new state in the form of a NetworkInfo object. If the new state is CONNECTED, additional extras may provide the BSSID and WifiInfo of the access point as a String
    String connectionSTATE = "android.net.wifi.STATE_CHANGE";

    //The connection state: connecting/ connected/ null
    String currentState = "";

    //The wifi state: true = ON
    boolean wifiOn = false;

    MainActivity activity;

    @Override
    public void onReceive(Context context, Intent intent) {

        //Get reference to activity
        activity = (MainActivity) context;

        takeAction(intent);

        // debugIntent(intent, "grokkingandroid");
    }

    private void takeAction(Intent intent) {

        //Get what type of intent it is
        String action = intent.getAction();

        assert action != null;
        if (action.equals(wifiSTATE)) {
            //get the previous wifi state
            int prevState = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, 4);
            //get the current wifi state
            int nextState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 4);

            //indicates previously there WAS    NO WIFI
            boolean prevNoWifi = prevState == WifiManager.WIFI_STATE_DISABLED ||
                    prevState == WifiManager.WIFI_STATE_DISABLING ||
                    prevState == WifiManager.WIFI_STATE_UNKNOWN;

            //indicates previously there WAS    YES WIFI
            boolean prevYesWifi = prevState == WifiManager.WIFI_STATE_ENABLED ||
                    prevState == WifiManager.WIFI_STATE_ENABLING;

            //indicates now there         IS    NO WIFI
            boolean nextNoWifi = nextState == WifiManager.WIFI_STATE_DISABLED ||
                    nextState == WifiManager.WIFI_STATE_DISABLING ||
                    nextState == WifiManager.WIFI_STATE_UNKNOWN;

            //indicates now there         IS    YES WIFI
            boolean nextYesWifi = nextState == WifiManager.WIFI_STATE_ENABLED;


            if (prevNoWifi && nextYesWifi) {
                wifiOn = true;
                //Let's wait for the network connection broadcast
                waitForConnection();
            } else if (prevYesWifi && nextNoWifi) {
                wifiOn = false;
                showNegative();
            }


        } else if (action.equals(connectionSTATE)) {

            //Get network info about wifi connection
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            NetworkInfo.State state = info.getState();

            switch (state) {
                case CONNECTING:
                    currentState = "connecting";
                    showIntermediate(INTERMEDIATE_ACTION.CONNECTING);
                    break;

                case CONNECTED:
                    currentState = "connected";
                    showPositive();
                    break;

                case DISCONNECTING:
                    showIntermediate(INTERMEDIATE_ACTION.DISCONNECTING);
                    break;

                case DISCONNECTED:
                    if (wifiOn)
                        showIntermediate(INTERMEDIATE_ACTION.WIFI_BUT_DISCONNECTED);
                    break;

                default:
                    Log.e(TAG, "Strange state received from network info");
                    if (wifiOn)
                        showIntermediate(INTERMEDIATE_ACTION.WIFI_BUT_DISCONNECTED);
                    break;
            }

        }
    }

    //Wait for some time to wait the arrival of network connection broadcast
    private void waitForConnection() {
        final Handler handler = new Handler();
        //run this check after 300 mills, when the network connection broadcast should have been arrived
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentState.equals("connecting")) {
                    handler.postDelayed(this, 500);
                } else if (currentState == null) {
                    showIntermediate(INTERMEDIATE_ACTION.WIFI_BUT_DISCONNECTED);
                }

            }
        }, 300);

    }

    private void showPositive() {
        activity.loadPositive();
    }

    private void showNegative() {
        activity.loadNegative();
    }

    private void showIntermediate(INTERMEDIATE_ACTION action) {
        activity.loadIntermediate(action);
    }

    //Debug method to display all info from received intent
    private void debugIntent(Intent intent, String tag) {
        Log.v(tag, "action: " + intent.getAction());
        Log.v(tag, "component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.v(tag, "key [" + key + "]: " +
                        extras.get(key));
            }
        } else {
            Log.v(tag, "no extras");
        }
    }
}
