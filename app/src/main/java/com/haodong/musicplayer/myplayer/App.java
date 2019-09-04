package com.haodong.musicplayer.myplayer;

import android.app.Application;

/**
 * created by linghaoDo on 2019-09-01
 * <p>
 * description:
 */
public class App extends Application {
    private static  App sInstance=null;

    public static App getInstance() {
        return sInstance;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance=this;
    }
}
