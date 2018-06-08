package com.mikwee.timebrowser.scan;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.mikwee.timebrowser.fragments.ScanFragment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Given an ip range loops through every ip and checks which one is a host
 */
public class ControlThread extends Thread {
    private final String TAG = getClass().getSimpleName();

    public volatile boolean interrupted = false;

    private Handler handle;

    private IpRange ir;
    private InetAddress irBegin;
    private InetAddress irEnd;

    //Total number of IPs in range
    private int totalHosts;
    //Total number of IPs in range tested
    private long totalScanned = 0;
    //Total number of ARP threads currently running
    private int arpThreadsCount = 0;

    //
    private ArrayList<HostScan> hostScans = new ArrayList<>();
    //List of IP, MAC as key, values
    private HashMap<String, String> ipMacList = new HashMap<>();
    //List of ARP threads running
    private ArrayList<ArpRequestThread> arpThreadsList = new ArrayList<>();


    //Constructor which takes in a handler and a IP range to analyze
    public ControlThread(Handler handle, IpRange ir) {
        this.handle = handle;
        this.ir = ir;

        //First and last IPs to analyze in the range
        irBegin = ir.getBeginIP();
        irEnd = ir.getEndIP();

        //How many IPs to analyze
        totalHosts = calcTotalHosts();

    }

    //Calculates total number of hosts to test
    private int calcTotalHosts() {
        return (((((irEnd.getAddress()[0] & 255) - (irBegin.getAddress()[0] & 255)) + 1) * (((irEnd.getAddress()[1] & 255) - (irBegin.getAddress()[1] & 255)) + 1)) * (((irEnd.getAddress()[2] & 255) - (irBegin.getAddress()[2] & 255)) + 1)) * (((irEnd.getAddress()[3] & 255) - (irBegin.getAddress()[3] & 255)) + 1);
    }

    //Called when a thread is started
    public void run() {

        //Get starting time
        long before = SystemClock.uptimeMillis();

        //Loop through every IP in our range
        walkOnRange();
        //Waits for all active ArpThreads to finish
        waitArpsFinish();
        //Do one last check to arpTable
        processArpTable();
        //Waits for all active HostScans to finish
        waitHostScansFinish();

        //Get end time
        long after = SystemClock.uptimeMillis();

        sendStatus(ScanFragment.ScanAction.SCAN_END, "SCAN COMPLETE (" + ((after - before) / 1000) + " SECONDS )");
    }

    //Loops throught every IP and calls initThread()
    private void walkOnRange() {
        for (int i0 = irBegin.getAddress()[0] & 255; i0 <= (irEnd.getAddress()[0] & 255); i0++) {
            for (int i1 = irBegin.getAddress()[1] & 255; i1 <= (irEnd.getAddress()[1] & 255); i1++) {
                for (int i2 = irBegin.getAddress()[2] & 255; i2 <= (irEnd.getAddress()[2] & 255); i2++) {
                    int i3 = irBegin.getAddress()[3] & 255;
                    while (i3 <= (irEnd.getAddress()[3] & 255)) {
                        //----------------------CORE
                        if (interrupted) {
                            terminateThis();
                            return;
                        }
                        if (!((i3 & 255) == 0 || (i3 & 255) == 255)) {
                            //for every ip call initThread()
                            try {
                                initThread(InetAddress.getByAddress(new byte[]{(byte) (i0 & 255), (byte) (i1 & 255), (byte) (i2 & 255), (byte) (i3 & 255)}));
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                        }
                        i3++;
                        //------------------------CORE
                    }
                }
            }
        }
    }

    //Called for every IP in range
    private void initThread(InetAddress ia) {

        //Checks if IP is reachable so that system updates ARP table 
        ArpRequestThread pt = new ArpRequestThread(ia, 400, 2);
        pt.start();

        arpThreadsList.add(pt);
        arpThreadsCount++;
        totalScanned++;

        //If active arpThreads are 32
        if (arpThreadsCount == 32) {

            //Send a progress update message
            sendStatus(ScanFragment.ScanAction.SCAN_UPDATE, (int) ((totalScanned * 100) / ((long) totalHosts)));
            //Process the table to gather all IPs active found
            processArpTable();
            //Wait for all arpThreads to finish
            waitArpsFinish();

            //clear all variables
            arpThreadsList.clear();
            arpThreadsCount = 0;
        }
    }

    //Gets the Arp table from device and checks every active IP
    private void processArpTable() {

        //Read the device Arp Table
        ArpTable arp = new ArpTable();

        //If ARP is not supported
        if (arp.ipList == null) {
            if (arp.error != null) {
                //show error message
                sendStatus(ScanFragment.ScanAction.SCAN_ERROR, arp.error);
            }
            interrupted = true;
            return;
        }

        //Get all IP, MAC from list
        ipMacList.putAll(arp.ipList);

        //For each item in the list
        for (Entry<String, String> me : ipMacList.entrySet()) {

            try {
                //If the IP in list is in our range
                InetAddress ia = InetAddress.getByName(me.getKey());
                if (ir.isInRange(ia)) {
                    //Call HostScan to identify all the hosts we are looking for
                    HostScan hostScan = new HostScan(handle, me.getKey(), me.getValue(), ia);
                    //If host has not been scanned yet
                    if (!hostScans.contains(hostScan)) {
                        //If running hostscans are >16 we wait until they die
                        waitForScan();
                        //Start hostScan
                        hostScan.start();
                        hostScans.add(hostScan);
                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }


    //Waits for hostscans to complete if more than 16 are running
    private void waitForScan() {
        try {
            //If number of hostscans still running is grater than 16
            if (getAliveHostScansCount() >= 16) {
                //Loop through every hostscan
                for (int i = 0; i < hostScans.size(); i++) {
                    //Check if is alive
                    if (hostScans.get(i).isAlive())
                        //Wait until it dies
                        hostScans.get(i).join();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Returns the number of hostscans currently running
    private int getAliveHostScansCount() {

        int scan_threads = 0;
        //Loop through every hostscan
        for (int i = 0; i < hostScans.size(); i++) {
            //Check if is alive
            if (hostScans.get(i).isAlive())
                scan_threads++;
        }

        return scan_threads;
    }

    //Send message via handler
    private void sendStatus(ScanFragment.ScanAction scanAction, Object obj) {
        if (interrupted)
            return;

        Message msg = new Message();
        msg.what = scanAction.ordinal();
        msg.obj = obj;
        handle.sendMessage(msg);
    }

    //Terminate this thread 
    private void terminateThis() {
        Log.e(TAG, "Thread is interrupted!");
        waitHostScansFinish();
    }

    //Waits for all active ArpThreads to finish
    private void waitArpsFinish() {
        //loop through every arpThread
        for (int i = 0; i < arpThreadsList.size(); i++) {
            //If interrupted terminate this thread
            if (interrupted) {
                terminateThis();
                return;
            }
            //wait for all arps threads to finish
            try {
                arpThreadsList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Waits for all active HostScans to finish
    private void waitHostScansFinish() {
        for (int i = 0; i < hostScans.size(); i++) {
            try {
                if (hostScans.get(i).isAlive())
                    (hostScans.get(i)).join();
                //hi.interrupted = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
