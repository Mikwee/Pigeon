package com.mikwee.timebrowser.glide;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.IOException;
import java.io.InputStream;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

class SmbDirectModelLoader implements ModelLoader<SmbFileModel, InputStream> {


    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(SmbFileModel model, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(model.getUrl()), new SmbDataFetcher(model));
    }

    @Override
    public boolean handles(SmbFileModel model) {
        return model.getUrl().startsWith("smb");
    }

    //My custom DataFetcher
    public class SmbDataFetcher implements DataFetcher<InputStream> {
        private final String TAG = SmbDataFetcher.class.getSimpleName();

        private NtlmPasswordAuthentication auth;
        private String path;
        private InputStream in;

        public SmbDataFetcher(SmbFileModel model) {

            this.auth = model.getAuth();
            this.path = model.getUrl();
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {

            try {
                SmbFile file = new SmbFile(path, auth);
                in = file.getInputStream();
                callback.onDataReady(in);
            } catch (Exception e) {
                callback.onLoadFailed(e);
                Log.e(TAG, "Wonderful exception: " + e.getMessage());
            }

        }

        @Override
        public void cleanup() {
            try {
                in.close();
            } catch (IOException e) {
                Log.d(TAG, "Couldn't close input stream: " + e.getMessage());
            }
        }

        @Override
        public void cancel() {
            // Intentionally empty.
            //todo only implement if we want to cancel as operation (such as navigating to a new directory)
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }
    }


}

