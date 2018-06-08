package com.mikwee.timebrowser.utils;

import java.io.Serializable;
import java.net.InetAddress;

public class HostInfo implements Comparable, Serializable {


    //GENERAL INTERNET ADDRESS
    private InetAddress ia;
    //IP ADDRESS
    private String ip;
    //MAC ADDRESS
    private String mac = "";

    //VENDOR
    private String vendor = ""; //Apple, Netgear
    //IS LOCKED
    private boolean passwordRequired;
    // THE SERVICE NAME
    private String name;
    //IS IT SMB ?
    private boolean smb;

    //the real name todo check is useful
    private String dns_name = "";

    //---------------------------------GETTERS & SETTERS

    public boolean isSmb() {
        return smb;
    }

    public void setSmb(boolean smb) {
        this.smb = smb;
    }

    public InetAddress getIa() {
        return ia;
    }

    public void setIa(InetAddress ia) {
        this.ia = ia;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    public void setPasswordRequired(boolean passwordRequired) {
        this.passwordRequired = passwordRequired;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDns_name() {
        return dns_name;
    }

    public void setDns_name(String dns_name) {
        this.dns_name = dns_name;
    }

    //----------------------------------END GETTERS & SETTERS


    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HostInfo)) {
            return false;
        }
        HostInfo hi = (HostInfo) obj;
        if (this.ip == null && hi.ip == null) {
            return true;
        }
        if (hi.ip == null) {
            return false;
        }
        if (this.ip == null) {
            return false;
        }
        return hi.ip.compareTo(this.ip) == 0;
    }

    public int compareTo(Object obj) throws ClassCastException {
        if (this == obj) {
            return 0;
        }
        if (!(obj instanceof HostInfo)) {
            return 0;
        }
        HostInfo anotherHost = (HostInfo) obj;
        for (int i = 0; i < 4; i++) {
            int retval = (this.ia.getAddress()[i] & 255) - (anotherHost.ia.getAddress()[i] & 255);
            if (retval != 0) {
                return retval;
            }
        }
        return 0;
    }
}

