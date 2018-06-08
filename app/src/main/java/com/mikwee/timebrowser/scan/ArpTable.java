package com.mikwee.timebrowser.scan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

class ArpTable {

    String error = null;
    HashMap<String, String> ipList;

    ArpTable() {
        getArpTable();
    }

    //Read the Arp table from device
    private void getArpTable() {

        try {
            //BufferedReader in = new BufferedReader(new FileReader("/proc/net/arp"), 24576);
            BufferedReader in = new BufferedReader(new FileReader("/proc/net/arp"));
            int count = 0;
            ipList = new HashMap<>();

            String line;
            String[] line_parts;
            String ip_part;
            String mac_part;

            //Loop through all the file
            while (true) {
                line = in.readLine();
                if (line == null) {
                    in.close();
                    return;
                } else if (count == 0) {
                    count++;
                } else {
                    count++;
                    line_parts = line.split(" +");
                    //Firts part of line is ip
                    ip_part = line_parts[0];
                    //Second part of line is mac
                    mac_part = line_parts[3];

                    //If it's different from... add it to our list
                    if (mac_part.compareTo("00:00:00:00:00:00") != 0) {
                        ipList.put(ip_part, mac_part);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e2) {
            e2.printStackTrace();
            error = "ARP scan not available in your device. We'll implement it soon";
        }
    }
}

