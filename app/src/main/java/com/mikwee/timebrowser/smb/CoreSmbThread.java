package com.mikwee.timebrowser.smb;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mikwee.timebrowser.utils.Utils;

import jcifs.smb.NtlmPasswordAuthentication;


public class CoreSmbThread extends Thread {
    private static final String TAG = CoreSmbThread.class.getSimpleName();

    //----------------------BASE VARIABLES
    public SmbAction action;
    public Handler handle;
    public Context c;
    public String url;
    public NtlmPasswordAuthentication auth;


    //----------------------CONSTRUCTORS

    CoreSmbThread(Handler handle, String url, SmbAction action, Context c, NtlmPasswordAuthentication auth) {
        this.handle = handle;
        this.url = url;
        this.action = action;
        this.c = c;
        this.auth = auth;
    }

    //-----------------------METHODS

    public void run() {
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


    //------------------------------------------- CLASSES AND ENUMS

    public enum SmbAction {
        AUTH,
        IS_SMB,
        ERROR,

        BROWS,

        OPEN_FILE,
        UPLOAD_FILES,
        INFO_FILE,
        DOWNLOAD_FILE,
        DELETE_FILE,

        UPDATE_PB,
        UPDATE_DOWNLOAD_NOTIFICATION,
        UPDATE_UPLOAD_NOTIFICATION,

        START_UPLOAD,
        START_OPEN,
        START_DOWNLOAD,

        FILE_DELETED,

        listfiles,

        TEST
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

        public CoreSmbThread build() {
            return new CoreSmbThread(handler, url, action, context, auth);
        }

    }


}
