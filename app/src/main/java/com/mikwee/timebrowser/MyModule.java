package com.mikwee.timebrowser;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.GlideModule;

import java.io.InputStream;


public class MyModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }
/*
    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(InputStream.class, InputStream.class, new PassthroughStreamLoader.Factory());
    }*/

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {

    }
}
