package com.mikwee.timebrowser.smb;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mikwee.timebrowser.utils.Utils;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import jcifs.netbios.NbtAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import static com.mikwee.timebrowser.smb.CheckThread.SmbAction.AUTH;


public class CheckThread extends Thread {
    public static final String TAG = CheckThread.class.getSimpleName();

    //Info about the server
    private String serverName = ""; // Real name (ex.. STATICA)
    private boolean isSmb = false;
    private boolean requiresPassword = false;
    private String resolveIp = "";

    //Num of login retries
    private int loginAttempts = 0;

    //----------------------BASE VARIABLES
    private SmbAction action;
    private Handler handle;
    private Context c;
    private String url;
    private NtlmPasswordAuthentication auth;


    //----------------------CONSTRUCTORS

    CheckThread(Handler handle, String url, SmbAction action, Context c, NtlmPasswordAuthentication auth) {
        this.handle = handle;
        this.url = url;
        this.action = action;
        this.c = c;
        this.auth = auth;
    }

    @Override
    public void run() {
        try {
            SmbFile file = new SmbFile(url, auth);
            Message msg = new Message();
            switch (action) {
                case IS_SMB: //-------------Is IP smb

                    if (file.listFiles() == null)  //Throws exception if is not smb
                        return;

                    isSmb = true;
                    requiresPassword = false;
                    //Retrieve the host name
                    retrieveName();
                    break;

                case AUTH: //---------------Verify authentication

                    file.listFiles(); // Throws exception if is not smb

                    msg.what = action.ordinal();
                    msg.obj = true;
                    break;
            }
            //sand back operation result
            handle.sendMessage(msg);

        } catch (SmbException e) {//---------------------SMB EXC
            if (e.getNtStatus() == -1073741715) { // If the cause is "logon failure"
                requiresPassword = true;
                isSmb = true;
                retrieveName();
            } else if (action == AUTH && loginAttempts < 2) { //if we had an exception try to login
                run();
                loginAttempts++;
                return;
            }
            sendErr(e.getNtStatus(), e.getMessage());

        } catch (MalformedURLException e) {//------------MALFORMED URL EXC
            Log.e(TAG, "malformed url exc");
            sendErr(666, null);

        } catch (RuntimeException e) { //----------------RUNTIME EXC
            Log.e(TAG, "runtime exc");
            if (serverName.equals(""))
                this.requiresPassword = true;
            sendErr(666, null);
        }

    }

    //Retrieve the host name given the IP
    private void retrieveName() {

        if (action != SmbAction.IS_SMB)
            return;


        try {
            //Find NbtAddresses from IP
            NbtAddress[] na = NbtAddress.getAllByAddress(resolveIp);

            if (na.length != 0) {
                //for every address
                for (int i = 0; serverName.equals(""); i++) {
                    if (na[i].getNameType() == 32)
                        serverName = na[i].getHostName();
                }
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }


    //Create error message to return to handler
    void sendErr(int errorCode, String defMessage) {
        if (action == SmbAction.IS_SMB)
            return;

        //Create the message
        Message msg = new Message();
        msg.what = SmbAction.ERROR.ordinal();
        msg.obj = Utils.getErrorString(c, errorCode, action, defMessage);
        //send the message
        handle.sendMessage(msg);

        Log.e(TAG, (String) msg.obj);
    }


    //-------------------GETTERS

    public String getServerName() {
        return serverName;
    }

    public String getResolveIp() {
        return resolveIp;
    }

    public boolean isSmb() {
        return isSmb;
    }

    public boolean isRequiresPassword() {
        return requiresPassword;
    }


    //-------------------SETTERS

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setSmb(boolean smb) {
        isSmb = smb;
    }

    public void setRequiresPassword(boolean requiresPassword) {
        this.requiresPassword = requiresPassword;
    }

    public void setResolveIp(String resolveIp) {
        this.resolveIp = resolveIp;
    }

    //-------------------BUILDER

    public enum SmbAction {
        AUTH,
        IS_SMB,
        ERROR
    }

    public static class Builder {

        private Handler handler;
        private SmbAction action;
        private Context context;
        private String url;

        private NtlmPasswordAuthentication auth;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setHandler(Handler handler) {
            this.handler = handler;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setAction(SmbAction action) {
            this.action = action;
            return this;
        }

        public Builder setUsernamePassword(String username, String password) {
            this.auth = new NtlmPasswordAuthentication(username + ":" + password);
            return this;
        }

        public Builder setAuth(NtlmPasswordAuthentication auth) {
            this.auth = auth;
            return this;
        }

        public CheckThread build() {
            return new CheckThread(handler, url, action, context, auth);
        }

    }

}
