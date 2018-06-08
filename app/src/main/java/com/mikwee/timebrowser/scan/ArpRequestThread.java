package com.mikwee.timebrowser.scan;


import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class ArpRequestThread extends Thread {

    private int attempts;
    private InetAddress ia;
    private boolean isUp = false;
    private int timeout;

    ArpRequestThread(InetAddress ia, int timeout, int attempts) {
        this.ia = ia;
        this.timeout = timeout;
        this.attempts = attempts;
    }

    public void run() {

        //Loop until our host is NOT UP but we have attempts
        while (!isUp && attempts > 0) {

            //Test the IP address to see if is reachable
            try {
                isUp = ia.isReachable(timeout);
                attempts--;
            } catch (SocketException e) {
                return;
            } catch (IOException e2) {
                e2.printStackTrace();
                return;
            }
        }
    }

}
