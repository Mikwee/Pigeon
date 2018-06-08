package com.mikwee.timebrowser.smb;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.utils.FileInfo;
import com.mikwee.timebrowser.utils.FileInfoExtended;
import com.mikwee.timebrowser.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;


public class BrowserThread extends Thread {
    public static final String TAG = BrowserThread.class.getSimpleName();


    //----------------------BASE VARIABLES
    private NtlmPasswordAuthentication auth;
    private SmbAction action;
    private Handler handle;
    private Context c;
    private String url;
    private List extraFiles;


    private NotificationCompat.Builder notifB;
    private NotificationManager notifM;


    //----------------------CONSTRUCTORS

    BrowserThread(Handler handle, String url, SmbAction action, Context c, NtlmPasswordAuthentication auth, List extrafiles) {
        this.handle = handle;
        this.url = url;
        this.action = action;
        this.c = c;
        this.auth = auth;
        this.extraFiles = extrafiles;

    }

    @Override
    public void run() {

        try {
            Message msg = new Message();

            switch (action) {

                case BROWS: //----------------------------------------------------------------------
                    SmbFile fileBrows = new SmbFile(url, auth);

                    SmbFile[] dirs = fileBrows.listFiles();
                    int length = dirs.length;

                    FileInfo[] fils = new FileInfo[length];

                    //Loop through every file in directory and get info
                    for (int i = 0; i < length; i++) {
                        fils[i] = new FileInfo();
                        fils[i].fileName = dirs[i].getName();
                        fils[i].fileFullPath = dirs[i].getCanonicalPath();
                        fils[i].isDirectory = dirs[i].isDirectory();

                    }

                    msg.what = action.ordinal();
                    msg.obj = fils;
                    break;

                case FILE_INFO: //------------------------------------------------------------------
                    SmbFile fileInfo = new SmbFile(url, auth);

                    msg.what = action.ordinal();
                    msg.obj = getFileInfo(fileInfo);
                    break;

                case DOWNLOAD_FILES: //-------------------------------------------------------------

                    List<String> succeed = new ArrayList<>();
                    List<String> failed = new ArrayList<>();

                    //Init notification stuff
                    initNotif();

                    updateNotification(extraFiles.size(), 0, 0);

                    //For each file in download list
                    for (int left = extraFiles.size(); left > 0; left--) {

                        //Get the file
                        FileInfo fileToDownload = (FileInfo) extraFiles.get(left - 1);

                        //Check if download is successful or not
                        if (downloadFile(fileToDownload.fileFullPath))
                            succeed.add(fileToDownload.fileName);
                        else
                            failed.add(fileToDownload.fileName);

                        //Update notification
                        updateNotification(left, succeed.size(), failed.size());

                    }

                    break;

             /*   case OPEN_FILE:
                    openFile(file);
                    break;

                case UPLOAD_FILES:
                    startProgressBar(SmbAction.START_UPLOAD);
                    uploadFiles();
                    break;



                case DELETE_FILE:
                    file.delete();
                    //create and send message
                    Message msgDelete = new Message();
                    msgDelete.obj = null;
                    msgDelete.what = SmbAction.FILE_DELETED.ordinal();
                    handle.sendMessage(msgDelete);
                    break;

               */
            }

            handle.sendMessage(msg);

        } catch (SmbException e) {//-------------------EXCEPTION HANDLING
            Log.e(TAG, "Smb exc"); //todo try again
            sendErr(e.getNtStatus(), e.getMessage());
        } catch (MalformedURLException e) {
            Log.e(TAG, "malformed url exc");
            sendErr(666, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "runtime exc");
            sendErr(666, null);
        }

    }

    //Setup notification stuff
    private void initNotif() {
        notifM = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        notifB = new NotificationCompat.Builder(c);
        notifB.setSmallIcon(R.mipmap.ic_launcher_foreground);
    }

    //Create error message to return to handler
    private void sendErr(int errorCode, String defMessage) {
        Message msg = new Message();
        msg.what = SmbAction.ERROR.ordinal();
        msg.obj = Utils.getErrorString(c, errorCode, action, defMessage);
        handle.sendMessage(msg);
    }

    //----------------------------------------------------------------------------------------------

    //get file detailed info
    private FileInfoExtended getFileInfo(SmbFile file) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy hh:mm:ss a");
        sdf.setCalendar(new GregorianCalendar());

        FileInfoExtended fi = new FileInfoExtended();

        fi.fileName = file.getName();
        fi.fileFullPath = file.getCanonicalPath();

        try {
            fi.isDirectory = file.isDirectory();
        } catch (SmbException e) {
            fi.isDirectory = false;
        }
        try {
            fi.fileSize = file.length();
        } catch (SmbException e) {
            fi.fileSize = 0L;
        }
        try {
            fi.lastModified = sdf.format(new Date(file.lastModified()));
        } catch (SmbException e) {
            fi.lastModified = null;
        }
        try {
            fi.createTime = sdf.format(new Date(file.createTime()));
        } catch (SmbException e) {
            fi.createTime = null;
        }
        try {
            fi.isWritable = file.canWrite();
        } catch (SmbException e) {
            fi.isWritable = false;
        }
        try {
            fi.isHidden = file.isHidden();
        } catch (SmbException e) {
            fi.isHidden = false;
        }
        try {
            file.getSecurity(true);
            fi.isAccessDenied = false;
        } catch (IOException e) {
            fi.isAccessDenied = true;
        }

        return fi;
    }

    //Download a SmbFile on phone and returns result of operation
    private boolean downloadFile(String smbFilePath) {
        Log.e(TAG, "Download");

        SmbFile fromFile = null;
        try {
            //Create file to download
            fromFile = new SmbFile(smbFilePath, auth);
            Log.e(TAG, "from smb file: " + fromFile.getPath());

            //Create target file
            File newFile = new File(url, fromFile.getName());
            Log.e(TAG, "to file: " + newFile.getPath());


            FileOutputStream outputStream = new FileOutputStream(newFile);
            SmbFileInputStream inputStream = (SmbFileInputStream) fromFile.getInputStream();

            byte[] buffer = new byte[1024];
            int len1;
            int count = 0;
            //Counts how many loops without sending update
            int updateNumber = 0;
            //progress percentage
            int progress;

            long tot = fromFile.length();

            while ((len1 = inputStream.read(buffer)) > 0) {
                count += len1;

                //calculate overall progress
                progress = (int) ((count * 100) / tot);

                //send only every 8 updates or when file download is finished
                if (updateNumber == 8 || progress == 100) {
                    updateNumber = 0;
                    //create notification
                    updateNotificationPartial(fromFile.getName(), progress);
                }
                updateNumber++;

                outputStream.write(buffer, 0, len1);
            }


            outputStream.close();
            inputStream.close();
            return true;

        } catch (MalformedURLException e) { //todo what is the need of dividing errors ?
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

    //Updates the notification after an entire file
    private void updateNotification(int left, int success, int failed) {
        String content = c.getString(R.string.notif_left) + left +
                c.getString(R.string.notif_success) + success +
                c.getString(R.string.notif_failed) + failed;

        notifB.setContentText(content);

        notifM.notify(SmbAction.DOWNLOAD_FILES.ordinal(), notifB.build());
    }

    //Updates the notification after partial
    private void updateNotificationPartial(String name, int progress) {

        String title = c.getString(R.string.notif_download_current) + name;
        notifB.setContentTitle(title);

        notifB.setProgress(100, progress, false);

        notifM.notify(SmbAction.DOWNLOAD_FILES.ordinal(), notifB.build());
    }


    //-------------------BUILDER
    public enum SmbAction {
        BROWS,
        ERROR,

        FILE_INFO,
        DOWNLOAD_FILES,


        OPEN_FILE,
        UPLOAD_FILES,

        DELETE_FILE,

        UPDATE_PB,

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
        private List extraFiles;
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

        public Builder setExtraFiles(List extraFiles) {
            this.extraFiles = extraFiles;
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

        public BrowserThread build() {
            return new BrowserThread(handler, url, action, context, auth, extraFiles);
        }

    }

}
