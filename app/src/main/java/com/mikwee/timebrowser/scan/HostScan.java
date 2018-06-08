package com.mikwee.timebrowser.scan;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.mikwee.timebrowser.fragments.ScanFragment;
import com.mikwee.timebrowser.smb.SmbThread;
import com.mikwee.timebrowser.utils.HostInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Analyzes an alive IP address and checks if it meets the requirements
 */
public class HostScan extends Thread {
    private String TAG = getClass().getSimpleName();

    private HostInfo hostInfo = new HostInfo();
    private Handler handle;
    public volatile boolean interrupted = false;

    HostScan(Handler handler, String ip, String hw, InetAddress ia) {
        this.handle = handler;

        //Set IP, MAC, IA to a host info object
        hostInfo.setIp(ip);
        hostInfo.setMac(hw);
        hostInfo.setIa(ia);
    }

    //Called when thread is started
    public void run() {

        //Get start time
        long before = SystemClock.uptimeMillis();
        Log.e(TAG, "Started host scan on: " + hostInfo.getIp());

        //Init and run SmbThread for IP
        SmbThread st = new SmbThread(handle, "smb://" + hostInfo.getIp() + "/", SmbThread.SmbAction.FIND_SHARES);
        st.setResolveIp(hostInfo.getIp());
        st.run();

        //If is not smb return
        if (!st.isSmb) {
            Log.e(TAG, hostInfo.getIp() + " is not smb");
            return;
        }
        //If does not meet the vendor restrictions return
       // if (!meetsVendorRequirements()) todo see how to handle this
          //  return;

        //Set relevant info
        hostInfo.setSmb(true);
        hostInfo.setPasswordRequired(st.requiresPassword);
        hostInfo.setName(st.serverName);

        //Notify user we found a machine
        Message msg = new Message();
        msg.obj = hostInfo;
        msg.what = ScanFragment.ScanAction.SCAN_HOST_FOUND.ordinal();
        handle.sendMessage(msg);

        //End scan notification
        Log.e(TAG, "Host " + hostInfo.getIp() + " scanned for " + Long.toString(SystemClock.uptimeMillis() - before) + " ms");
    }

    //Checks if the vendor is right for the machines we are looking for
    private boolean meetsVendorRequirements() {
        hostInfo.setVendor(analyzeMacOnline(hostInfo.getMac()));
        //todo add restriction
        //vendor is somehow "apple" or "AzureWave Technology Inc."
        return true;
    }

    //Checks online for the name of vendor 
    private String analyzeMacOnline(String mac) {

        //if we have an actual mac
        String vendor = "";
        if (mac == null || mac.equals(""))
            return "";

        HttpURLConnection urlConnection;
        try {

            URL url = new URL("https://api.macvendors.com/" + mac);
            urlConnection = (HttpURLConnection) url.openConnection();

            int status = urlConnection.getResponseCode();
            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    vendor = sb.toString();
                    break;
            }
            urlConnection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vendor;
    }

    //Compares two HostScan Objects
    public boolean equals(Object obj) throws ClassCastException {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HostScan)) {
            return false;
        }
        HostScan anotherHostScan = (HostScan) obj;
        if (hostInfo == null && anotherHostScan.hostInfo == null) {
            return true;
        }
        if (hostInfo == null) {
            return false;
        }
        return hostInfo.equals(anotherHostScan.hostInfo);
    }

}
