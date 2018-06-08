package com.mikwee.timebrowser.smb;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mikwee.timebrowser.utils.FileInfo;
import com.mikwee.timebrowser.utils.FileInfoExtended;
import com.mikwee.timebrowser.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import jcifs.netbios.NbtAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;


public class NewSmbThread extends Thread {
    private static final String TAG = NewSmbThread.class.getSimpleName();

    //Info about the server
    public String serverName = ""; //The real name  (ex.. STATICA)
    public boolean isSmb = false;
    public boolean requiresPassword = false;
    private String resolveIp = "";

    private String server_lanman = "";  //windows 8
    private String server_os = "";   //windows 8.1 bla bla


    public boolean interrupted = false;
    private int id = -1;
    private String targetUrl;
    private String[] uploadUrls;


    //----------------------BASE VARIABLES
    private SmbAction action;
    private Handler handle;
    private Context c;
    private String url;
    private NtlmPasswordAuthentication auth;


    //----------------------CONSTRUCTORS

    NewSmbThread(Handler handle, String url, SmbAction action, Context c, NtlmPasswordAuthentication auth) {
        this.handle = handle;
        this.url = url;
        this.action = action;
        this.c = c;
        this.auth = auth;
    }


    public void run() {

        Message msg = new Message();
        try {

            SmbFile file = new SmbFile(url, auth);

            switch (action) {

                case FIND_SHARES: //-------------Is IP smb

                    if (file.listFiles() == null)  //Throws exception if is not smb
                        return;

                    isSmb = true;
                    requiresPassword = false;
                    //Retreive the host name
                    retrieveName();

                    break;

                case AUTH: //---------------Verify authentication

                    file.listFiles(); // Throws exception if is not smb

                    msg.what = action.ordinal();
                    msg.obj = true;
                    break;


                case BROWS: //----------------Given a url, returns all files

                    SmbFile[] dirs = file.listFiles();
                    int length = dirs.length;

                    FileInfo[] fils = new FileInfo[length];

                    for (int i = 0; i < length; i++) {
                        fils[i] = new FileInfo();
                        fils[i].fileName = dirs[i].getName();
                        fils[i].fileFullPath = dirs[i].getCanonicalPath();
                        fils[i].fileSize = dirs[i].length();
                       // fils[i].isWritable = dirs[i].canWrite();
                        fils[i].isDirectory = dirs[i].isDirectory();

                    }

                    //create the message

                    msg.what = action.ordinal();
                    msg.obj = fils;
                    //send the message
                    handle.sendMessage(msg);
                    break;

                case OPEN_FILE:
                    openFile(file);
                    break;

                case UPLOAD_FILES:
                    startProgressBar(SmbAction.START_UPLOAD);
                    uploadFiles();
                    break;

                case INFO_FILE:
                    getFileDetailedInfo(file);
                    break;

                case DELETE_FILE:
                    file.delete();
                    //create and send message
                    Message msgDelete = new Message();
                    msgDelete.obj = null;
                    msgDelete.what = SmbAction.FILE_DELETED.ordinal();
                    handle.sendMessage(msgDelete);
                    break;

                case DOWNLOAD_FILE:
                    startProgressBar(SmbAction.START_DOWNLOAD);
                    File local = downloadFile(file, targetUrl, SmbAction.UPDATE_DOWNLOAD_NOTIFICATION.ordinal());
                    if (local == null) {
                        //create and send message
                        Message msgDownload = new Message();
                        msgDownload.obj = "Error downloading file";
                        msgDownload.what = SmbAction.ERROR.ordinal();
                        handle.sendMessage(msgDownload);
                    }
                    break;
            }


        } catch (SmbException e) {

            // If the cause is "logon failure"
            if (e.getNtStatus() == -1073741715) {
                requiresPassword = true;
                isSmb = true;
                retrieveName();
            }
            sendErr(e.getNtStatus(), e.getMessage());


        } catch (MalformedURLException e) {
            Log.e(TAG, "malformed url exc");
            sendErr(666, null);
        } catch (RuntimeException e) { //_________________________________RUNTIME EXC
            Log.e(TAG, "runtime exc");
            if (this.serverName.equals("")) {
                this.requiresPassword = true;
            }
            sendErr(666, null);
        } finally {
            handle.sendMessage(msg);
        }


    }

    //-----------------------METHODS


    //Retrieve the host name given the IP
    private void retrieveName() {

        if (action != SmbAction.FIND_SHARES) {
            return;
        }

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


    //get file detailed info
    private void getFileDetailedInfo(SmbFile file) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy hh:mm:ss a");
        GregorianCalendar cal = new GregorianCalendar();
        sdf.setCalendar(cal);
        FileInfoExtended fi = new FileInfoExtended();

        fi.fileName = file.getName();

        fi.fileFullPath = file.getCanonicalPath();


        try {
            fi.isDirectory = file.isDirectory();
        } catch (SmbException e) {
            fi.isDirectory = false;
            Log.e(TAG, "can't figure out if is directory or not, assuming not");
        }
        try {
            fi.fileSize = file.length();
        } catch (SmbException e) {
            fi.fileSize = 0L;
        }

        try {
            fi.isWritable = file.canWrite();
        } catch (SmbException e) {
            fi.isWritable = false;
        }

        /*
        try {
           fi.freeDiskSpace = file.getDiskFreeSpace();
        } catch (SmbException e) {
            fi.freeDiskSpace = -1;
        }*/

        try {
            fi.lastModified = sdf.format(new Date(file.lastModified()));
        } catch (SmbException e) {
            fi.lastModified = null;
        }

        try {
            fi.isHidden = file.isHidden();
        } catch (SmbException e) {
            fi.isHidden = false;
        }

        try {
            fi.createTime = sdf.format(new Date(file.createTime()));
        } catch (SmbException e) {
            fi.createTime = null;
        }

        /*
        try {
            file.getSecurity(true);
            fi.accessDenied = false;
        } catch (IOException e) {
            Log.e(TAG, "access is denied");
            fi.accessDenied = true;
        }*/

        Message msgInfo = new Message();
        msgInfo.what = action.ordinal();
        msgInfo.obj = fi;
        //send the message
        handle.sendMessage(msgInfo);
    }


    //download file and notify fragment
    private void openFile(SmbFile smbFile) {

        //download the file
        File local = downloadFile(smbFile, targetUrl, SmbAction.UPDATE_PB.ordinal());

        //create the message
        Message msg = new Message();
        if (local != null) {
            msg.what = action.ordinal();
            msg.obj = local;
        } else {
            msg.what = SmbAction.ERROR.ordinal();
            msg.obj = "Error downloading file";
        }
        //send the message
        handle.sendMessage(msg);
    }

    //Download a SmbFile on phone and return it. Null if error occurred
    private File downloadFile(SmbFile fromFile, String toUrl, int updateType) {

        Log.e(TAG, "smb file: " + fromFile.getPath());
        try {

            File newFile = new File(toUrl, fromFile.getName());
            Log.e(TAG, "to file: " + newFile.getPath());

            FileOutputStream outputStream = new FileOutputStream(newFile);

            SmbFileInputStream inputStream = (SmbFileInputStream) fromFile.getInputStream();

            byte[] buffer = new byte[1024];
            int len1;
            int count = 0;
            int i = 0;
            int progr;


            long tot = fromFile.length();

            while ((len1 = inputStream.read(buffer)) > 0) {
                count += len1;
                //send update message
                progr = (int) ((count * 100) / tot);
                if (i == 8 || progr == 100) {
                    i = 0;
                    Message msg = new Message();
                    msg.what = updateType;
                    msg.obj = progr;
                    handle.sendMessage(msg);
                }
                i++;
                outputStream.write(buffer, 0, len1);
            }


            outputStream.close();
            inputStream.close();
            return newFile;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e("MalformURL", e.getMessage());
            return null;
        } catch (SmbException e) {
            e.printStackTrace();
            Log.e("SMBException", e.getMessage());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", e.getMessage());
            return null;
        }
    }

    private void uploadFiles() {
        Long totSize = 0L;
        File[] filz = new File[uploadUrls.length];
        //calculate total file size
        for (int i = 0; i < uploadUrls.length; i++) {
            filz[i] = new File(uploadUrls[i]);
            totSize += filz[i].length();
        }

        Long start = 0L;
        //upload every file
        for (File f : filz) {
            uploadFile(f, start, totSize);
            start += f.length();
        }
    }

    public boolean uploadFile(File fromFile, long start, long totalSize) {

        try {
            InputStream in = new FileInputStream(fromFile);
            String path = url + fromFile.getName();
            SmbFile sFile = new SmbFile(path, auth);
            SmbFileOutputStream out = new SmbFileOutputStream(sFile);

            byte[] buf = new byte[1024];
            int len;
            int i = 0;
            int progr;

            while ((len = in.read(buf)) > 0) {
                start += len;

                //send update message
                progr = (int) ((start * 100) / totalSize);
                if (i == 8 || progr == 100) {
                    i = 0;
                    Message msg = new Message();
                    msg.what = SmbAction.UPDATE_UPLOAD_NOTIFICATION.ordinal();
                    msg.obj = progr;
                    handle.sendMessage(msg);
                }
                i++;
                out.write(buf, 0, len);
            }

            out.close();
            in.close();
            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e("MalformURL", e.getMessage());
            return false;
        } catch (SmbException e) {
            e.printStackTrace();
            Log.e("SMBException", e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", e.getMessage());
            return false;
        }
    }

    //Create error message to return to handler
    private void sendErr(int errorCode, String defMessage) {
        if (action == SmbAction.FIND_SHARES)
            return;

        //Create the message
        Message msg = new Message();
        msg.what = SmbAction.ERROR.ordinal();
        msg.obj = Utils.getErrorString(c, errorCode, action, defMessage);

        Log.e(TAG, (String) msg.obj);
        //send the message
        handle.sendMessage(msg);
    }

    private void startProgressBar(SmbAction progAction) {
        Message msg = new Message();
        msg.what = progAction.ordinal();
        msg.obj = null;
        handle.sendMessage(msg);
    }

    //---------------SETTERS


    public NewSmbThread setAction(SmbAction action) {
        this.action = action;
        return this;
    }


    public void setResolveIp(String resolveIp) {
        this.resolveIp = resolveIp;
    }


    public void setUploadFromUrls(String[] urls) {
        this.uploadUrls = urls;
    }

    //set the url on phone where to download file
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }


    //------------------------------------------- CLASSES AND ENUMS

    public enum SmbAction {
        AUTH,
        FIND_SHARES,
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
        ERROR,
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

        public NewSmbThread build() {
            return new NewSmbThread(handler, url, action, context, auth);
        }
    }


}
