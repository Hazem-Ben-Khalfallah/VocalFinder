package com.blacknebula.vocalfinder;

import android.app.Application;
import android.content.Context;

public class VocalFinderApplication extends Application {

    private static Application context;

    public static Context getAppContext() {
        return VocalFinderApplication.context;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.context = this;
    }
}