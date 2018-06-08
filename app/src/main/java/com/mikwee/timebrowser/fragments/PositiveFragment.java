package com.mikwee.timebrowser.fragments;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mikwee.timebrowser.R;

import static android.content.Context.WIFI_SERVICE;

public class PositiveFragment extends Fragment implements View.OnClickListener {

    private OnStartScanListener mCallback;

    private String SSID = null;
    private String IP = null;
    private String NETMASK = null;

    //Views
    private TextView ipTv;
    private TextView netmaskTv;
    private TextView ssidTv;


    // The container Activity must implement this interface so the frag can deliver messages
    public interface OnStartScanListener {
        //called to perform a network scan
        void onPerformScan(String ip, String netmask);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_positive, container, false);

        //Init views and populate them
        initViews(v);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnStartScanListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Get SSID, IP, NETMASK
        getWLANinfo();
        populate();

    }

    @Override
    public void onClick(View v) {
        mCallback.onPerformScan(IP, NETMASK);
    }

    //Initialize the layout views
    private void initViews(View v) {

        Button scanButton = (Button) v.findViewById(R.id.button_scan);
        scanButton.setOnClickListener(this);

        ipTv = (TextView) v.findViewById(R.id.ip);
        netmaskTv = (TextView) v.findViewById(R.id.netmask);
        ssidTv = (TextView) v.findViewById(R.id.ssid);

    }

    //Populate the lyout views
    private void populate() {
        //sanitize strings
        if (SSID == null)
            SSID = "---";
        if (IP == null)
            IP = "---";
        if (NETMASK == null)
            NETMASK = "---";

        ssidTv.setText(SSID);
        ipTv.setText(IP);
        netmaskTv.setText(NETMASK);

    }

    //Get the network SSID, IP and NETMASK
    private void getWLANinfo() {

        Context c = getContext().getApplicationContext();

        //See if IP is static
        String use_static_ip = Settings.System.getString(c.getContentResolver(), "wifi_use_static_ip");
        boolean use_static = !(use_static_ip == null || use_static_ip.compareTo("0") == 0);

        //Get the wifiManager
        WifiManager wifiManager = (WifiManager) c.getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo info = wifiManager.getConnectionInfo();

        //Get the SSID
        if (info != null)
            SSID = info.getSSID();

        if (use_static) {
            //Determine static ip and netmask
            IP = Settings.System.getString(c.getContentResolver(), "wifi_static_ip");
            NETMASK = Settings.System.getString(c.getContentResolver(), "wifi_static_netmask");
        } else {
            //Determine dynamic ip and netmask
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            if (dhcpInfo != null) {
                IP = intToIpStr(dhcpInfo.ipAddress);
                NETMASK = intToIpStr(dhcpInfo.netmask);
            }
        }

    }

    //Trasforms an int into a IP structured string
    private String intToIpStr(int i) {
        return (i & 255) + "." + ((i >> 8) & 255) + "." + ((i >> 16) & 255) + "." + ((i >> 24) & 255);
    }


}