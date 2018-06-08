package com.mikwee.timebrowser;

import com.mikwee.timebrowser.utils.Utils;

import java.net.MalformedURLException;

import jcifs.smb.SmbFile;

public class SomeCode {

/*
    //navigate to the previous directory
    public boolean goToPreviousFolder() {
        //retrieve the previous directory
        String parent;
        try {
            parent = new SmbFile(currentPath).getParent();
        } catch (MalformedURLException e) {
            return false;
        }

        //if we are at root directory there is no need to go back
        if ((parent == null) || parent.equals("smb://"))
            return false;

        //browse to previous
        brows(parent);

        return true;

    }

    //Given a path it gives back it's parent url
    private String getParentPath(String path) {
        if (path.equals("smb://"))
            return null;
        //remove last char in case it's a "/"if (path.endsWith("/"))
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        //find last "/" character
        int end = path.lastIndexOf("/");
        //this is the parent
        return path.substring(0, end + 1);
    }

    //Gets the path of a  parent directory at index newI and brows to it
    public void navigateBackTo(int newI) {

        //Current depth
        int oldI = Utils.getDepth(currentPath);

        futurePath = currentPath;

        //go back directory until we are where we clicked
        while (oldI > newI) {
            oldI--;
            futurePath = getParentPath(futurePath);
        }
        //brows to selected directory
        brows(futurePath);
    }


*/
}
