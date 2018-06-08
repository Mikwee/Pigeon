package com.mikwee.timebrowser.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.activities.MainActivity;
import com.mikwee.timebrowser.scan.ControlThread;
import com.mikwee.timebrowser.scan.IpRange;
import com.mikwee.timebrowser.utils.HostInfo;
import com.mikwee.timebrowser.views.ProgressCircle;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ScanFragment extends Fragment implements ProgressCircle.FinishAnimation {
    private final String TAG = getClass().getSimpleName();

    public enum ScanAction {
        SCAN_END,
        SCAN_UPDATE,
        SCAN_HOST_FOUND,
        SCAN_MESSAGE,
        SCAN_ERROR
    }

    private ControlThread ct;
    private boolean isScanning;

    private ArrayList<HostInfo> hosts = new ArrayList<>();
    private int found = 0;

    //Views
    private ProgressCircle progressCircle;
    private TextView servicesFound;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_search_loading, container, false);

        //Init views
        initViews(v);

        //Keeps the fragment alive through configuration changes
        setRetainInstance(true);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getArguments() != null) {
            String ip = getArguments().getString("ip");
            String netmask = getArguments().getString("netmask");
            scanNetwork(ip, netmask);
        }

        servicesFound.setText(found + "");
    }

    //Initialize views
    private void initViews(View v) {
        servicesFound = (TextView) v.findViewById(R.id.found_number);

        progressCircle = (ProgressCircle) v.findViewById(R.id.progress_circle);
        progressCircle.setFinishListener(this);

    }

    //Starts the newtwork scan process to find available services
    public void scanNetwork(String ip, String netmask) {

        if (isScanning)
            return;

        try {
            // Try to get the range of IP to analyze
            IpRange ir = new IpRange(InetAddress.getByName(ip), InetAddress.getByName(netmask));

            isScanning = true;

            //start the control thread
            ct = new ControlThread(new Hand(this), ir);
            ct.start();


        } catch (UnknownHostException e1) {

            Log.e(TAG, "Ip range failed");
            Toast.makeText(getContext(), "An error Occurred.", Toast.LENGTH_LONG).show();
            e1.printStackTrace();
            //Go back to positive fragment
            getFragmentManager().popBackStackImmediate();

        }

    }

    //Where all messages from scan will be handled
    private static class Hand extends Handler {
        private final WeakReference<ScanFragment> mFragment;

        Hand(ScanFragment sf) {
            mFragment = new WeakReference<>(sf);
        }

        //Handles the message passed from the Control Thread
        public void handleMessage(Message msg) {
            //Retrieve the ScanFragment instance
            ScanFragment sf = mFragment.get();
            //Get the message action
            ScanAction action = ScanAction.values()[msg.what];

            switch (action) {

                case SCAN_END:
                    sf.setProgress(100);
                    sf.isScanning = false;
                    Log.e(sf.TAG, "Scan finished");
                    break;

                case SCAN_UPDATE:
                    sf.setProgress((int) msg.obj);
                    break;

                case SCAN_HOST_FOUND:
                    HostInfo hostInfo = (HostInfo) msg.obj;
                    //Add host found and increment the counter view
                    sf.hosts.add(hostInfo);
                    sf.addFound();

                    //Log the host found
                    Log.e(sf.TAG, "----------host found----------");
                    Log.e(sf.TAG, "name= " + hostInfo.getName());
                    Log.e(sf.TAG, "ip= " + hostInfo.getIp());
                    Log.e(sf.TAG, "mac = " + hostInfo.getMac());
                    Log.e(sf.TAG, "requires password= " + hostInfo.isPasswordRequired());
                    Log.e(sf.TAG, "-------------------------------");
                    break;

                case SCAN_MESSAGE:
                    Toast.makeText(sf.getContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;

                case SCAN_ERROR:
                    String error = (String) msg.obj;
                    Toast.makeText(sf.getContext(), error, Toast.LENGTH_SHORT).show();
                    Log.e(sf.TAG, "Error:" + error);
                    sf.isScanning = false;
                    break;

            }
            super.handleMessage(msg);
        }

    }

    //Update the ProgressCircle view

    public void setProgress(int progress) {
        progressCircle.setProgress((float) progress / 100);
        progressCircle.startAnimation();
    }

    //Increment the host found counter view
    private void addFound() {
        found++;
        servicesFound.setText(found + "");
    }

    //Stops the Control Thread
    public void stopScan() {
        if (isScanning) {
            ct.interrupted = true;
            isScanning = false;
        }
        interrupt();
    }

    //Returns true if is running
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public void onAnimationFinish() {
        MainActivity ma = (MainActivity) getActivity();
        if (ma != null)
            ma.loadScanResult(hosts);

        interrupt();
    }

    public void interrupt() {
        if (getFragmentManager().beginTransaction() != null)
            getFragmentManager().beginTransaction().remove(this).commit();
    }

}