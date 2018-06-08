package com.mikwee.timebrowser.fragments;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikwee.timebrowser.R;

import static android.content.Context.WIFI_SERVICE;


public class NegativeFragment extends Fragment {
    TextView subtitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_negative, container, false);
        initViews(v);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        //try to turn on wifi programmatically
        turnWifiOn();
    }

    private void initViews(View v) {
        subtitle = (TextView) v.findViewById(R.id.negative_subtitle);
    }

    //Try to turn wifi on
    private void turnWifiOn() {
        WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);

        //if wifi couldn't be turned on display message
        assert wifiManager != null;
        if (!wifiManager.setWifiEnabled(true))
            subtitle.setText(R.string.negative_subtitle_failed);
    }

}
