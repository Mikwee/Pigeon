package com.mikwee.timebrowser.utils;

import java.io.Serializable;

public class FileInfoExtended extends FileInfo implements Serializable {

    public String lastModified;
    public String createTime;

    public boolean isHidden;
    public boolean isAccessDenied;
    public boolean isWritable;

}
