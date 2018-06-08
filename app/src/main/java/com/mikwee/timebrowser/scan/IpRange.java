package com.mikwee.timebrowser.scan;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Object containing the first and last IP in the range of the ones we have to scan
 */
public class IpRange {
    private final String TAG = getClass().getSimpleName();

    private String ipRangeStrPretty = "";
    private InetAddress ip_range_begin;
    private InetAddress ip_range_end;

    //Constructor with ip and netmask
    public IpRange(InetAddress ip, InetAddress netmask) {
        setBeginEnd(ip, netmask);
    }

    //Find BEGIN and END IPs
    private void setBeginEnd(InetAddress ip, InetAddress netmask) {

        byte[] begin = new byte[4];
        byte[] end = new byte[4];

        for (int i = 0; i < 4; i++) {
            begin[i] = (byte) (ip.getAddress()[i] & netmask.getAddress()[i]);
            end[i] = (byte) (ip.getAddress()[i] | (netmask.getAddress()[i] ^ -1));
        }

        if ((begin[3] & 255) == 0) {
            begin[3] = (byte) 1;
        }
        if ((begin[3] & 255) == 255) {
            begin[3] = (byte) -2;
        }
        if ((end[3] & 255) == 0) {
            end[3] = (byte) 1;
        }
        if ((end[3] & 255) == 255) {
            end[3] = (byte) -2;
        }

        try {
            ip_range_begin = InetAddress.getByAddress(begin);
            ip_range_end = InetAddress.getByAddress(end);
            ipRangeStrPretty = toPrettyString();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //Creates a String indicating the ip range
    private String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int addr1 = ip_range_begin.getAddress()[i] & 255;
            int addr2 = ip_range_end.getAddress()[i] & 255;
            if (addr1 == addr2) {
                sb.append(addr1);
            } else {
                sb.append(addr1);
                sb.append("-");
                sb.append(addr2);
            }
            if (i < 3) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    //Checks if a given ip is in our IP range 
    boolean isInRange(InetAddress ia) {
        if (ia == null)
            return false;

        for (int i = 0; i < 4; i++) {
            int i_end = ip_range_end.getAddress()[i] & 255;
            int i_test = ia.getAddress()[i] & 255;
            if (i_test < (ip_range_begin.getAddress()[i] & 255) || i_test > i_end) {
                return false;
            }
        }
        return true;
    }

/*
    public IpRange(String ip_range_str) throws UnknownHostException {
        if (!parseIpRange(ip_range_str)) {
            throw new UnknownHostException();
        }
    }

    private boolean parseIpRange(String iprange_str) {

        byte[] begin = new byte[4];
        byte[] end = new byte[4];

        if (iprange_str.contains("/")) {
            return parseNetmask(iprange_str);
        }

        String[] iprange_parts = iprange_str.split("\\.");
        if (iprange_parts.length != 4) {
            Log.e(TAG, "Bad ip range");
            return false;
        }

        int i = 0;
        while (i < 4) {
            try {
                int end_int;
                String[] ip_begin_end = iprange_parts[i].split("-");
                int begin_int = Integer.parseInt(ip_begin_end[0]);
                if (ip_begin_end.length == 2) {
                    end_int = Integer.parseInt(ip_begin_end[1]);
                } else {
                    end_int = begin_int;
                }
                if (begin_int < 0 || begin_int > 255 || end_int < 0 || end_int > 255) {
                    Log.d(TAG, "Bad ip range: error at octet " + Integer.toString(i) + 1);
                    return false;
                } else if (begin_int > end_int) {
                    Log.d(TAG, "Bad ip range, begin is bigger than end at octet " + Integer.toString(i + 1));
                    return false;
                } else {
                    begin[i] = (byte) begin_int;
                    end[i] = (byte) end_int;
                    i++;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ((begin[3] & 255) == 0) {
            begin[3] = (byte) 1;
        }
        if ((begin[3] & 255) == 255) {
            begin[3] = (byte) -2;
        }
        if ((end[3] & 255) == 0) {
            end[3] = (byte) 1;
        }
        if ((end[3] & 255) == 255) {
            end[3] = (byte) -2;
        }
        try {
            ip_range_begin = InetAddress.getByAddress(begin);
            ip_range_end = InetAddress.getByAddress(end);
            ipRangeStrPretty = iprange_str;
            return true;
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
            return false;
        }
    }

    private boolean parseNetmask(String iprange_str) {
        String[] iprange_parts = iprange_str.split("/");
        String net_str = iprange_parts[0];
        try {
            InetAddress mask = InetAddress.getByAddress(intToByteArray(((1 << (32 - Integer.parseInt(iprange_parts[1]))) - 1) ^ -1));
            InetAddress net = InetAddress.getByName(net_str);
            setBeginEnd(net, mask);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private byte[] intToByteArray(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }
*/

    //-----------GETTERS 

    public String getPrettyRange() {
        return ipRangeStrPretty;
    }

    public InetAddress getBeginIP() {
        return ip_range_begin;
    }

    public InetAddress getEndIP() {
        return ip_range_end;
    }


}
