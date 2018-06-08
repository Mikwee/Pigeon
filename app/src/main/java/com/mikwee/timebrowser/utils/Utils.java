package com.mikwee.timebrowser.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.smb.BrowserThread;
import com.mikwee.timebrowser.smb.CheckThread;
import com.mikwee.timebrowser.smb.CoreSmbThread;
import com.mikwee.timebrowser.smb.NewSmbThread;
import com.mikwee.timebrowser.smb.SmbThread;

import java.io.File;

public class Utils {

    //Get the number of directories in path
    public static int getDepth(String path) {
        //If file is long less than minimum
        if (path.length() <= 7)
            return -1;

        //if last char is a / remove it
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        //remove initial smb:// part
        path = path.substring(6, path.length());

        //we split every dir
        String[] split = path.split("/");

        return split.length - 1;
    }

    //Get the right error string
    public static String getErrorString(Context c, int errorCode, BrowserThread.SmbAction action, String defMessage) {

        // Create first part of error string, indicating the action that failed
        String msg1 = "";
        switch (action) {
            case BROWS:
                msg1 = c.getString(R.string.error_at_brows);
                break;
            case OPEN_FILE:
                msg1 = c.getString(R.string.error_at_open);
                break;

            case UPLOAD_FILES:
                msg1 = c.getString(R.string.error_at_upload);
                break;
/*
            case INFO_FILE:
                msg1 = c.getString(R.string.error_at_info);
                break;
*/
            case DELETE_FILE:
                msg1 = c.getString(R.string.error_at_delete);
                break;

            case DOWNLOAD_FILES:
                msg1 = c.getString(R.string.error_at_download);
                break;
        }

        //Create second part of string error
        String msg2 = "";
        switch (errorCode) {
            case -1073741715:
                msg2 = c.getString(R.string.error_login);
                break;
            default:
                if (defMessage == null)
                    msg2 = c.getString(R.string.error_generic);
                else
                    msg2 = defMessage;
                break;
        }

        return msg1 + " " + msg2;
    }

    //Get a file MIME from extension name
    public static String getMIMEbyFileName(String filename) {
        String ext = null, type = null;

        //Get Extension
        int lastdot = filename.lastIndexOf(".");
        if (lastdot > 0)
            ext = filename.substring(lastdot + 1);

        //Get Type
        if (ext != null)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());

        //Unknown type
        if (ext == null || type == null)
            type = "application/unknown";

        return type;
    }

    //Get image resource from MIME
    public static int getImageByMIME(String type) {

        //Set as default the generic file icon
        int resourceId = R.drawable.file_generic;
        if (type.startsWith("file_folder")) {
            resourceId = R.drawable.file_folder;
        } else if (type.startsWith("image")) {
            resourceId = R.drawable.file_img;
        } else if (type.startsWith("video")) {
            resourceId = R.drawable.file_video;
        } else if (type.startsWith("audio")) {
            resourceId = R.drawable.file_audio;
        } else if (type.startsWith("text")) {
            resourceId = R.drawable.file_text;
        } else if (type.startsWith("archive")) {
            resourceId = R.drawable.file_archive;
        }
        return resourceId;
    }

    //Get underline color from MIME
    public static int getColorByMIME(String type, Context c) {

        //Set as default the generic file icon
        int resourceId = R.color.fileTypeGeneric;
        if (type.equals("file_folder")) {
            resourceId = R.color.fileTypeFolder;
        } else if (type.startsWith("image")) {
            resourceId = R.color.fileTypeImage;
        } else if (type.startsWith("video")) {
            resourceId = R.color.fileTypeVideo;
        } else if (type.startsWith("audio")) {
            resourceId = R.color.fileTypeAudio;
        } else if (type.startsWith("text")) {
            resourceId = R.color.fileTypeText;
        } else if (type.startsWith("archive")) {
            resourceId = R.color.fileTypeArchive;
        }

        return c.getResources().getColor(resourceId);
    }




    public static String[] getSortMethods(Context c) {
        // the array size is the sort methods lenght
        String[] sortM = new String[4];

        sortM[0] = c.getString(R.string.sort_size_ascending);
        sortM[1] = c.getString(R.string.sort_size_descending);
        sortM[2] = c.getString(R.string.sort_folder_first);
        sortM[3] = c.getString(R.string.sort_files_first);

        return sortM;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String getInternalTempDir(Context c) {
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "tempDownloads");
        f.mkdirs();
        return f.getAbsolutePath();
    }

    public static void deleteTempFiles(Context context) {
        //Create a file from out temp directory
        File file = new File(getInternalTempDir(context));
        //List all files
        File[] files = file.listFiles();
        //If there are files
        if (files != null) {
            //Delete every file
            for (File f : files) {
                boolean success = f.delete();
                if (!success)
                    Log.e("Utils", "Error deleting temp file ");
            }
        }

    }


    public static String getErrorString(Context c, int errorCode, SmbThread.SmbAction action, String defMessage) {

        // Create first part of error string, indicating the action that failed
        String msg1 = "";
        switch (action) {
            case BROWS:
                msg1 = c.getString(R.string.error_at_brows);
                break;
            case OPEN_FILE:
                msg1 = c.getString(R.string.error_at_open);
                break;

            case UPLOAD_FILES:
                msg1 = c.getString(R.string.error_at_upload);
                break;

            case INFO_FILE:
                msg1 = c.getString(R.string.error_at_info);
                break;

            case DELETE_FILE:
                msg1 = c.getString(R.string.error_at_delete);
                break;

            case DOWNLOAD_FILE:
                msg1 = c.getString(R.string.error_at_download);
                break;
        }

        //Create second part of string error
        String msg2 = "";
        switch (errorCode) {
            case -1073741715:
                msg2 = c.getString(R.string.error_login);
                break;
            default:
                if (defMessage == null)
                    msg2 = c.getString(R.string.error_generic);
                else
                    msg2 = defMessage;
                break;
        }

        return msg1 + " " + msg2;
    }

    public static String getErrorString(Context c, int errorCode, NewSmbThread.SmbAction action, String defMessage) {

        // Create first part of error string, indicating the action that failed
        String msg1 = "";
        switch (action) {
            case BROWS:
                msg1 = c.getString(R.string.error_at_brows);
                break;
            case OPEN_FILE:
                msg1 = c.getString(R.string.error_at_open);
                break;

            case UPLOAD_FILES:
                msg1 = c.getString(R.string.error_at_upload);
                break;

            case INFO_FILE:
                msg1 = c.getString(R.string.error_at_info);
                break;

            case DELETE_FILE:
                msg1 = c.getString(R.string.error_at_delete);
                break;

            case DOWNLOAD_FILE:
                msg1 = c.getString(R.string.error_at_download);
                break;
        }

        //Create second part of string error
        String msg2 = "";
        switch (errorCode) {
            case -1073741715:
                msg2 = c.getString(R.string.error_login);
                break;
            default:
                if (defMessage == null)
                    msg2 = c.getString(R.string.error_generic);
                else
                    msg2 = defMessage;
                break;
        }

        return msg1 + " " + msg2;
    }

    public static String getErrorString(Context c, int errorCode, CheckThread.SmbAction action, String defMessage) {


        //Create second part of string error
        String msg2 = "";
        switch (errorCode) {
            case -1073741715:
                msg2 = c.getString(R.string.error_login);
                break;
            default:
                if (defMessage == null)
                    msg2 = c.getString(R.string.error_generic);
                else
                    msg2 = defMessage;
                break;
        }

        return msg2;
    }

    public static String getErrorString(Context c, int errorCode, CoreSmbThread.SmbAction action, String defMessage) {


        //Create second part of string error
        String msg2 = "";
        switch (errorCode) {
            case -1073741715:
                msg2 = c.getString(R.string.error_login);
                break;
            default:
                if (defMessage == null)
                    msg2 = c.getString(R.string.error_generic);
                else
                    msg2 = defMessage;
                break;
        }

        return msg2;
    }


    // IMPORTANT, WHEN ADDING SORT METHOD, MODIFY GETSORTMETHODS
    public enum Sort {
        SIZE_ASCENDING,
        SIZE_DESCENDING,
        FOLDER_FIRST,
        FILES_FIRST,
    }

}
