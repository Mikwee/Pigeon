package com.mikwee.timebrowser.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

@GlideModule
public class SmbDirectModule extends AppGlideModule {

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.prepend(SmbFileModel.class, InputStream.class, new MyModelLoaderFactory());
    }

    public class MyModelLoaderFactory implements ModelLoaderFactory<SmbFileModel, InputStream> {

        @Override
        public ModelLoader<SmbFileModel, InputStream> build(MultiModelLoaderFactory unused) {
            return new SmbDirectModelLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

}