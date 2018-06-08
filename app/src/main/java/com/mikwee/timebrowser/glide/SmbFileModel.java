package com.mikwee.timebrowser.glide;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class SmbFileModel {

    private NtlmPasswordAuthentication auth;
    private String url;

    public SmbFileModel(NtlmPasswordAuthentication auth, String url){
        this.auth = auth;
        this.url = url;
    }



    public NtlmPasswordAuthentication getAuth() {
        return auth;
    }

    public void setAuth(NtlmPasswordAuthentication auth) {
        this.auth = auth;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


}
